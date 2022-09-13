package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

@TenantScoped
public class TenantProvider {

	private String tenantId;
	private Map<String, Object> tenantConfig;

	void setTenantConfig(String tenantId, Map<String, Object> tenantConfig) {
		this.tenantId = tenantId;
		this.tenantConfig = tenantConfig;
	}

	@Dependent
	@Produces
	@TenantProperty
	public Object produceTenantProperty(InjectionPoint injectionpoint) {
		for (Annotation a : injectionpoint.getQualifiers()) {
			if (a.annotationType().equals(TenantProperty.class)) {
				TenantProperty property = (TenantProperty) a;
				Object value = this.tenantConfig.get(property.name());
				if (value == null && !TenantProperty.UNCONFIGURED_VALUE.equals(property.defaultValue())) {
					value = property.defaultValue();
				}
				return value;
			}
		}
		return null;
	}

	@Dependent
	@Produces
	@TenantConfig
	public Map<String, Object> produceTenantConfig() {
		return this.tenantConfig;
	}

	@Dependent
	@Produces
	@TenantId
	public Object produceTenantId() {
		return this.tenantId;
	}
}
