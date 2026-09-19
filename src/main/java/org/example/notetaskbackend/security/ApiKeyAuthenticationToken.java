package org.example.notetaskbackend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final String principal;

    /** До аутентификации — credentials = apiKey, principal = null. */
    public ApiKeyAuthenticationToken(String apiKey) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.apiKey = apiKey;
        this.principal = null;
        setAuthenticated(false);
    }

    /** После аутентификации — credentials обнулены, principal = имя клиента. */
    public ApiKeyAuthenticationToken(String principal,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = null;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
