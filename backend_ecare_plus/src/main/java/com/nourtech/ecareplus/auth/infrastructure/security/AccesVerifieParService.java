package com.nourtech.ecareplus.auth.infrastructure.security;

import java.lang.annotation.*;

/** Explicit opt-in for controllers whose application service verifies every resource. */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface AccesVerifieParService {}
