package io.github.aaronanderson.quarkus.multitenancy.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
public @interface TenantConfig {

	public static final String CONTEXT_TENANT_ID = "io.github.aaronanderson.quarkus.multitenancy.tenant-id";
	public static final String CONTEXT_TENANT = "io.github.aaronanderson.quarkus.multitenancy.tenant";

	public static AnnotationLiteral<TenantConfig> LITERAL = new AnnotationLiteral<TenantConfig>() {
	};
}