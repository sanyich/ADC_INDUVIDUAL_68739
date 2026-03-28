package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/changeuserrole")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangeUserRoleResource {

    private static final String ACCOUNT_KIND = "Account";
    private static final String TOKEN_KIND = "Token";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public static class Request {
        public Input input;
        public AuthToken token;

        public static class Input {
            public String username;
            public String role;
        }
    }

    public static class ResponseData {
        public String username;
        public String newRole;

        public ResponseData(String username, String newRole) {
            this.username = username;
            this.newRole = newRole;
        }
    }

    @POST
    public Response changeUserRole(Request req) {

        // validate input
        if (req == null || req.input == null || req.token == null ||
            req.input.username == null || req.input.role == null) {

            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Missing username, role or token."
            );
        }

        // validate token
        AuthUtil.TokenCheckResult check =
                AuthUtil.validateToken(datastore, req.token);

        if (check.error != null) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    check.error,
                    "Invalid or expired token."
            );
        }

        // only ADMIN
        if (!"ADMIN".equals(req.token.role)) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "FORBIDDEN",
                    "Only ADMIN can change roles."
            );
        }

        String username = req.input.username.trim().toLowerCase();
        String newRole = req.input.role.trim().toUpperCase();

        // validate role
        if (!newRole.equals("USER") &&
            !newRole.equals("BOFFICER") &&
            !newRole.equals("ADMIN")) {

            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_ROLE",
                    "Role must be USER, BOFFICER or ADMIN."
            );
        }

        // get user
        Key key = datastore.newKeyFactory()
                .setKind(ACCOUNT_KIND)
                .newKey(username);

        Entity user = datastore.get(key);

        if (user == null) {
            return ResponseUtil.error(
                    Response.Status.NOT_FOUND,
                    "USER_NOT_FOUND",
                    "User does not exist."
            );
        }

        // update role
        Entity updatedUser = Entity.newBuilder(user)
                .set("role", newRole)
                .build();

        datastore.put(updatedUser);

        // invalidate all tokens of that user
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(TOKEN_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();

        QueryResults<Entity> tokens = datastore.run(query);

        while (tokens.hasNext()) {
            Entity t = tokens.next();
            datastore.delete(t.getKey());
        }

        return ResponseUtil.success(
                new ResponseData(username, newRole)
        );
    }
}