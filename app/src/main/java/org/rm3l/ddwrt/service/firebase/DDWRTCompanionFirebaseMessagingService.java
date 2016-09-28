package org.rm3l.ddwrt.service.firebase;

import com.google.firebase.messaging.FirebaseMessagingService;

/**
 * This is required if you want to do any message handling beyond receiving notifications on
 * apps in the background.
 * To receive notifications in foregrounded apps,
 * to receive data payload, to send upstream messages, and so on, you must extend this service.
 *
 * Created by rm3l on 26/09/2016.
 */
public class DDWRTCompanionFirebaseMessagingService extends FirebaseMessagingService {
}
