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

		TenantContext context = (TenantContext) container.getActiveContext(TenantScoped.class);
		if (context != null) {
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

		TenantContext context = (TenantContext) container.getActiveContext(TenantScoped.class);

		if (context != null) {
			log.debugf("clearedContext - %s -%s", context.getState(), props);
		}
		log.debugf("clearedContext - not active");
		return null;
	}

	@Override
	public String getThreadContextType() {
		return "Tenant";
	}

	private static final class TenantContextSnapshot implements ThreadContextSnapshot {

		private final TenantContextState state;

		public TenantContextSnapshot(TenantContextState state) {
			this.state = state;
		}

		@Override
		public ThreadContextController begin() {
			ArcContainer container = Arc.container();
			if (container == null) {
				return NOOP;
			}
			log.debugf("Begining new thread context snapshot - %s %s", state);

			List<InjectableContext> contexts = Arc.container().getContexts(TenantScoped.class);
			if (contexts.size() != 1) {
				throw new ContextException(String.format("Unexpected TenantScope contexts count %d", contexts.size()));
			}
			TenantContext tenantContext = (TenantContext) contexts.get(0);
			TenantContextState priorState = (TenantContextState) tenantContext.getStateIfActive();
			TenantContextState targetState = state != null && state.isValid() ? state : null;

			if (priorState != null) {
				log.debugf("Propagating current active state %s -  %s", targetState, priorState);
				tenantContext.activate(targetState);
				return new TenantContextController(tenantContext, priorState);
			}
			log.debugf("No prior state, not propagating %s", targetState);
			return NOOP;

		}

	}

	private static final class TenantContextController implements ThreadContextController {

		private final ManagedContext tenantContext;
		private final InjectableContext.ContextState priorState;
		private final boolean destroyTenantContext;

		TenantContextController(ManagedContext tenantContext, ContextState stateToRestore) {
			this(tenantContext, stateToRestore, false);
		}

		// in case of ClearContextSnapshot, we want to destroy instances of the intermediate context
		TenantContextController(ManagedContext tenantContext, ContextState stateToRestore, boolean destroyRequestContext) {
			this.tenantContext = tenantContext;
			this.priorState = stateToRestore;
			this.destroyTenantContext = destroyRequestContext;
		}

		@Override
		public void endContext() throws IllegalStateException {
			log.debugf("Ending thread context snapshot - %s %s", destroyTenantContext, priorState);
			if (destroyTenantContext) {
				tenantContext.destroy();
			}
			// it is not necessary to deactivate the context first - just overwrite the previous state
			tenantContext.activate(priorState != null && priorState.isValid() ? priorState : null);
		}

	}
}
