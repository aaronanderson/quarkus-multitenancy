package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.Map;

public interface TenantLoader {

	// public static final String CONTEXT_TENANT_ID = "io.github.aaronanderson.quarkus.multitenancy.tenant-id";
	// public static final String CONTEXT_TENANT = "io.github.aaronanderson.quarkus.multitenancy.tenant";

	Map<String, Object> load(String tenantId);

}
