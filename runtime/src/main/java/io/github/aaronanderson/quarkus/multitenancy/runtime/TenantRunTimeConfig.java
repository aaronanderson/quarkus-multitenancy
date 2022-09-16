package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.util.List;
import java.util.Optional;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantResolverHandler.ResolverMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.DefaultConverter;

@ConfigRoot(name = "multitenancy", phase = ConfigPhase.RUN_TIME)
public class TenantRunTimeConfig {

	/**
	* Standard tenant resolution mode, PATH or SUBDOMAIN
	*/
	@DefaultConverter
	@ConfigItem(name = "resolve-mode", defaultValue = "PATH")
	ResolverMode resolverMode;

	/**
	* If base path is used to resolve tenants automatically reroute requests to the root path
	*/
	@ConfigItem(name = "reroute-paths", defaultValue = "true")
	public boolean reroutePaths;
	
	/**
	* If the root tenant request does not end in a slash redirect. This is useful for some SPA frameworks.  i.e. /tenant1 -> /tenant1/ 
	*/
	@ConfigItem(name = "root-redirect", defaultValue = "true")
	public boolean rootRedirect;

	/**
	* If base path is used to resolve tenants specify a list of root paths that should be ignored during tenant resolution
	*/
	@ConfigItem(name = "exclude-paths", defaultValue = "/q")
	Optional<List<String>> excludePaths;
}