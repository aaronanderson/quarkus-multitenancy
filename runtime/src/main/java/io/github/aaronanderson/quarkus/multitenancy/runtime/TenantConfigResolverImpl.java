package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class TenantConfigResolverImpl implements TenantConfigResolver {

	@ConfigProperty(name = "quarkus.profile")
	String profile;

	@Override
	public Uni<OidcTenantConfig> resolve(RoutingContext routingContext, OidcRequestContext<OidcTenantConfig> requestContext) {
		return Uni.createFrom().item(() -> {
			String tenantId = routingContext.get(TenantResolverHandler.CONTEXT_TENANT_ID);
			if (tenantId != null) {
				Map<String, Object> tenantDetails = routingContext.get(TenantResolverHandler.CONTEXT_TENANT);
				OidcTenantConfig oidcConfig = new OidcTenantConfig();
				oidcConfig.setApplicationType(ApplicationType.WEB_APP);
				oidcConfig.setTenantId((String) tenantId);
				oidcConfig.setAuthServerUrl((String) tenantDetails.get("authURL"));
				oidcConfig.setClientId((String) tenantDetails.get("clientId"));

				// Logout logout = new Logout();
				// logout.setPath(Optional.of("/" + tenantName + "/logout"));
				// oidcConfig.setLogout(logout);
				Authentication auth = new Authentication();
				auth.setRedirectPath("/" + tenantId + "/");
				auth.setCookiePath("/" + tenantId);
				auth.setRestorePathAfterRedirect(true);
				auth.setJavaScriptAutoredirect(true);
				auth.setScopes(List.of("profile", "openid", "email"));// "phone"
				auth.setSessionAgeExtension(Duration.of(15, ChronoUnit.MINUTES));
				oidcConfig.setAuthentication(auth);

				OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
				credentials.setSecret((String) tenantDetails.get("clientSecret"));
				oidcConfig.setCredentials(credentials);
				return oidcConfig;
			}
			return null;
		});
	}

}
