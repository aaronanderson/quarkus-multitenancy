package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ContextException;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantLoader;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantResolver;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;

@Recorder
public class TenantRecorder {

	private static final Logger log = Logger.getLogger(TenantRecorder.class);

	final RuntimeValue<TenantRunTimeConfig> config;

	public TenantRecorder(RuntimeValue<TenantRunTimeConfig> config) {
		this.config = config;
	}

	public RuntimeValue<DefaultTenantResolver> defaultResolver() {
		TenantRunTimeConfig runtimeConfig = config.getValue();
		return new RuntimeValue<>(new DefaultTenantResolver(runtimeConfig.resolverMode, runtimeConfig.excludePaths));
	}

	public RuntimeValue<DefaultTenantLoader> defaultLoader() {
		return new RuntimeValue<>(new DefaultTenantLoader());
	}

	public Handler<RoutingContext> tenantResolverHandler(BeanContainer beanContainer) {
		// Can't programmatically set cache setting on quarkus-cache (only name available on AdditionalCacheNameBuildItem), so directly create internal Caffine cache
		Cache<String, Map<String, Object>> tenantResolverCache = Caffeine.newBuilder().initialCapacity(20).maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();// expireAfterAccess(10, TimeUnit.MINUTES)

		TenantResolver tenantResolver = beanContainer.instance(TenantResolver.class);
		if (tenantResolver == null) {
			throw new IllegalStateException("TenantResolver implementation is unavailable");
		}
		TenantLoader tenantLoader = beanContainer.instance(TenantLoader.class);
		if (tenantLoader == null) {
			throw new IllegalStateException("TenantLoader implementation is unavailable");
		}
		Handler<RoutingContext> handler = new TenantResolverHandler(tenantResolver, tenantLoader, tenantResolverCache);
		return new BlockingHandlerDecorator(handler, true);
	}

	public Handler<RoutingContext> tenantPathHandler() {
		if (config.getValue().reroutePaths) {
			return new TenantPathHandler(config.getValue().rootRedirect);
		}
		return null;
	}

	public BeanContainerListener initTenantContext() {
		return new BeanContainerListener() {

			@Override
			public void created(BeanContainer container) {
				List<InjectableContext> contexts = Arc.container().getContexts(TenantScoped.class);
				if (contexts.size() != 1) {
					throw new ContextException(String.format("Unexpected TenantScope contexts count %d", contexts.size()));
				}
				TenantContext tenantContext = (TenantContext) contexts.get(0);
				tenantContext.setCurrentContext(Arc.container().getCurrentContextFactory().create(TenantScoped.class));
			}

		};

	}

}
