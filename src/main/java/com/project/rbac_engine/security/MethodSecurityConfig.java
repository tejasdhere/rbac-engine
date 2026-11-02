package com.project.rbac_engine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level security so that @PreAuthorize annotations are processed.
 *
 * The bean method here is STATIC for an important reason:
 * Spring Security's AOP infrastructure initialises MethodSecurityExpressionHandler
 * very early in the context lifecycle — before regular @Configuration beans are fully
 * ready. A static @Bean method lets Spring call it without first instantiating this
 * class, which avoids a circular-dependency failure between the security post-processors
 * and the beans they need to wrap.
 */
@Configuration
@EnableMethodSecurity   // activates @PreAuthorize / @PostAuthorize; replaces old @EnableGlobalMethodSecurity
public class MethodSecurityConfig {

    /**
     * Registers DefaultMethodSecurityExpressionHandler and plugs in our CustomPermissionEvaluator
     * so that hasPermission(...) expressions in @PreAuthorize are routed to it.
     *
     * Spring injects CustomPermissionEvaluator as a method parameter at startup.
     */
    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator customPermissionEvaluator) {

        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        // Wire in our evaluator — this is what connects @PreAuthorize("hasPermission(...)")
        // to the DB-driven logic in CustomPermissionEvaluator.
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}
