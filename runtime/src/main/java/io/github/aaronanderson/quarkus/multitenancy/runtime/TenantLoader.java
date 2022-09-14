package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.Map;

public interface TenantLoader {

	Map<String, Object> load(String tenantId);

}
