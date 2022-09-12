package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.lang.annotation.Annotation;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext;

public class TenantContext implements InjectableContext {

	private static final Logger log = Logger.getLogger(TenantContext.class);

	@Override
	public void destroy(Contextual<?> contextual) {
		log.infof("destroy");

	}

	@Override
	public Class<? extends Annotation> getScope() {
		return TenantScoped.class;
	}

	@Override
	public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		log.infof("get");
		return null;
	}

	@Override
	public <T> T get(Contextual<T> contextual) {
		log.infof("get");
		return null;
	}

	@Override
	public boolean isActive() {
		log.infof("isActive");
		return false;
	}

	@Override
	public void destroy() {
		log.infof("destroy");

	}

	@Override
	public ContextState getState() {
		log.infof("getStatus");
		return null;
	}

}
