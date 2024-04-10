package com.sithum.mediatracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Start your service here
            Intent serviceIntent = new Intent(context, MainService.class);
            context.startService(serviceIntent);
        }
    }
}
