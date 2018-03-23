/*
 *  Copyright (C) 2018 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/

package org.omnirom.omnigears.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import org.omnirom.omnigears.preference.AppSelectListPreference;

import java.util.List;
import java.util.ArrayList;

public class EventServiceSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, Indexable {
    private static final String TAG = "EventServiceSettings";
    public static final String EVENTS_PREFERENCES_NAME = "event_service";

    public static final String EVENT_A2DP_CONNECT = "bt_a2dp_connect_app";
    public static final String EVENT_WIRED_HEADSET_CONNECT = "headset_connect_app";
    public static final String EVENT_SERVICE_ENABLED = "event_service_enabled";
    public static final String EVENT_MEDIA_PLAYER_START = "media_player_autostart";

    private AppSelectListPreference mA2DPappSelect;
    private AppSelectListPreference mWiredHeadsetAppSelect;
    private SwitchPreference mEnable;
    private SwitchPreference mAutoStart;
    private Handler mHandler = new Handler();

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    private SharedPreferences getPrefs() {
        return getActivity().getSharedPreferences(EVENTS_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.event_service_settings);

        mEnable = (SwitchPreference) findPreference(EVENT_SERVICE_ENABLED);
        mEnable.setChecked(getPrefs().getBoolean(EventServiceSettings.EVENT_SERVICE_ENABLED, false));
        mEnable.setOnPreferenceChangeListener(this);
        mEnable.setSummary(isServiceRunning() ? getResources().getString(R.string.event_service_running)
                : getResources().getString(R.string.event_service_stopped));

        mAutoStart = (SwitchPreference) findPreference(EVENT_MEDIA_PLAYER_START);
        mAutoStart.setChecked(getPrefs().getBoolean(EventServiceSettings.EVENT_MEDIA_PLAYER_START, false));
        mAutoStart.setOnPreferenceChangeListener(this);

        mA2DPappSelect = (AppSelectListPreference) findPreference(EVENT_A2DP_CONNECT);
        mEnable.setChecked(getPrefs().getBoolean(EventServiceSettings.EVENT_SERVICE_ENABLED, false));
        String value = getPrefs().getString(EVENT_A2DP_CONNECT, null);
        mA2DPappSelect.setValue(value);
        mA2DPappSelect.setOnPreferenceChangeListener(this);

        mWiredHeadsetAppSelect = (AppSelectListPreference) findPreference(EVENT_WIRED_HEADSET_CONNECT);
        value = getPrefs().getString(EVENT_WIRED_HEADSET_CONNECT, null);
        mWiredHeadsetAppSelect.setValue(value);
        mWiredHeadsetAppSelect.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mA2DPappSelect) {
            String value = (String) newValue;
            boolean appDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            getPrefs().edit().putString(EVENT_A2DP_CONNECT, appDisabled ? null : value).commit();
            return true;
        } else if (preference == mWiredHeadsetAppSelect) {
            String value = (String) newValue;
            boolean appDisabled = value.equals(AppSelectListPreference.DISABLED_ENTRY);
            getPrefs().edit().putString(EVENT_WIRED_HEADSET_CONNECT, appDisabled ? null : value).commit();
            return true;
        } else if (preference == mEnable) {
            boolean value = ((Boolean) newValue).booleanValue();
            if (value) {
                getActivity().startService(new Intent(getActivity(), EventService.class));
            } else {
                getActivity().stopService(new Intent(getActivity(), EventService.class));
            }
            getPrefs().edit().putBoolean(EVENT_SERVICE_ENABLED, value).commit();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEnable.setSummary(isServiceRunning() ? getResources().getString(R.string.event_service_running)
                            : getResources().getString(R.string.event_service_stopped));
                }
            }, 1000);
            return true;
        } else if (preference == mAutoStart) {
            boolean value = ((Boolean) newValue).booleanValue();
            getPrefs().edit().putBoolean(EVENT_MEDIA_PLAYER_START, value).commit();
            return true;
        }
        return false;
    }

    private boolean isServiceRunning() {
        return EventService.isRunning();
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.event_service_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
    };
}
