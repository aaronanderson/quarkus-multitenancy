package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ContextException;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantContext.TenantContextState;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;

//inspired by ArcContextProvider
public class TenantContextProvider implements ThreadContextProvider {

	private static final Logger log = Logger.getLogger(TenantContextProvider.class);

	private static final ThreadContextController NOOP = () -> {
	};

	@Override
	public ThreadContextSnapshot currentContext(Map<String, String> props) {
		ArcContainer container = Arc.container();
		if (container == null) {
			return null;
		}

		TenantContext context = currentTenantContext();
		if (context.isActive()) {
			TenantContextState state = (TenantContextState) context.getState();
			log.debugf("currentContext - %s - %s", state, props);
			return new TenantContextSnapshot(state);
		}
		log.debugf("currentContext - not active");
		return null;

	}

	@Override
	public ThreadContextSnapshot clearedContext(Map<String, String> props) {

		ArcContainer container = Arc.container();
		if (container == null) {
			return null;
		}

		TenantContext context = currentTenantContext();
		if (context.isActive()) {
			log.debugf("clearedContext - deactivating active context %s -%s", context.getState(), props);
			context.deactivate();
		} else {
			log.debugf("clearedContext - not active");
		}

		return null;
	}

	@Override
	public String getThreadContextType() {
		return "Tenant";
	}

	private static final class TenantContextSnapshot implements ThreadContextSnapshot {

		private final TenantContextState snapshotState;

		public TenantContextSnapshot(TenantContextState state) {
			this.snapshotState = state;
		}

		@Override
		public ThreadContextController begin() {
			ArcContainer container = Arc.container();
			if (container == null) {
				return NOOP;
			}
			log.debugf("begining new thread context snapshot - %s", snapshotState);

			TenantContext tenantContext = currentTenantContext();
			TenantContextState existingState = (TenantContextState) tenantContext.getStateIfActive();
			TenantContextState targetState = snapshotState != null && snapshotState.isValid() ? snapshotState : null;

			if (existingState != null) {
				if (targetState != null) {
					log.debugf("Propagatated to active state %s - %s", tenantContext, targetState);
					tenantContext.activate(targetState);
				} else {
					log.debugf("Propagatated state is invalid, deactivating  %s", tenantContext);
					tenantContext.deactivate();
				}
				return new TenantContextController(existingState);
			} else {
				if (targetState != null) {
					log.debugf("Propagatated to inactive state %s - %s", tenantContext, targetState);
					tenantContext.activate(targetState);
				} else {
					log.debugf("Propagatated state is invalid, context inactive  %s", tenantContext);
				}
				return NOOP;
			}
		}

	}

	private static TenantContext currentTenantContext() {
		List<InjectableContext> contexts = Arc.container().getContexts(TenantScoped.class);
		if (contexts.size() != 1) {
			throw new ContextException(String.format("Unexpected TenantScope contexts count %d", contexts.size()));
		}
		return (TenantContext) contexts.get(0);
	}

	private static final class TenantContextController implements ThreadContextController {

		private final TenantContextState priorState;

		// in case of ClearContextSnapshot, we want to destroy instances of the intermediate context
		TenantContextController(TenantContextState priorState) {
			this.priorState = priorState;
		}

		@Override
		public void endContext() throws IllegalStateException {
			TenantContext currentTenantContext = currentTenantContext();
			TenantContextState targetState = priorState != null && priorState.isValid() ? priorState : null;
			if (targetState != null) {
				log.debugf("Ending thread context - restoring - %s", targetState);
				currentTenantContext.activate(targetState);
			} else if (currentTenantContext.isActive()) {
				log.debugf("Ending thread context - deactivating");
				currentTenantContext.deactivate();
			}

		}

	}
}
