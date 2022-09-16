# Quarkus Multitenancy

## Overview

A [Quarkus](https://quarkus.io/) extension that provides extensible [multitenancy](https://en.wikipedia.org/wiki/Multitenancy) support.  Quarkus already has some multitenancy support via the [OIDC extension](https://quarkus.io/guides/security-openid-connect-multitenancy). This extension provides high level multitenancy support that other Quarkus extensions or web-based applications can utilize to tailor individual application tenant security and settings.

## Features
The Quarkus Multitenancy extension provides the following features:

* Tenant Resolution - Identification based on subdomain, i.e. (`tenant1.acme.com`) or base path, i.e. (`acme.com/tenant1`).
* Tenant Routing - Optional automatic Vert.x internal rerouting of requests from tenant paths to root paths, ie. `/tenant1/` -> `/` or `/tenant1/graphql` -> `/graphql`
* Tenant Configuration Loading - lazy load application specific tenant settings that are cached and then made available on every request as Vert.x RoutingContext data.
* Custom CDI Tenant Scope - Inject tenant configuration properties per request
* Quarkus OIDC [TenantConfigResolver](https://quarkus.io/guides/security-openid-connect-multitenancy) Compatability - Support per tenant OIDC configurations like in this [example](example/src/main/java/io/github/aaronanderson/multitenancy/example/TenantConfigResolverImpl.java)


## Installation

1. Add the extension to the Quarkus web application's Maven pom.xml
    ```
     <dependency>
	 	<groupId>io.github.aaronanderson</groupId>
  		<artifactId>quarkus-multitenancy</artifactId>
    	<version>1.0.0-SNAPSHOT</version>
     </dependency>
    ```

1. Create an [Tenant Loader](runtime/src/main/java/io/github/aaronanderson/quarkus/multitenancy/runtime/TenantLoader.java) implementation that will retrieve individual tenant configuration details based on the resolved tenant ID.

1. Review and run the [example appliation](example) for insight on how to setup tenant routing and utilize the @TenantScope CDI scope. Login with any user ID/password combination as authentication is not enforced. 

## Further Research
The tenant CDI scope context is being set on a Quarkus Vert.x  based filter. The filter next() invocation is non-blocking so it is not possible to determine when the end of the request is finished in order to terminate the scope context. A Vert.x EndHandler is being used to terminate the context but if it is not invoked or invoked on the wrong thread the stale active contect will remain in a threadlocal and corrupt future requests. A more robust means of performing the tenant scope handling should be investigated.

