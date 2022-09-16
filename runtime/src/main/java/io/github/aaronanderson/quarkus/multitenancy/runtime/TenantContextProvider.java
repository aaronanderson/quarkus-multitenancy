package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ContextException;

import org.eclipse.microprofile.context.ThreadContext;
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

	@Override
	public ThreadContextSnapshot currentContext(Map<String, String> props) {
		return newSnapshot(Mode.CURRENT);

	}

	@Override
	public ThreadContextSnapshot clearedContext(Map<String, String> props) {
		return newSnapshot(Mode.CLEAR);
	}

	private TenantContextSnapshot newSnapshot(Mode mode) {
		log.debugf("creating new thread context snapshot - %s", mode);
		ArcContainer container = Arc.container();
		if (container == null) {
			return null;
		}
		TenantContextState state = null;
		TenantContext context = (TenantContext) container.getActiveContext(TenantScoped.class);
		if (context != null) {
			state = (TenantContextState) context.getState();
		}

		return new TenantContextSnapshot(state, mode);
	}

	@Override
	public String getThreadContextType() {
		return "Tenant";
	}

	private static enum Mode {
		CURRENT, CLEAR;
	}

	private static final class TenantContextSnapshot implements ThreadContextSnapshot {

		private final TenantContextState state;
		private final Mode mode;

		public TenantContextSnapshot(TenantContextState state, Mode mode) {
			this.state = state;
			this.mode = mode;
		}

		@Override
		public ThreadContextController begin() {
			ArcContainer container = Arc.container();
			if (container == null) {
				return () -> {
				};
			}
			log.debugf("Begining new thread context snapshot - %s %s", mode, state);

			List<InjectableContext> contexts = Arc.container().getContexts(TenantScoped.class);
			if (contexts.size() != 1) {
				throw new ContextException(String.format("Unexpected TenantScope contexts count %d", contexts.size()));
			}
			TenantContext tenantContext = (TenantContext) contexts.get(0);
			InjectableContext.ContextState currentState = tenantContext.getStateIfActive();
			// this is executed on another thread, context can but doesn't need to be active here

			if (currentState != null) {
				if (mode == Mode.CLEAR) {
					return tenantContext::deactivate;
				} else {
					tenantContext.activate(state != null && state.isValid() ? state : null);
					return new TenantContextController(tenantContext, currentState);
				}

			} else {
				tenantContext.activate(state != null && state.isValid() ? state : null);
				return tenantContext::deactivate;
			}
		}

	}

	private static final class TenantContextController implements ThreadContextController {

		private final ManagedContext tenantContext;
		private final InjectableContext.ContextState stateToRestore;
		private final boolean destroyTenantContext;

		TenantContextController(ManagedContext tenantContext, ContextState stateToRestore) {
			this(tenantContext, stateToRestore, false);
		}

		// in case of ClearContextSnapshot, we want to destroy instances of the intermediate context
		TenantContextController(ManagedContext tenantContext, ContextState stateToRestore, boolean destroyRequestContext) {
			this.tenantContext = tenantContext;
			this.stateToRestore = stateToRestore;
			this.destroyTenantContext = destroyRequestContext;
		}

		@Override
		public void endContext() throws IllegalStateException {
			log.debugf("Ending thread context snapshot - %s %s", destroyTenantContext, stateToRestore);
			if (destroyTenantContext) {
				tenantContext.destroy();
			}
			// it is not necessary to deactivate the context first - just overwrite the previous state
			tenantContext.activate(stateToRestore != null && stateToRestore.isValid() ? stateToRestore : null);
		}

	}
}
