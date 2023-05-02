package io.github.aaronanderson.multitenancy.example;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantConfig;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProperty;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.quarkus.arc.Arc;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class TenantConfigResolverImpl implements TenantConfigResolver {

//	@Inject
//	@TenantConfig
//	Map<String, Object> tenantConfig;

	@Override
	public Uni<OidcTenantConfig> resolve(RoutingContext routingContext, OidcRequestContext<OidcTenantConfig> requestContext) {
		return Uni.createFrom().item(() -> {
			String tenantId = routingContext.get(TenantConfig.CONTEXT_TENANT_ID);
			if (tenantId != null) {
				// Can obtain tenant configuration through injection or from the route
				Map<String, Object> tenantConfig = routingContext.get(TenantConfig.CONTEXT_TENANT);
				OidcTenantConfig oidcConfig = new OidcTenantConfig();
				oidcConfig.setApplicationType(ApplicationType.WEB_APP);
				oidcConfig.setTenantId((String) tenantId);
				oidcConfig.setAuthServerUrl((String) tenantConfig.get("oidc-auth-url"));
				oidcConfig.setClientId((String) tenantConfig.get("oidc-client-id"));

				//TODO create a quarkus-realm.json for web-app authentication.
				//Need to log into http://localhost:8081 as admin/admin and edit the quarkus-app client. Set the Base URL to http://localhost:8080/tenant3/
				//and add the profile and email client scopes
				Authentication auth = new Authentication();
				auth.setRedirectPath("/" + tenantId + "/");
				auth.setCookiePath("/" + tenantId);
				auth.setRestorePathAfterRedirect(true);
				auth.setJavaScriptAutoredirect(true);
				auth.setScopes(List.of("profile", "email"));// "phone" "openid",
				auth.setSessionAgeExtension(Duration.of(15, ChronoUnit.MINUTES));
				oidcConfig.getToken().setRefreshExpired(true);
				oidcConfig.getToken().setRefreshTokenTimeSkew(Duration.of(15, ChronoUnit.MINUTES));

				oidcConfig.setAuthentication(auth);

				OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
				credentials.setSecret((String) tenantConfig.get("oidc-client-secret"));
				oidcConfig.setCredentials(credentials);
				return oidcConfig;
			}
			return null;
		});
	}

}
