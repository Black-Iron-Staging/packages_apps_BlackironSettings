/*
 * Copyright (C) 2016-2024 BlackIron Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackiron.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.blackiron.OmniJawsClient;
import com.android.internal.util.blackiron.Utils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.blackiron.udfps.UdfpsUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lineageos.providers.LineageSettings;

@SearchIndexable
public class LockScreen extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener  {

    public static final String TAG = "LockScreen";

    private static final String LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category";
    private static final String LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category";
    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String UDFPS_CATEGORY = "udfps_category";

    private static final String MAIN_WIDGET_1_KEY = "main_custom_widgets1";
    private static final String MAIN_WIDGET_2_KEY = "main_custom_widgets2";
    private static final String EXTRA_WIDGET_1_KEY = "custom_widgets1";
    private static final String EXTRA_WIDGET_2_KEY = "custom_widgets2";
    private static final String EXTRA_WIDGET_3_KEY = "custom_widgets3";
    private static final String EXTRA_WIDGET_4_KEY = "custom_widgets4";

    private PreferenceCategory mUdfpsCategory;
    private Preference mRippleEffect;
    private Preference mWeather;
    
    private Preference mMainWidget1;
    private Preference mMainWidget2;
    private Preference mExtraWidget1;
    private Preference mExtraWidget2;
    private Preference mExtraWidget3;
    private Preference mExtraWidget4;
    
    private Map<Preference, String> widgetKeysMap = new HashMap<>();

    private OmniJawsClient mWeatherClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.blackiron_settings_lockscreen);

        PreferenceScreen prefScreen = getPreferenceScreen();

        PreferenceCategory gestCategory = (PreferenceCategory) findPreference(LOCKSCREEN_GESTURES_CATEGORY);

        mMainWidget1 = findPreference(MAIN_WIDGET_1_KEY);
        mMainWidget2 = findPreference(MAIN_WIDGET_2_KEY);
        mExtraWidget1 = findPreference(EXTRA_WIDGET_1_KEY);
        mExtraWidget2 = findPreference(EXTRA_WIDGET_2_KEY);
        mExtraWidget3 = findPreference(EXTRA_WIDGET_3_KEY);
        mExtraWidget4 = findPreference(EXTRA_WIDGET_4_KEY);

        List<Preference> widgetPreferences = Arrays.asList(mMainWidget1, mMainWidget2, mExtraWidget1, mExtraWidget2, mExtraWidget3, mExtraWidget4);
        for (Preference widgetPref : widgetPreferences) {
            widgetPref.setOnPreferenceChangeListener(this);
            widgetKeysMap.put(widgetPref, "");
        }

        String mainWidgets = Settings.System.getString(getActivity().getContentResolver(), "lockscreen_widgets");
        String extraWidgets = Settings.System.getString(getActivity().getContentResolver(), "lockscreen_widgets_extras");

        setWidgetValues(mainWidgets, mMainWidget1, mMainWidget2);
        setWidgetValues(extraWidgets, mExtraWidget1, mExtraWidget2, mExtraWidget3, mExtraWidget4);

        FingerprintManager mFingerprintManager = (FingerprintManager)
                getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mRippleEffect = (Preference) findPreference(KEY_RIPPLE_EFFECT);

        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
            gestCategory.removePreference(mRippleEffect);
        }
            Resources resources = getResources();
    

        mUdfpsCategory = findPreference(UDFPS_CATEGORY);
        if (!UdfpsUtils.hasUdfpsSupport(getContext())) {
            prefScreen.removePreference(mUdfpsCategory);
        }

        mWeather = (Preference) findPreference(KEY_WEATHER);
        mWeatherClient = new OmniJawsClient(getContext());
        updateWeatherSettings();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (widgetKeysMap.containsKey(preference)) {
            widgetKeysMap.put(preference, String.valueOf(newValue));
            updateWidgetPreferences();
            return true;
        }
        return false;
    }

    private void setWidgetValues(String widgets, Preference... preferences) {
        if (widgets == null) {
            return;
        }
        List<String> widgetList = Arrays.asList(widgets.split(","));
        for (int i = 0; i < preferences.length && i < widgetList.size(); i++) {
            widgetKeysMap.put(preferences[i], widgetList.get(i).trim());
        }
    }

    private void updateWidgetPreferences() {
        List<String> mainWidgetsList = Arrays.asList(widgetKeysMap.get(mMainWidget1), widgetKeysMap.get(mMainWidget2));
        List<String> extraWidgetsList = Arrays.asList(widgetKeysMap.get(mExtraWidget1), widgetKeysMap.get(mExtraWidget2), widgetKeysMap.get(mExtraWidget3), widgetKeysMap.get(mExtraWidget4));

        mainWidgetsList = filterEmptyStrings(mainWidgetsList);
        extraWidgetsList = filterEmptyStrings(extraWidgetsList);

        String mainWidgets = TextUtils.join(",", mainWidgetsList);
        String extraWidgets = TextUtils.join(",", extraWidgetsList);

        Settings.System.putString(getActivity().getContentResolver(), "lockscreen_widgets", mainWidgets);
        Settings.System.putString(getActivity().getContentResolver(), "lockscreen_widgets_extras", extraWidgets);
    }

    private List<String> filterEmptyStrings(List<String> inputList) {
        return inputList.stream().filter(s -> !TextUtils.isEmpty(s)).collect(Collectors.toList());
    }

    private void updateWeatherSettings() {
        if (mWeatherClient == null || mWeather == null) return;

        boolean weatherEnabled = mWeatherClient.isOmniJawsEnabled();
        mWeather.setEnabled(weatherEnabled);
        mWeather.setSummary(weatherEnabled ? R.string.lockscreen_weather_summary :
            R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeatherSettings();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BLKI_SETTINGS;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blackiron_settings_lockscreen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    FingerprintManager mFingerprintManager = (FingerprintManager)
                            context.getSystemService(Context.FINGERPRINT_SERVICE);
                    if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
                        keys.add(KEY_RIPPLE_EFFECT);
                    }
                        Resources resources = context.getResources();
                    return keys;
                }
            };
}
