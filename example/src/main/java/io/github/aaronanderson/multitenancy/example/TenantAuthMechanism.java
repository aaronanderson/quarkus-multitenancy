package io.github.aaronanderson.multitenancy.example;

import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(1)
@ApplicationScoped
public class TenantAuthMechanism implements HttpAuthenticationMechanism {

	private static final Logger log = Logger.getLogger(TenantAuthMechanism.class);

	//@Inject
	//MfaAuthenticationMechanism mfa;

	//@Inject
	//OidcAuthenticationMechanism oidc;

	@Inject
	BasicAuthenticationMechanism ba;
	
	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
	    // do some custom action and delegate
        //    return mfa.authenticate(context, identityProviderManager);
		//return oidc.authenticate(context, identityProviderManager);
		log.infof("authenticate");
		return ba.authenticate(context, identityProviderManager);
	}

	@Override
	public Uni<ChallengeData> getChallenge(RoutingContext context) {		
		//return mfa.getChallenge(context);
		log.infof("getChallenge");
		return ba.getChallenge(context);
	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
		//return mfa.getCredentialTypes();
		return ba.getCredentialTypes();
	}


}

