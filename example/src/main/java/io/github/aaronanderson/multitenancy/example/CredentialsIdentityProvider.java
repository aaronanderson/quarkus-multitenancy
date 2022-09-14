package io.github.aaronanderson.multitenancy.example;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Trust all basic or form based auth credentials
 * TODO add support for tenant checking to prevent cross tenant access 
 */
@ApplicationScoped
public class CredentialsIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

	@Override
	public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
		return UsernamePasswordAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
		return Uni.createFrom().item(() -> {

			QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
			builder.setPrincipal(new QuarkusPrincipal(request.getUsername()));
			return builder.build();

		});

	}

}
