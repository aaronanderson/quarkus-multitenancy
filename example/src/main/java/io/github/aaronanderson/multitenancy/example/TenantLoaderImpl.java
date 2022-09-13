package io.github.aaronanderson.multitenancy.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantLoader;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class TenantLoaderImpl implements TenantLoader {

	@Override
	public Map<String, Object> load(String tenantId) {
		Map<String, Object> tenantConfig = new HashMap<>();
		if ("tenant1".equals(tenantId)) {
			tenantConfig.put("name", "Tenant 1");
			tenantConfig.put("color", "blue");
		} else if ("tenant2".equals(tenantId)) {
			tenantConfig.put("name", "Tenant 2");
			tenantConfig.put("color", "green");
		} else if ("tenant3".equals(tenantId)) {
			tenantConfig.put("name", "Tenant 3");
			tenantConfig.put("color", "orange");
		}

		return tenantConfig;

	}

}
