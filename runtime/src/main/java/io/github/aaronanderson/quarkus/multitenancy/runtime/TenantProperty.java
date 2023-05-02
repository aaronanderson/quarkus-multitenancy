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
public @interface TenantProperty {
	String UNCONFIGURED_VALUE = "io.github.aaronanderson.quarkus.multitenancy.unconfigureddvalue";

	@Nonbinding
	String name() default "";

	@Nonbinding
	String defaultValue() default UNCONFIGURED_VALUE;

	class Literal extends AnnotationLiteral<TenantProperty> implements TenantProperty {

		private String name;
		private String defaultValue;

		public Literal(String name) {
			this(name, UNCONFIGURED_VALUE);
		}

		public Literal(String name, String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String defaultValue() {
			// TODO Auto-generated method stub
			return defaultValue;
		}

	}
}