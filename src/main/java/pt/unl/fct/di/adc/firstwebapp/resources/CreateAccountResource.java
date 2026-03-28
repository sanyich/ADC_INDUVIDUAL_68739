package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.Set;
import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.FirestoreUtil;
import pt.unl.fct.di.adc.firstwebapp.util.ResponseUtil;
import pt.unl.fct.di.adc.firstwebapp.util.SecurityUtil;

@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateAccountResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private static final String ACCOUNT_KIND = "Account";

    private static final Set<String> VALID_ROLES =
            Set.of("USER", "BOFFICER", "ADMIN");

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public CreateAccountResource() {}

    public static class CreateAccountRequest {
        public Input input;

        public static class Input {
            public String username;
            public String password;
            public String confirmation;
            public String phone;
            public String address;
            public String role;
        }
    }

    public static class CreateAccountResponseData {
        public String username;
        public String role;

        public CreateAccountResponseData(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }

    @POST
    public Response createAccount(CreateAccountRequest req) {

        if (req == null || req.input == null) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Missing input object."
            );
        }

        CreateAccountRequest.Input in = req.input;

        if (isBlank(in.username) || isBlank(in.password) || isBlank(in.confirmation)
                || isBlank(in.phone) || isBlank(in.address) || isBlank(in.role)) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "All fields are required."
            );
        }

        String username = in.username.trim().toLowerCase();
        String roleUpper = in.role.trim().toUpperCase();

        if (!username.contains("@")) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Username must be in email format."
            );
        }

        if (!in.password.equals(in.confirmation)) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Password and confirmation do not match."
            );
        }

        if (!VALID_ROLES.contains(roleUpper)) {
            return ResponseUtil.error(
                    Response.Status.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Role must be USER, BOFFICER, or ADMIN."
            );
        }

        Key userKey = datastore.newKeyFactory()
                .setKind(ACCOUNT_KIND)
                .newKey(username);

        Entity existing = datastore.get(userKey);
        if (existing != null) {
            return ResponseUtil.error(
                    Response.Status.CONFLICT,
                    "USER_ALREADY_EXISTS",
                    "An account with that username already exists."
            );
        }

        String hashedPassword = SecurityUtil.hashPassword(in.password);

        Entity account = Entity.newBuilder(userKey)
                .set("username", username)
                .set("password", hashedPassword)
                .set("phone", in.phone.trim())
                .set("address", in.address.trim())
                .set("role", roleUpper)
                .build();

        datastore.put(account);

        LOG.info("Account created: " + username + " with role " + roleUpper);

        return ResponseUtil.success(new CreateAccountResponseData(username, roleUpper));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}