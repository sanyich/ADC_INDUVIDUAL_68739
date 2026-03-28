package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShowUserRoleResource {

    private static final String ACCOUNT_KIND = "Account";

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
        public String role;

        public ResponseData(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }

    @POST
    public Response showUserRole(Request req) {

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

        // check permissions
        String role = req.token.role;
        if (!"ADMIN".equals(role) && !"BOFFICER".equals(role)) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "FORBIDDEN",
                    "Not allowed."
            );
        }

        String username = req.input.username.trim().toLowerCase();

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

        return ResponseUtil.success(
                new ResponseData(username, user.getString("role"))
        );
    }
}