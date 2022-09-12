package io.github.aaronanderson.multitenancy.example;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Authentication;
import io.vertx.ext.web.RoutingContext;

public class Tenant {

	public static final String CONTEXT_TENANT = "application-tenant";
	public static final String REQUEST_ROUTED = "application-tenant-routed";

	private final String name;
	private final Environment environment;
	private final Map<String, String> attributes;
	private final OidcTenantConfig oidcConfig;

	public Tenant(String tenantName, Environment environment, Map<String, String> attributes) {
		this.name = tenantName;
		this.environment = environment;
		this.attributes = attributes;
		if (environment == Environment.LOCAL) {
			oidcConfig = null;
		} else {
			oidcConfig = buildOIDCConfig(tenantName, attributes);
		}
	}

	public OidcTenantConfig oidcConfig() {
		return oidcConfig;
	}

	public String name() {
		return name;
	}

	public Environment environment() {
		return environment;
	}

	public boolean oidcEnabled() {
		return Boolean.valueOf(attributes.getOrDefault("oidc-enabled", "false"));
	}

	public Map<String, String> attributes() {
		return attributes;
	}

	public static enum Environment {
		PROD, STAGING, DEV, LOCAL;
	}

	public static OidcTenantConfig buildOIDCConfig(String tenantName, Map<String, String> attributes) {
		OidcTenantConfig oidcConfig = new OidcTenantConfig();
		oidcConfig.setApplicationType(ApplicationType.WEB_APP);
		oidcConfig.setTenantId(attributes.get("id"));
		oidcConfig.setAuthServerUrl(attributes.get("authURL"));
		oidcConfig.setClientId(attributes.get("clientId"));

		// Logout logout = new Logout();
		// logout.setPath(Optional.of("/" + tenantName + "/logout"));
		// oidcConfig.setLogout(logout);
		Authentication auth = new Authentication();
		auth.setRedirectPath("/" + tenantName + "/");
		auth.setCookiePath("/" + tenantName);
		auth.setRestorePathAfterRedirect(true);
		auth.setJavaScriptAutoredirect(true);
		auth.setScopes(List.of("profile", "openid", "email"));// "phone"
		auth.setSessionAgeExtension(Duration.of(15, ChronoUnit.MINUTES));
		oidcConfig.setAuthentication(auth);

		OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
		credentials.setSecret(attributes.get("clientSecret"));
		oidcConfig.setCredentials(credentials);
		return oidcConfig;
	}

	public static String logoutRedirectURL(RoutingContext ctx) {
		Tenant tenantConfig = ctx.get(CONTEXT_TENANT);
		String redirectURI = String.format("%s://%s/%s/", ctx.request().scheme(), ctx.request().host(), tenantConfig.name());
		if (tenantConfig.environment() == Environment.LOCAL) {
			ctx.response().putHeader("Clear-Site-Data", "cookies");
			return redirectURI;
		} else {
			String domain = tenantConfig.attributes().get("domain");
			String clientId = tenantConfig.attributes().get("clientId");
			return String.format("https://%s.auth.us-east-1.amazoncognito.com/logout?response_type=code&client_id=%s&redirect_uri=%s&scope=openid+profile", domain, clientId, redirectURI);
		}

	}

}