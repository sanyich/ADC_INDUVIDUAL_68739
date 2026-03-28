package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShowAuthSessionsResource {

    private static final String TOKEN_KIND = "Token";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public static class Request {
        public Object input;
        public AuthToken token;
    }

    public static class SessionData {
        public String tokenId;
        public String username;
        public String role;
        public long issuedAt;
        public long expiresAt;

        public SessionData(String tokenId, String username, String role,
                           long issuedAt, long expiresAt) {
            this.tokenId = tokenId;
            this.username = username;
            this.role = role;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }

    public static class ResponseData {
        public List<SessionData> sessions;

        public ResponseData(List<SessionData> sessions) {
            this.sessions = sessions;
        }
    }

    @POST
    public Response showSessions(Request req) {

        // validate input
        if (req == null || req.token == null) {
            return ResponseUtil.error(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Missing token.");
        }

        // validate token
        AuthUtil.TokenCheckResult check =
                AuthUtil.validateToken(datastore, req.token);

        if (check.error != null) {
            return ResponseUtil.error(Response.Status.FORBIDDEN,
                    check.error, "Invalid or expired token.");
        }

        // check ADMIN
        if (!"ADMIN".equals(req.token.role)) {
            return ResponseUtil.error(Response.Status.FORBIDDEN,
                    "FORBIDDEN", "Only ADMIN can see sessions.");
        }

        // query tokens
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(TOKEN_KIND)
                .build();

        QueryResults<Entity> results = datastore.run(query);

        List<SessionData> sessions = new ArrayList<>();

        while (results.hasNext()) {
            Entity e = results.next();

            sessions.add(new SessionData(
                    e.getKey().getName(),
                    e.getString("username"),
                    e.getString("role"),
                    e.getLong("issuedAt"),
                    e.getLong("expiresAt")
            ));
        }

        return ResponseUtil.success(new ResponseData(sessions));
    }
}