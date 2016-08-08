package com.dosse.bwentrain.androidPlayer;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.widget.Toast;


public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName("SINE");
        addPreferencesFromResource(R.xml.pref_general);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                this);
        try {
            PreferenceGroup p = ((PreferenceGroup) getPreferenceScreen());
            for (int i = 0; i < p.getPreferenceCount(); i++) {
                Preference pref = p.getPreference(i);
                if (pref instanceof ListPreference) {
                    ListPreference listPref = (ListPreference) pref;
                    pref.setSummary(listPref.getEntry());
                }
            }
        }catch (Throwable t){}
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equalsIgnoreCase("noise_switch")){
            Toast.makeText(getBaseContext(),getString(R.string.noise_switch_restarting),Toast.LENGTH_LONG).show();
            new Thread(){
                public void run(){
                    try{Thread.sleep(500);}catch(Throwable e){}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(0);
                            Intent mStartActivity = new Intent(SettingsActivity.this,MainActivity.class);
                            PendingIntent mPendingIntent = PendingIntent.getActivity(SettingsActivity.this, 40437, mStartActivity,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) SettingsActivity.this.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            System.exit(0);
                        }
                    });

                }
            }.start();
            finish();
        }
    }
}