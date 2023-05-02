package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.ContextInstanceHandleImpl;
import io.quarkus.arc.impl.CreationalContextImpl;

//Inspired by RequstContext
public class TenantContext implements ManagedContext {

	private static final Logger log = Logger.getLogger(TenantContext.class);

	private CurrentContext<TenantContextState> currentContext;

	public void setCurrentContext(CurrentContext<TenantContextState> currentContext) {
		this.currentContext = currentContext;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return TenantScoped.class;
	}

	@Override
	public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		TenantContextState ctxState = currentContext.get();
		if (ctxState == null) {
			throw new ContextNotActiveException("TenantScope not activated");
		}
		ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctxState.map.get(contextual);
		if (instance == null) {
			CreationalContextImpl<T> creationalContextFun = CreationalContextImpl.unwrap(creationalContext);
			CreationalContext<T> newCreationalContext = creationalContextFun.apply(contextual);
			// Bean instance does not exist - create one if we have CreationalContext
			instance = new ContextInstanceHandleImpl<T>((InjectableBean<T>) contextual, contextual.create(newCreationalContext), newCreationalContext);
			ctxState.map.put(contextual, instance);
		}
		return instance.get();
	}

	@Override
	public <T> T get(Contextual<T> contextual) {
		InjectableBean<T> bean = (InjectableBean<T>) contextual;
		TenantContextState state = currentContext.get();
		if (state == null) {
			throw new ContextNotActiveException("TenantScope not activated");
		}
		ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) state.map.get(contextual);
		return instance == null ? null : instance.get();
	}

	@Override
	public void destroy(Contextual<?> contextual) {
		TenantContextState state = currentContext.get();
		if (state == null) {
			throw new ContextNotActiveException("TenantScope not activated");
		}
		ContextInstanceHandle<?> instance = state.map.remove(contextual);
		if (instance != null) {
			instance.destroy();
		}
	}

	@Override
	public boolean isActive() {
		return currentContext.get() != null;
	}

	@Override
	public void destroy() {
		destroy(currentContext.get());

	}

	@Override
	public ContextState getState() {
		TenantContextState state = currentContext.get();
		if (state == null) {
			throw new ContextNotActiveException("TenantScope not activated");
		}
		return state;
	}

	@Override
	public void activate(ContextState initialState) {
		if (initialState == null) {
			currentContext.set(new TenantContextState(new ConcurrentHashMap<>()));
		} else {
			if (initialState instanceof TenantContextState) {
				currentContext.set((TenantContextState) initialState);
			} else {
				throw new IllegalArgumentException("Invalid initial state: " + initialState.getClass().getName());
			}
		}

		log.debugf("activate %s", currentContext.get());

	}

	@Override
	public void deactivate() {
		TenantContextState state = currentContext.get();
		log.debugf("deactivate %s", state);
		state.isValid = false;
		currentContext.remove();
	}

	static class TenantContextState implements ContextState {

		private final Map<Contextual<?>, ContextInstanceHandle<?>> map;

		private volatile boolean isValid;

		TenantContextState(ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value) {
			this.map = Objects.requireNonNull(value);
			this.isValid = true;
		}

		@Override
		public Map<InjectableBean<?>, Object> getContextualInstances() {
			return map.values().stream().collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
		}

		@Override
		public boolean isValid() {
			return isValid;
		}

	}

}
