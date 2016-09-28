package org.rm3l.ddwrt.service.firebase;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * handle the creation, rotation, and updating of registration tokens.
 * This is required for sending to specific devices or for creating device groups.
 *
 * Created by rm3l on 26/09/2016.
 */
public class DDWRTCompanionFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = DDWRTCompanionFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        //TODO After you've obtained the token, you can send it to your app server and store it using your preferred method.
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
//        sendRegistrationToServer(refreshedToken);
    }

}
