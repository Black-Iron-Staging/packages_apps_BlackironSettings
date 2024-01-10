package com.blackiron.settings.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;

import java.util.Arrays;
import java.util.HashSet;

import com.android.settings.SettingsPreferenceFragment;

public class MiscSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private Preference mUserSwitcher;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.blackiron_settings_misc);
        PreferenceScreen prefSet = getPreferenceScreen();
        mUserSwitcher = findPreference("persist.sys.flags.enableBouncerUserSwitcher");
        mUserSwitcher.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	Context mContext = getActivity().getApplicationContext();
	ContentResolver resolver = mContext.getContentResolver();
        if (preference == mUserSwitcher) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                Settings.Secure.PREF_KG_USER_SWITCHER, value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BLKI_SETTINGS;
    }
}
