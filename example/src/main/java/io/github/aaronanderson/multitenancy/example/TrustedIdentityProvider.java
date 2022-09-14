package io.github.aaronanderson.multitenancy.example;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Trust all basic auth
 */
@ApplicationScoped
public class TrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

	@Override
	public Class<TrustedAuthenticationRequest> getRequestType() {
		return TrustedAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
		builder.setPrincipal(new QuarkusPrincipal(request.getPrincipal()));
		return Uni.createFrom().item(builder.build());
	}

}
