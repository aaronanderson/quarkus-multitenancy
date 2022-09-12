package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

public interface TenantResolver {

	Optional<String> resolve(RoutingContext routingContext);

}
