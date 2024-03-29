package io.github.aaronanderson.quarkus.multitenancy.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.DotName;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantConfig;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantContext;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantLoader;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProperty;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantProvider;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantRecorder;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolver;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantLoader;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantResolver;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

class QuarkusMultitenancyProcessor {

	private static final String FEATURE = "multitenancy";

	@BuildStep
	FeatureBuildItem feature() {
		return new FeatureBuildItem(FEATURE);
	}

	@BuildStep
	public void myBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
		AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
		builder.addBeanClass(TenantProvider.class);
		additionalBeans.produce(builder.build());
	}

	@BuildStep
	void autoInjectMetric(BuildProducer<AutoInjectAnnotationBuildItem> autoInjectAnnotationBuildItemBuildProducer) {
		autoInjectAnnotationBuildItemBuildProducer.produce(new AutoInjectAnnotationBuildItem(DotName.createSimple(TenantId.class.getName())));
		autoInjectAnnotationBuildItemBuildProducer.produce(new AutoInjectAnnotationBuildItem(DotName.createSimple(TenantConfig.class.getName())));
		autoInjectAnnotationBuildItemBuildProducer.produce(new AutoInjectAnnotationBuildItem(DotName.createSimple(TenantProperty.class.getName())));
	}

	@BuildStep
	@Record(ExecutionTime.STATIC_INIT)
	void setupAuthenticationMechanisms(TenantRecorder recorder, BuildProducer<BeanContainerListenerBuildItem> beanContainerListenerBuildItemBuildProducer) {
		beanContainerListenerBuildItemBuildProducer.produce(new BeanContainerListenerBuildItem(recorder.initTenantContext()));
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	void defaultTenantImplementations(TenantRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
		syntheticBeans.produce(SyntheticBeanBuildItem.configure(DefaultTenantResolver.class).defaultBean().unremovable().addType(TenantResolver.class).scope(ApplicationScoped.class).runtimeValue(recorder.defaultResolver()).setRuntimeInit().done());
		syntheticBeans.produce(SyntheticBeanBuildItem.configure(DefaultTenantLoader.class).defaultBean().unremovable().addType(TenantLoader.class).scope(ApplicationScoped.class).runtimeValue(recorder.defaultLoader()).setRuntimeInit().done());

	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	void tenantFilters(TenantRecorder recorder, BeanContainerBuildItem beanContainer, BuildProducer<FilterBuildItem> filters) {
		filters.produce(new FilterBuildItem(recorder.tenantResolverHandler(beanContainer.getValue()), FilterBuildItem.AUTHENTICATION + 2));
		filters.produce(new FilterBuildItem(recorder.tenantPathHandler(), FilterBuildItem.AUTHENTICATION + 1));
	}

	@BuildStep
	public ContextConfiguratorBuildItem transactionContext(ContextRegistrationPhaseBuildItem contextRegistrationPhase) {
		return new ContextConfiguratorBuildItem(contextRegistrationPhase.getContext().configure(TenantScoped.class).normal().contextClass(TenantContext.class));
	}

	@BuildStep
	public CustomScopeBuildItem registerScope() {
		return new CustomScopeBuildItem(TenantScoped.class);
	}
}
