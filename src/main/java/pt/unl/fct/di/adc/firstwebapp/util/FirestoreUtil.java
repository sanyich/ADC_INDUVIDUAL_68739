package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

public class FirestoreUtil {

    private static Datastore datastore;

    private static final String PROJECT_ID = "individual-proj-491416";

    private FirestoreUtil() {}

    public static synchronized Datastore getDatastore() {
        if (datastore == null) {
            datastore = DatastoreOptions.newBuilder()
                    .setProjectId(PROJECT_ID)
                    .build()
                    .getService();
        }
        return datastore;
    }
}