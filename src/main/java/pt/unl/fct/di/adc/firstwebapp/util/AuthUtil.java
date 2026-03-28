package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class AuthUtil {
    public static class TokenCheckResult {
        public Entity tokenEntity;
        public String error;
        public TokenCheckResult(Entity tokenEntity, String error) {
            this.tokenEntity = tokenEntity;
            this.error = error;
        }
    }

    public static TokenCheckResult validateToken(Datastore datastore, AuthToken token) {
        if (token == null || token.tokenId == null || token.username == null || token.role == null) {
            return new TokenCheckResult(null, "INVALID_TOKEN");
        }

        Key key = datastore.newKeyFactory().setKind("Token").newKey(token.tokenId);
        Entity stored = datastore.get(key);

        if (stored == null) {
            return new TokenCheckResult(null, "INVALID_TOKEN");
        }

        if (!stored.getString("username").equals(token.username) ||
            !stored.getString("role").equals(token.role)) {
            return new TokenCheckResult(null, "INVALID_TOKEN");
        }

        long expiresAt = stored.getLong("expiresAt");
        long now = System.currentTimeMillis() / 1000L;
        if (now > expiresAt) {
            return new TokenCheckResult(null, "TOKEN_EXPIRED");
        }

        return new TokenCheckResult(stored, null);
    }
}