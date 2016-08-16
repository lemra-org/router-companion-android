package org.rm3l.ddwrt.tasker;

import com.amazonaws.regions.Regions;

/**
 * Created by rm3l on 06/08/16.
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Not instantiable");
    }

    public static final String TAG = "DDWRTCompanionTaskerPlugin";

    public static final String FILEPROVIDER_AUTHORITY = (BuildConfig.APPLICATION_ID + ".fileprovider");

    public static final int DOORBELL_APPID = 0;
    public static final String DOORBELL_APIKEY = \"fake-api-key\";


    public static final String URL_SHORTENER_API_BASE_URL =
            "https://www.googleapis.com/urlshortener/v1/";

    public static final String GOOGLE_API_KEY = \"fake-api-key\";

    public static final String AWS_COGNITO_IDENTITY_POOL_ID =
            "us-east-1:2d76d7a3-17f4-4c78-a5c4-ed3a548fe45b";
    public static final Regions AWS_COGNITO_IDENTITY_POOL_REGION = Regions.US_EAST_1;
    public static final String AWS_S3_BUCKET_NAME = "dd-wrt-companion";
    public static final String AWS_S3_FEEDBACKS_FOLDER_NAME = "feedbacks/tasker";
    public static final String AWS_S3_LOGS_FOLDER_NAME = "_logs";

    public static final String AWS_S3_FEEDBACK_PENDING_TRANSFER_PREF = "feedbacks_pending_transfer";

    public static final String Q_A_WEBSITE = "https://www.codewake.com/p/ddwrt-companion";

    public static final String DEFAULT_SHARED_PREFERENCES_KEY = \"fake-key\";

}
