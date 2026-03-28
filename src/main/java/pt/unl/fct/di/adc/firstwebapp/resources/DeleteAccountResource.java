package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeleteAccountResource {

    private static final String ACCOUNT_KIND = "Account";
    private static final String TOKEN_KIND = "Token";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public static class Request {
        public Input input;
        public AuthToken token;

        public static class Input {
            public String username;
        }
    }

    public static class ResponseData {
        public String username;

        public ResponseData(String username) {
            this.username = username;
        }
    }

    @POST
    public Response deleteAccount(Request req) {

        // validate input
        if (req == null || req.input == null || req.token == null ||
            req.input.username == null || req.input.username.isBlank()) {

            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Missing username or token."
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

        // validate role
        if (!"ADMIN".equals(req.token.role)) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "FORBIDDEN",
                    "Only ADMIN can delete accounts."
            );
        }

        String username = req.input.username.trim().toLowerCase();

        // validate user exists
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

        // delete user
        datastore.delete(key);

        // delete all tokens of that user
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
                new ResponseData(username)
        );
    }
}