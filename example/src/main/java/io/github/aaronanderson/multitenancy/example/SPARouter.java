package io.github.aaronanderson.multitenancy.example;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.quarkus.arc.Arc;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;

@ApplicationScoped
public class SPARouter {

	private static final Logger log = Logger.getLogger(SPARouter.class);
	
	@ConfigProperty(name = "quarkus.profile")
	String profile;

	@Inject
	@CacheName("template-cache")
	Cache cache;

	public void setupRouter(@Observes @Priority(value = 1) Router router) {
		log.infof("Setting up routes\n");

		Handler<RoutingContext> notFound = template("not-found.html", "/", null);
		router.route("/not-found").handler(notFound);
		router.errorHandler(404, notFound);

		Handler<RoutingContext> accessDenied = template("access-denied.html", "/", null);
		router.route("/access-denied").handler(accessDenied);
		router.errorHandler(401, accessDenied);

		router.route("/mfa_login").handler(this::handleLogin);
		router.route("/mfa_logout").handler(template("logout.html", "/", null));

		router.route("/").handler(template("index.html", "/", null));

		/*
		boolean isLocal = Environment.valueOf(profile.toUpperCase()) == Environment.LOCAL;
		Tenant localTenant = new Tenant("tenant", Environment.LOCAL, new HashMap<>());
		
		
		Handler<RoutingContext> tenantRouteHandler = ctx -> {
		
			System.out.format("Tenant Route: %s\n", ctx.request().path());
		
			if (!ctx.get(REQUEST_ROUTED, false)) {
				if (isLocal) {
					ctx.put(CONTEXT_TENANT, localTenant);
				}
				Tenant tenantConfig = ctx.get(CONTEXT_TENANT);
				if (tenantConfig != null) {
					String tenantPath = "/" + tenantConfig.name();
					String path = ctx.request().path();
		
					if (path.startsWith(tenantPath)) {
						ctx.put(REQUEST_ROUTED, true);
						String localPath = path.substring(tenantPath.length());
						if (localPath.startsWith("/logout")) {
							ctx.redirect(TenantUtil.logoutRedirectURL(ctx));
						} else if (localPath.contains(".")) { // || localPath.startsWith("/graphql")
							ctx.reroute(localPath);
						} else if (localPath.isEmpty()) {
							// If needed send redirect for empty path to compensate for Vaadin Router issue with base URLs not ending with a slash
							ctx.response().putHeader(LOCATION, tenantPath + "/").setStatusCode(302).end();
						} else {
							template("index.html", tenantPath + "/", ctx, null);
						}
		
						return;
					}
				}
			}
		
			ctx.request().pause();
			ctx.next();
			ctx.request().resume();
		
		};
		
		router.route("/*").handler(tenantRouteHandler);*/

	}

	private void handleLogin(RoutingContext context) {
		//ViewAction action = context.get(MfaAuthConstants.AUTH_ACTION_KEY);
		//ViewStatus status = context.get(MfaAuthConstants.AUTH_STATUS_KEY);
		//log.infof("action: %s status: %s", action, status);
		template("login.html", "/", null).handle(context);
	}

	public String lookupTemplate(String templateName, String base) {
		return cache.get(templateName + "-" + base, k -> {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/resources/" + templateName);
			if (is != null) {
				try {
					String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					return template.replaceAll("@@BASE@@", base);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}).await().indefinitely();
	}

	private Handler<RoutingContext> template(String templateName, String base, Function<String, String> mapper) {
		//Arc.container().instance(String.class, TenantScoped.LITERAL);
		return new BlockingHandlerDecorator(ctx -> {
			HttpServerResponse response = ctx.response();
			String template = lookupTemplate(templateName, base);
			if (template != null) {
				if (mapper != null) {
					template = mapper.apply(template);
				}
				response.setStatusCode(200);
				response.setChunked(true);
				response.putHeader(CACHE_CONTROL, "no-store, no-cache, no-transform, must-revalidate, max-age=0");
				response.write(template);
			} else {
				response.setStatusCode(404);
			}

			ctx.response().end();
		}, true);
	}

}