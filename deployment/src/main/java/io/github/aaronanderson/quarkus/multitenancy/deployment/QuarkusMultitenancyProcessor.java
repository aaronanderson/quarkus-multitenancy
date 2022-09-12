package io.github.aaronanderson.quarkus.multitenancy.deployment;

import javax.enterprise.context.ApplicationScoped;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantContext;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantLoader;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantRecorder;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolver;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantLoader;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.DefaultTenantResolver;
import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantScoped;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.RuntimeValue;
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
		builder.addBeanClass(TenantResolver.class);
		builder.addBeanClass(TenantLoader.class);
		additionalBeans.produce(builder.build());
	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	void defaultTenantResolver(TenantRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
		RuntimeValue<DefaultTenantResolver> defaultResolver = recorder.defaultResolver();
		RuntimeValue<DefaultTenantLoader> defaultLoader = recorder.defaultLoader();
		syntheticBeans.produce(SyntheticBeanBuildItem.configure(DefaultTenantResolver.class).defaultBean().addType(TenantResolver.class).scope(ApplicationScoped.class).runtimeValue(defaultResolver).unremovable().setRuntimeInit().done());
		syntheticBeans.produce(SyntheticBeanBuildItem.configure(DefaultTenantLoader.class).defaultBean().addType(TenantLoader.class).scope(ApplicationScoped.class).runtimeValue(defaultLoader).unremovable().setRuntimeInit().done());

	}

	@BuildStep
	@Record(ExecutionTime.RUNTIME_INIT)
	void tenantResolver(TenantRecorder recorder, BeanContainerBuildItem beanContainer, BuildProducer<FilterBuildItem> filters) {
		filters.produce(new FilterBuildItem(recorder.tenantResolverHandler(beanContainer.getValue()), FilterBuildItem.AUTHENTICATION + 1));
		filters.produce(new FilterBuildItem(recorder.tenantPathHandler(), FilterBuildItem.AUTHENTICATION));
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
