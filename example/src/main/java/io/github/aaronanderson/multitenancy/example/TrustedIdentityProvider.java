package io.github.aaronanderson.multitenancy.example;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Trust all authenticated cookies
 */
@ApplicationScoped
public class TrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

	private static final Logger log = Logger.getLogger(TrustedIdentityProvider.class);

	@TenantId
	Instance<String> tenantId;

	private String currentTenantId() {
		try {
			return tenantId.get();
		} catch (ContextNotActiveException ce) {
			return null;
		}
	}

	@Override
	public Class<TrustedAuthenticationRequest> getRequestType() {
		return TrustedAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
		String subject = request.getPrincipal();

		String tenantId = currentTenantId();

		log.debugf("TrustedIdentityProvider authenticate: %s - %s", subject, tenantId);

		String[] parts = subject.split("@");
		if (tenantId != null && parts.length > 1 && !parts[parts.length - 1].equals(tenantId)) {
			log.debugf("Cross tenant access attempted, rejecting  %s - %s", parts[1], tenantId);
			throw new AuthenticationFailedException(String.format("Subject tenant %s does not match requested tenant %s", parts[1], tenantId));
		}
		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
		builder.setPrincipal(new QuarkusPrincipal(subject));
		return Uni.createFrom().item(builder.build());
	}

}
