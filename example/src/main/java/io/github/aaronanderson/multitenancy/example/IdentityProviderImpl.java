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
 * MFA IdentityProvider
 */
@ApplicationScoped
public class IdentityProviderImpl implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

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
