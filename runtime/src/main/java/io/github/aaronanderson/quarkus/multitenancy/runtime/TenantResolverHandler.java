package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TenantResolverHandler implements Handler<RoutingContext> {

	public static final String CONTEXT_TENANT_ID = "io.github.aaronanderson.quarkus.multitenancy.tenant-id";
	public static final String CONTEXT_TENANT = "io.github.aaronanderson.quarkus.multitenancy.tenant";

	private static final Logger log = Logger.getLogger(TenantResolverHandler.class);

	private final TenantResolver tenantResolver;
	private final TenantLoader tenantLoader;
	private final Cache<String, Map<String, Object>> tenantResolverCache;

	TenantResolverHandler(TenantResolver tenantResolver, TenantLoader tenantLoader, Cache<String, Map<String, Object>> tenantResolverCache) {
		this.tenantResolver = tenantResolver;
		this.tenantLoader = tenantLoader;
		this.tenantResolverCache = tenantResolverCache;
	}

	@Override
	public void handle(RoutingContext ctx) {
		log.debugf("handle %s", ctx.request().path());
		Optional<String> tenantId = tenantResolver.resolve(ctx);
		if (tenantId.isPresent()) {
			Map<String, Object> tenantDetails = tenantResolverCache.get(tenantId.get(), k -> {
				return tenantLoader.load(k);
			});
			if (tenantDetails != null && !tenantDetails.isEmpty()) {
				log.debugf("Loaded details for tenant ID %s", tenantId.get());
				ctx.put(CONTEXT_TENANT_ID, tenantId.get());
				ctx.put(CONTEXT_TENANT, tenantDetails);
			}
		}
		ctx.next();
	}

	static enum ResolverMode {
		PATH, SUBDOMAIN
	}

	public static class DefaultTenantResolver implements TenantResolver {

		private final ResolverMode mode;
		private final List<String> excludePaths;

		DefaultTenantResolver(ResolverMode mode, Optional<List<String>> excludePaths) {
			this.mode = mode;
			this.excludePaths = excludePaths.orElse(Collections.EMPTY_LIST);
		}

		@Override
		public Optional<String> resolve(RoutingContext routingContext) {
			if (mode == ResolverMode.SUBDOMAIN) {
				String host = routingContext.request().host();
				String[] parts = host.split("\\.");
				if (parts.length > 0) {
					log.debugf("Resolved tenant %s from host %s", parts[1], host);
					return Optional.of(parts[1]);
				}
			} else {
				String path = routingContext.request().path();
				String[] parts = path.split("/");
				if (parts.length > 0) {
					String candidate = parts[1];
					if (!candidate.contains(".") && !excludePaths.contains(candidate)) {
						log.debugf("Resolved tenant %s from path %s", parts[1], path);
						return Optional.of(parts[1]);
					}
				}
			}
			return Optional.empty();
		}
	}

	public static class DefaultTenantLoader implements TenantLoader {

		@Override
		public Map<String, Object> load(String tenantId) {
			log.warnf("Default TenantLoader being used.");
			return Collections.EMPTY_MAP;
		}

	}

}
