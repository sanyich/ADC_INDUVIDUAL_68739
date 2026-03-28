package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/modaccount")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModifyAccountResource {

    private static final String ACCOUNT_KIND = "Account";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public static class Request {
        public Input input;
        public AuthToken token;

        public static class Input {
            public String username;
            public String phone;
            public String address;
        }
    }

    public static class ResponseData {
        public String username;
        public String phone;
        public String address;

        public ResponseData(String username, String phone, String address) {
            this.username = username;
            this.phone = phone;
            this.address = address;
        }
    }

    @POST
    public Response modifyAccount(Request req) {

        // validate input
        if (req == null || req.input == null || req.token == null ||
            req.input.username == null || req.input.username.isBlank()) {

            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Missing username or token."
            );
        }

        String targetUsername = req.input.username.trim().toLowerCase();

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

        String requester = req.token.username;
        String requesterRole = req.token.role;

        // get target user
        Key key = datastore.newKeyFactory()
                .setKind(ACCOUNT_KIND)
                .newKey(targetUsername);

        Entity targetUser = datastore.get(key);

        if (targetUser == null) {
            return ResponseUtil.error(
                    Response.Status.NOT_FOUND,
                    "USER_NOT_FOUND",
                    "User does not exist."
            );
        }

        String targetRole = targetUser.getString("role");

        // authorization check

        boolean allowed = false;

        if ("ADMIN".equals(requesterRole)) {
            allowed = true;
        } else if ("BOFFICER".equals(requesterRole)) {
            // can modify themselves + users
            if (requester.equals(targetUsername) ||
                "USER".equals(targetRole)) {
                allowed = true;
            }
        } else if ("USER".equals(requesterRole)) {
            // only themselves
            if (requester.equals(targetUsername)) {
                allowed = true;
            }
        }

        if (!allowed) {
            return ResponseUtil.error(
                    Response.Status.FORBIDDEN,
                    "FORBIDDEN",
                    "Not allowed to modify this account."
            );
        }

        // update fields
        Entity.Builder builder = Entity.newBuilder(targetUser);

        if (req.input.phone != null && !req.input.phone.isBlank()) {
            builder.set("phone", req.input.phone.trim());
        }

        if (req.input.address != null && !req.input.address.isBlank()) {
            builder.set("address", req.input.address.trim());
        }

        Entity updated = builder.build();
        datastore.put(updated);

        return ResponseUtil.success(
                new ResponseData(
                        targetUsername,
                        updated.getString("phone"),
                        updated.getString("address")
                )
        );
    }
}