package com.salonbook.app.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.salonbook.app.R;
import com.salonbook.app.utils.Constants;

public class AppointmentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Constants.ACTION_APPOINTMENT_REMINDER.equals(action)) {
            String serviceName = intent.getStringExtra("serviceName");
            String stylistName = intent.getStringExtra("stylistName");
            showNotification(context, serviceName, stylistName);
        } else if (Constants.ACTION_APPOINTMENT_STATUS_CHANGED.equals(action)) {
            String status = intent.getStringExtra("status");
            showStatusChangeNotification(context, status);
        }
    }

    private void showNotification(Context context, String serviceName, String stylistName) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            notificationManager.createNotificationChannel(channel);
        }

        String body = context.getString(R.string.notification_body,
                serviceName != null ? serviceName : "",
                stylistName != null ? stylistName : "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_appointments)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(Constants.NOTIFICATION_ID, builder.build());
    }

    private void showStatusChangeNotification(Context context, String status) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_appointments)
                .setContentTitle(context.getString(R.string.appointment_status_changed))
                .setContentText("Status: " + (status != null ? status : ""))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(Constants.NOTIFICATION_ID + 1, builder.build());
    }
}
