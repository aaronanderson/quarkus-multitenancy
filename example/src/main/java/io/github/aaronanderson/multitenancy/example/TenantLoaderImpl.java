package io.github.aaronanderson.multitenancy.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantLoader;

@ApplicationScoped
public class TenantLoaderImpl implements TenantLoader {

	@Override
	public Map<String, Object> load(String tenantId) {
		Map<String, Object> tenantDetails = new HashMap<>();
		if ("tenant1".equals(tenantId)) {
			return tenantDetails;
		}

		return Collections.EMPTY_MAP;
	}

}
