package io.github.aaronanderson.multitenancy.example;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

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
			tenantConfig.put("oidc-enabled", false);
		} else if ("tenant2".equals(tenantId)) {
			tenantConfig.put("name", "Tenant 2");
			tenantConfig.put("color", "green");
			tenantConfig.put("oidc-enabled", false);
		} else if ("tenant3".equals(tenantId)) {
			tenantConfig.put("name", "Tenant 3");
			tenantConfig.put("color", "orange");
			tenantConfig.put("oidc-enabled", false);
			tenantConfig.put("oidc-auth-url", "http://localhost:8081/realms/quarkus");
			tenantConfig.put("oidc-client-id", "quarkus-app");
			tenantConfig.put("oidc-client-secret", "secret");
		}
		return tenantConfig;
	}
	
}
