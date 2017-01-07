package org.rm3l.router_companion.common.utils;

//import com.pusher.client.Pusher;
//import com.pusher.client.channel.Channel;
//import com.pusher.client.channel.SubscriptionEventListener;


/**
 * Created by rm3l on 11/08/16.
 */
public final class PushUtils {

    private PushUtils() {}

//    public static Pusher getPusher(@NonNull final String channelName,
//                                   @Nullable final Map<String, SubscriptionEventListener> eventListenerMap,
//                                   final boolean connect) {
//
//        final Pusher pusher = new Pusher(Constants.PUSHER_APP_KEY);
//
//        final Channel channel = pusher.subscribe(channelName);
//        if (eventListenerMap != null) {
//            for (Map.Entry<String, SubscriptionEventListener> eventListenerEntry : eventListenerMap.entrySet()) {
//                final String event = eventListenerEntry.getKey();
//                final SubscriptionEventListener eventListener = eventListenerEntry.getValue();
//                if (event == null || event.isEmpty() || eventListener == null) {
//                    continue;
//                }
//                channel.bind(event, eventListener);
//            }
//        }
//        if (connect) {
//            pusher.connect();
//        }
//        return pusher;
//    }
}
