package io.github.aaronanderson.quarkus.multitenancy.runtime;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TenantPathHandler implements Handler<RoutingContext> {

	public static final String REQUEST_ROUTED = "io.github.aaronanderson.quarkus.multitenancy.tenant-routed";

	private static final Logger log = Logger.getLogger(TenantPathHandler.class);

	@Override
	public void handle(RoutingContext ctx) {
		log.debugf("handle %s", ctx.request().path());

		if (!ctx.get(REQUEST_ROUTED, false)) {
			String tenantId = ctx.get(TenantResolverHandler.CONTEXT_TENANT_ID);
			if (tenantId != null) {
				String tenantPath = "/" + tenantId;
				String path = ctx.request().path();

				if (path.startsWith(tenantPath)) {
					ctx.put(REQUEST_ROUTED, true);
					String localPath = path.substring(tenantPath.length());
					log.debugf("Rerouting path %s to %s", path, localPath);
					ctx.reroute(localPath);
					return;
				}
			}
		}

		ctx.request().pause();
		ctx.next();
		ctx.request().resume();

	}

}
