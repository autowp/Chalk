package com.autowp.chalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by Dmitry on 03.08.2015.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences settings = getDefaultSharedPreferences(getActivity().getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(this);


        Context context = getActivity().getApplicationContext();
        EditTextPreference versionPref = (EditTextPreference)findPreference("version");
        try {
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            versionPref.setTitle(getString(R.string.version) + ": " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        SharedPreferences settings = getDefaultSharedPreferences(getActivity().getApplicationContext());
        settings.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
