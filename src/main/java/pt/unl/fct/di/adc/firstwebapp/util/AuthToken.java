package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

    public String tokenId;
    public String username;
    public String role;
    public long issuedAt;
    public long expiresAt;

    // 15 minutes in milliseconds
    private static final long TOKEN_DURATION = 1000L * 60 * 15;

    public AuthToken() {}

    public AuthToken(String username, String role) {
        this.tokenId = UUID.randomUUID().toString();
        this.username = username;
        this.role = role;

        this.issuedAt = System.currentTimeMillis();
        this.expiresAt = this.issuedAt + TOKEN_DURATION;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.expiresAt;
    }
}