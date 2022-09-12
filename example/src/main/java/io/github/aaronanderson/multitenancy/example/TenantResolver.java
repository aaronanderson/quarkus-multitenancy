package io.github.aaronanderson.multitenancy.example;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.aaronanderson.multitenancy.example.Tenant.Environment;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class TenantResolver implements TenantConfigResolver {

	@ConfigProperty(name = "quarkus.profile")
	String profile;

	

	@Override
	public Uni<OidcTenantConfig> resolve(RoutingContext routingContext, OidcRequestContext<OidcTenantConfig> requestContext) {
//		return resolveTenant(routingContext).flatMap(t -> {
//			if (t != null) {
//				return Uni.createFrom().item(t.oidcConfig());
//			}
			return Uni.createFrom().nullItem();
//		});
	}

//	public Uni<Tenant> resolveTenant(RoutingContext routingContext) {
//
//		Uni<Tenant> cachedTenant = Uni.createFrom().item(() -> routingContext.get(Tenant.CONTEXT_TENANT));
//		Uni<String> tenantPath = Uni.createFrom().item(() -> {
//			String path = routingContext.request().path();
//			String[] parts = path.split("/");
//
//			if (parts.length > 0) {
//				return parts[1];
//			}
//			return null;
//		});
//		Uni<Tenant> tenant = tenantPath.flatMap(s -> {
//			if (s != null) {
//				Uni<Tenant> t2 = lookupTenant(s);
//				return t2.chain(t -> {
//					routingContext.put(Tenant.CONTEXT_TENANT, t);
//					return Uni.createFrom().item(t);
//				});
//			}
//			return Uni.createFrom().nullItem();
//		});
//		return cachedTenant.onItem().ifNull().switchTo(tenant);
//
//	}

}
