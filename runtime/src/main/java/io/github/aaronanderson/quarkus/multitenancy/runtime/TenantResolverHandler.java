package io.github.aaronanderson.quarkus.multitenancy.runtime;

import static io.github.aaronanderson.quarkus.multitenancy.runtime.TenantConfig.CONTEXT_TENANT;
import static io.github.aaronanderson.quarkus.multitenancy.runtime.TenantConfig.CONTEXT_TENANT_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import jakarta.enterprise.context.ContextException;

import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TenantResolverHandler implements Handler<RoutingContext> {

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
		// Rerouted requests will already have the RoutingCountext data set and an active CDI TenantScope TenantContext
		log.debugf("handle %s", ctx.request().path());

		String tenantId = ctx.get(CONTEXT_TENANT_ID);
		Map<String, Object> tenantConfig = ctx.get(CONTEXT_TENANT);
		if (tenantConfig == null) {
			Optional<String> resolvedTenantId = tenantResolver.resolve(ctx);
			if (resolvedTenantId.isPresent()) {
				tenantId = resolvedTenantId.get();
				tenantConfig = tenantResolverCache.get(tenantId, k -> {
					return tenantLoader.load(k);
				});
				if (tenantConfig != null && !tenantConfig.isEmpty()) {
					log.debugf("Loaded details for tenant ID %s", tenantId);
					ctx.put(CONTEXT_TENANT_ID, tenantId);
					ctx.put(CONTEXT_TENANT, tenantConfig);
				} else {
					tenantId = null;
					tenantConfig = null;
				}
			}
		}

		if (tenantId != null && tenantConfig != null) {
			List<InjectableContext> contexts = Arc.container().getContexts(TenantScoped.class);
			if (contexts.size() != 1) {
				throw new ContextException(String.format("Unexpected TenantScope contexts count %d", contexts.size()));
			}
			TenantContext tenantContext = (TenantContext) contexts.get(0);

			if (!tenantContext.isActive()) {
				tenantContext.activate();
				log.debugf("activate %s", tenantContext.getState());
				InstanceHandle<TenantProvider> tenantProvider = Arc.container().instance(TenantProvider.class);
				tenantProvider.get().setTenantConfig(tenantId, tenantConfig);

				// force the end handler to run on the current Vert.x thread the tenant context was activated on.
				ctx.addEndHandler(v -> ctx.vertx().runOnContext(v2 -> {
					TenantContext termContext = (TenantContext) Arc.container().getActiveContext(TenantScoped.class);
					if (termContext != null) {
						log.debugf("terminateHook - terminating active context %s", termContext.getState());
						termContext.terminate();
					} else {
						log.debugf("terminateHook - not active");
					}

				}));
				ctx.request().pause();
				ctx.next();
				// log.debugf("terminate %s", tenantContext.getState());
				// tenantContext.terminate();
				ctx.request().resume();
				return;
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
					log.debugf("Resolved candidate tenant %s from host %s", parts[1], host);
					return Optional.of(parts[1]);
				}
			} else {
				String path = routingContext.request().path();
				String[] parts = path.split("/");
				if (parts.length > 0) {
					String candidate = parts[1];
					if (!candidate.contains(".") && !excludePaths.contains("/" + candidate)) {
						log.debugf("Resolved candidate tenant %s from path %s", parts[1], path);
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
