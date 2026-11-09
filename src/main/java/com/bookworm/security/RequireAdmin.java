package com.bookworm.security;

import java.lang.annotation.*;

/** Endpoint requires a logged-in user with the ADMIN role. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {}
