package com.bookworm.security;

import java.lang.annotation.*;

/** Endpoint requires a logged-in user (Bearer token). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {}
