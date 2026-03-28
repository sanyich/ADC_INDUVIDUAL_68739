package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.FirestoreUtil;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.SecurityUtil;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final String USER_KIND = "Account";    
	private static final String TOKEN_KIND = "Token";

    private final Gson g = new Gson();
    private final Datastore datastore = FirestoreUtil.getDatastore();

    public LoginResource() {}

    public static class LoginRequest {
        public LoginData input;
    }

	static class LoginResponseData {
        AuthToken token;

        LoginResponseData(AuthToken token) {
            this.token = token;
        }
    }

    public static class SuccessData {
        String status;
        Object data;

        SuccessData(String status, Object data) {
            this.status = status;
            this.data = data;
        }
    }

    public static class ErrorData {
        String status;
        String data;

        ErrorData(String status, String data) {
            this.status = status;
            this.data = data;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginRequest req) {
        if (req == null || req.input == null ||
            req.input.username == null || req.input.password == null ||
            req.input.username.isBlank() || req.input.password.isBlank()) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(g.toJson(new ErrorData("INVALID_INPUT", "Missing username or password.")))
                    .build();
        }

        String username = req.input.username.trim().toLowerCase();

        Key userKey = datastore.newKeyFactory().setKind(USER_KIND).newKey(username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorData("INVALID_CREDENTIALS", "User not found or wrong password.")))
                    .build();
        }

        String storedPwdHash = user.getString("password");
        String givenPwdHash = SecurityUtil.hashPassword(req.input.password);

        if (!storedPwdHash.equals(givenPwdHash)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson(new ErrorData("INVALID_CREDENTIALS", "User not found or wrong password.")))
                    .build();
        }

        String role = user.getString("role");

		AuthToken token = new AuthToken(username, role);
        Key tokenKey = datastore.newKeyFactory().setKind(TOKEN_KIND).newKey(token.tokenId);
        Entity tokenEntity = Entity.newBuilder(tokenKey)
                .set("username", token.username)
                .set("role", token.role)
                .set("issuedAt", token.issuedAt)
                .set("expiresAt", token.expiresAt)
                .build();

        datastore.put(tokenEntity);

        return Response.ok(g.toJson(new SuccessData("success", new LoginResponseData(token)))).build();
    }
}