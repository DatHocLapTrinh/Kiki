package com.kiki.backend.security;

import java.security.Principal;

public record UserPrincipal(
    String uid,
    String email,
    String displayName
) implements Principal {

    @Override
    public String getName() {
        return uid;
    }
}
