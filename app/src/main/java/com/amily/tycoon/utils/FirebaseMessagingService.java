package com.amily.tycoon.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amily.tycoon.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingServic";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);



        try {
            /* This class help us to receive the notification in foreground i.e while we are using our app*/

            String notification_title = remoteMessage.getNotification().getTitle();
            String notification_message = remoteMessage.getNotification().getBody();

            /* To use this line of code we first added the intent filter into the manifest into the class
             * we want to redirect the user when he clicks on the notification, in our case we choose the profile activity
             * then we added the into the cloud function a click action in the building of a notification, you better check
             * it out in order to understand what i'm trying to say */



            String click_action = remoteMessage.getNotification().getClickAction();
            String userWhoSentNotification = remoteMessage.getData().get("user_who_sent_the_notification");

            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            notification.setContentTitle(notification_title);
            notification.setContentText(notification_message);
            notification.setSmallIcon(R.mipmap.ic_launcher_round);
            notification.setAutoCancel(true);

            // vibration
            notification.setVibrate(new long[] {200, 200,200, 200, 200});


            // initializing the intent and adding intent extras
            Intent resultIntent = new Intent(click_action);
            // resultIntent.putExtra("user_id", userWhoSentNotification);
            /* The above code is used pass intent iextras and we don't need to pass intent extras, so we commented it*/

            // add the result intent above to the appending intent in order to work
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                    resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setContentIntent(resultPendingIntent);


            int notificationID = (int) System.currentTimeMillis();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // if you are the one who send a sms or commenteed, don't show the notification on your phone
            if(!userWhoSentNotification.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                notificationManager.notify(notificationID, notification.build());
            }


        }catch (Exception e) {
            Log.e(TAG, "onMessageReceived: there is an EXCEPTION : " + e.getMessage());
        }


    }
}
