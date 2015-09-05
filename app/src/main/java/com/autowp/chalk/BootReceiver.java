package com.autowp.chalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by Dmitry on 25.07.2015.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences mSettings = getDefaultSharedPreferences(context);
        if (mSettings.getBoolean(ChalkService.PREFERENCES_AUTOSTART, ChalkService.PREFERENCES_AUTOSTART_DEFAULT)) {
            Intent serviceIntent = new Intent(context, ChalkService.class);
            context.startService(serviceIntent);
        }
    }
}
