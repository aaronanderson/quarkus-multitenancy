package io.github.aaronanderson.multitenancy.example;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProperty;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.arc.Arc;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(1)
@ApplicationScoped
public class TenantAuthMechanism implements HttpAuthenticationMechanism {

	private static final Logger log = Logger.getLogger(TenantAuthMechanism.class);

	@Inject
	FormAuthenticationMechanism form;

	@Inject
	@TenantProperty(name = "oidc-enabled")
	Instance<Boolean> oidcEnabled;

	@Inject
	@TenantId
	Instance<String> tenantId;

	@Inject
	OidcAuthenticationMechanism oidc;

	// @Inject
	// BasicAuthenticationMechanism ba;

	private boolean isOidcEnabled() {
		try {
			return oidcEnabled.get();
		} catch (ContextNotActiveException e) {
			return false;
		}
	}

	private String tenantPath() {
		try {
			return "/" + tenantId.get();
		} catch (ContextNotActiveException e) {
			return "";
		}
	}

	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
		if (isOidcEnabled()) {
			return oidc.authenticate(context, identityProviderManager);
		}
		return form.authenticate(context, identityProviderManager);
	}

	@Override
	public Uni<ChallengeData> getChallenge(RoutingContext context) {
		if (isOidcEnabled()) {
			return oidc.getChallenge(context);
		}
		// override form challenge redirect to consider tenant path
		if (context.normalizedPath().endsWith("/login-action") && context.request().method().equals(HttpMethod.POST)) {
			String loc = context.request().scheme() + "://" + context.request().host() + "/access-denied";
			return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
		} else {
			String loc = context.request().scheme() + "://" + context.request().host() + tenantPath() + "/login";
			return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
		}

	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
		Set<Class<? extends AuthenticationRequest>> types = new HashSet<>();
		types.addAll(form.getCredentialTypes());
		types.addAll(oidc.getCredentialTypes());
		return types;
	}

}
