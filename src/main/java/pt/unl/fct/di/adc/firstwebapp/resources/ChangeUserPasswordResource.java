package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/changeuserpwd")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangeUserPasswordResource {

    private static final String ACCOUNT_KIND = "Account";
    private static final String TOKEN_KIND = "Token";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public static class Request {
        public Input input;
        public AuthToken token;

        public static class Input {
            public String username;
            public String oldPassword;
            public String newPassword;
            public String confirmation;
        }
    }

    public static class ResponseData {
        public String username;

        public ResponseData(String username) {
            this.username = username;
        }
    }

    @POST
    public Response changePassword(Request req) {

        // validate input
        if (req == null || req.input == null || req.token == null ||
            req.input.username == null ||
            req.input.oldPassword == null ||
            req.input.newPassword == null ||
            req.input.confirmation == null) {

            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Missing fields."
            );
        }

        String username = req.input.username.trim().toLowerCase();

        // self-change only
        if (!username.equals(req.token.username)) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "FORBIDDEN",
                    "You can only change your own password."
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

        // verify old password
        String storedHash = user.getString("password");
        String givenOldHash = SecurityUtil.hashPassword(req.input.oldPassword);

        if (!storedHash.equals(givenOldHash)) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "INVALID_CREDENTIALS",
                    "Old password is incorrect."
            );
        }

        // validate new password
        if (!req.input.newPassword.equals(req.input.confirmation)) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "New password and confirmation do not match."
            );
        }

        // update password
        String newHash = SecurityUtil.hashPassword(req.input.newPassword);

        Entity updatedUser = Entity.newBuilder(user)
                .set("password", newHash)
                .build();

        datastore.put(updatedUser);

        // invalidate all tokens
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(TOKEN_KIND)
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();

        QueryResults<Entity> tokens = datastore.run(query);

        while (tokens.hasNext()) {
            datastore.delete(tokens.next().getKey());
        }

        return ResponseUtil.success(
                new ResponseData(username)
        );
    }
}