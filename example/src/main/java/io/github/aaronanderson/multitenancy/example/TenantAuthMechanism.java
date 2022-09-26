package io.github.aaronanderson.multitenancy.example;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProperty;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(1)
@ApplicationScoped
public class TenantAuthMechanism implements HttpAuthenticationMechanism {

	private static final Logger log = Logger.getLogger(TenantAuthMechanism.class);

	FormAuthenticationMechanism form;

	@TenantProperty(name = "oidc-enabled")
	Instance<Boolean> oidcEnabled;

	@TenantId
	Instance<String> tenantId;

	@Inject
	OidcAuthenticationMechanism oidc;

	@PostConstruct
	public void init() {
		PersistentLoginManager loginManager = new PersistentLoginManager(null, "quarkus-credential", Duration.ofMinutes(30).toMillis(), Duration.ofMinutes(1).toMillis());
		form = new TenantFormAuthenticationMechanism(loginManager);
	}
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
		return form.getChallenge(context);

	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
		Set<Class<? extends AuthenticationRequest>> types = new HashSet<>();
		types.addAll(form.getCredentialTypes());
		types.addAll(oidc.getCredentialTypes());
		return types;
	}

	// Override the FormAuthenticationMechanism to support logout and adjust redirect URLs to account for tenant paths.
	private class TenantFormAuthenticationMechanism extends FormAuthenticationMechanism {

		private PersistentLoginManager loginManager;

		public TenantFormAuthenticationMechanism(PersistentLoginManager loginManager) {
			super("/login", "/login-action", "username", "password", "/access-denied", "/", true, "quarkus-redirect-location", loginManager);
			this.loginManager = loginManager;
		}

		@Override
		public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
			if (context.request().path().endsWith("/logout")) {
				log.debugf("performing logout");
				loginManager.clear(context);
				return Uni.createFrom().nullItem();
			} else {
				return super.authenticate(context, identityProviderManager);
			}

		}

		protected void storeInitialLocation(final RoutingContext context) {
			String path = context.request().path();
			path = path.startsWith("/") ? path.substring(1) : path;
			String loc = context.request().scheme() + "://" + context.request().host() + tenantPath() + "/" + path;
			log.debugf("initial location %s", loc);
			context.response().addCookie(Cookie.cookie("quarkus-redirect-location", loc).setPath("/").setSecure(context.request().isSSL()));
		}

		@Override
		public Uni<ChallengeData> getChallenge(RoutingContext context) {
			if (context.normalizedPath().endsWith("/login-action") && context.request().method().equals(HttpMethod.POST)) {
				String loc = context.request().scheme() + "://" + context.request().host() + "/access-denied";
				return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
			} else {

				String loc = context.request().scheme() + "://" + context.request().host() + tenantPath() + "/login";
				log.debugf("form challenge redirect %s", loc);
				storeInitialLocation(context);
				loginManager.clear(context);
				return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
			}
		}

	}

}
