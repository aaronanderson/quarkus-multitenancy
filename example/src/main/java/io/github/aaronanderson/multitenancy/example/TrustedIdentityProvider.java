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

	@Inject
	@TenantId
	Instance<String> tenantId;

	private String currentTenantId() {
		try {
			return tenantId.get();
		} catch (ContextNotActiveException ce) {
			return "";
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

		log.infof("TrustedIdentityProvider authenticate: %s - %s", subject, tenantId);

//		Disable validation until Vert.x context propation/lifecycle issue is resolved	
//		String[] parts = subject.split("@");
//		if (parts.length > 1 && !parts[1].equals(tenantId)) {
//			throw new AuthenticationFailedException(String.format("Subject tenant %s does not match requested tenant %s", parts[1], tenantId));
//		}
		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
		builder.setPrincipal(new QuarkusPrincipal(subject));
		return Uni.createFrom().item(builder.build());
	}

}
