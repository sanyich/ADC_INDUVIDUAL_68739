package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.AuthUtil;
import pt.unl.fct.di.adc.firstwebapp.util.FirestoreUtil;
import pt.unl.fct.di.adc.firstwebapp.util.ResponseUtil;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShowUsersResource {

    private static final String ACCOUNT_KIND = "Account";

    private final Datastore datastore = FirestoreUtil.getDatastore();

    public ShowUsersResource() {}

    public static class ShowUsersRequest {
        public Object input;
        public AuthToken token;
    }

    public static class UserData {
        public String username;
        public String phone;
        public String address;
        public String role;

        public UserData(String username, String phone, String address, String role) {
            this.username = username;
            this.phone = phone;
            this.address = address;
            this.role = role;
        }
    }

    public static class ShowUsersResponseData {
        public List<UserData> users;

        public ShowUsersResponseData(List<UserData> users) {
            this.users = users;
        }
    }

    @POST
    public Response showUsers(ShowUsersRequest req) {

        if (req == null || req.token == null) {
            return ResponseUtil.badRequest("INVALID_INPUT", "Missing token.");
        }

        AuthUtil.TokenCheckResult check = AuthUtil.validateToken(datastore, req.token);
        if (check.error != null) {
            return ResponseUtil.forbidden(check.error, "Invalid or expired token.");
        }

        String role = req.token.role;
        if (!"ADMIN".equals(role) && !"BOFFICER".equals(role)) {
            return ResponseUtil.forbidden("FORBIDDEN", "Only ADMIN or BOFFICER can list users.");
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(ACCOUNT_KIND)
                .build();

        QueryResults<Entity> results = datastore.run(query);

        List<UserData> users = new ArrayList<>();

        while (results.hasNext()) {
            Entity e = results.next();
            users.add(new UserData(
                    e.getString("username"),
                    e.getString("phone"),
                    e.getString("address"),
                    e.getString("role")
            ));
        }

        return ResponseUtil.success(new ShowUsersResponseData(users));
    }
}