package io.github.aaronanderson.multitenancy.example;

import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProperty;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.quarkus.arc.Arc;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.vertx.http.runtime.filters.accesslog.LogFileHeaderGenerator;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
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
		router.route("/favicon.ico").handler(notFound);

		Handler<RoutingContext> accessDenied = template("access-denied.html", "/", null);
		router.route("/access-denied").handler(accessDenied);
		router.errorHandler(401, accessDenied);

		router.route("/login").handler(this::handleLogin);
		router.route("/logout").handler(this::handleLogout);

		router.route("/").handler(this::handleRoot);
		router.route("/home").handler(this::handleHome);

	}

	private void handleRoot(RoutingContext context) {
		String tenantId = getTenantId();
		log.infof("handleRoot: %s", tenantId);
		if (tenantId != null) {
			context.reroute("/home");
		} else {
			QuarkusHttpUser quser = (QuarkusHttpUser) context.user();
			final String user = quser.principal().getString("username");
			template("index.html", "/", (t, c) -> {
				t = t.replace("@@USER@@", user);
				return t;
			}).handle(context);
		}
	}

	private void handleHome(RoutingContext context) {
		QuarkusHttpUser quser = (QuarkusHttpUser) context.user();
		final String user = quser.principal().getString("username");
		String tenantId = getTenantId();
		log.infof("handleHome: %s", tenantId);
		if (tenantId != null) {
			String tenantColor = getTenantProperty("color", "red");
			template("tenant.html", "/" + tenantId + "/", (t, c) -> {
				t = t.replace("@@USER@@", user);
				t = t.replace("@@TENANT_ID@@", tenantId);
				t = t.replace("@@TENANT_COLOR@@", tenantColor);
				return t;
			}).handle(context);
		} else {
			context.reroute("/access-denied");
		}

	}

	private void handleLogin(RoutingContext context) {
		String tenantId = getTenantId();
		log.infof("handleLogin: %s", tenantId);
		if (tenantId != null) {
			template("login.html", "/" + tenantId + "/", null).handle(context);
		} else {
			template("login.html", "/", null).handle(context);
		}

	}

	private void handleLogout(RoutingContext context) {
		String tenantId = getTenantId();
		log.infof("handleLogout: %s", tenantId);
		template("logout.html", "/", null).handle(context);
	}

	public String lookupTemplate(String templateName, String base) {
		return cache.get(templateName + "-" + base, k -> {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/META-INF/resources/" + templateName);
			if (is != null) {
				try {
					String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					template = template.replaceAll("@@BASE@@", base);
					return template;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}).await().indefinitely();
	}

	private String getTenantId() {
		try {
			return Arc.container().instance(String.class, TenantId.LITERAL).get();
		} catch (ContextNotActiveException ce) {
			return null;
		}
	}

	private String getTenantProperty(String name, String defaultValue) {
		// Arc.container().instance() does not pass qualifiers to the producer's InjectionPoint parameter. Perhaps a defect.
		InjectableInstance<String> colorInstance = Arc.container().select(String.class, new TenantProperty.Literal("color"));
		String color = colorInstance.get();
		if (color != null) {
			colorInstance.destroy(color);
		}
		return color;
	}

	private Handler<RoutingContext> template(String templateName, String base, BiFunction<String, RoutingContext, String> mapper) {

		return new BlockingHandlerDecorator(ctx -> {

			HttpServerResponse response = ctx.response();
			String template = lookupTemplate(templateName, base);
			if (template != null) {
				if (mapper != null) {
					template = mapper.apply(template, ctx);
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