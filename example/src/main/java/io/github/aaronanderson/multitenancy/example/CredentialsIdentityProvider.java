package io.github.aaronanderson.multitenancy.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Trust all basic or form based auth credentials 
 */
@ApplicationScoped
public class CredentialsIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

	private static final Logger log = Logger.getLogger(CredentialsIdentityProvider.class);

	@TenantId
	Instance<String> tenantId;

	private String subject(String userName) {
		try {
			return userName + "@" + tenantId.get();
		} catch (ContextNotActiveException ce) {
			return userName;
		}
	}

	@Override
	public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
		return UsernamePasswordAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
		String subject = subject(request.getUsername());
		return Uni.createFrom().item(() -> {
			log.debugf("CredentialsIdentityProvider authenticate: %s", subject);
			QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
			builder.setPrincipal(new QuarkusPrincipal(subject));
			return builder.build();

		});

	}

}
