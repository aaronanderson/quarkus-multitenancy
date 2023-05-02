package io.github.aaronanderson.quarkus.multitenancy.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.util.AnnotationLiteral;

@Documented
@NormalScope
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface TenantScoped {

	public static AnnotationLiteral<TenantScoped> LITERAL = new AnnotationLiteral<TenantScoped>() {
	};
}
