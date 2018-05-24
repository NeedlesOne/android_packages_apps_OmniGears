/*
 *  Copyright (C) 2015-2018 The OmniROM Project
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
package org.omnirom.omnigears.interfacesettings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.ArrayList;

import org.omnirom.omnilib.preference.ColorSelectPreference;
import org.omnirom.omnilib.preference.SeekBarPreference;
import org.omnirom.omnilib.preference.SystemCheckBoxPreference;

public class StatusbarBatterySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "StatusbarBatterySettings";

    private static final String STATUSBAR_BATTERY_STYLE = "statusbar_battery_style";
    private static final String STATUSBAR_BATTERY_PERCENT = "statusbar_battery_percent_enable";
    private static final String STATUSBAR_CHARGING_COLOR = "statusbar_battery_charging_color";
    private static final String STATUSBAR_BATTERY_PERCENT_INSIDE = "statusbar_battery_percent_inside";
    private static final String STATUSBAR_BATTERY_SHOW_BOLT = "statusbar_battery_charging_image";

    private ListPreference mBatteryStyle;
    private ListPreference mBatteryPercent;
    private ColorSelectPreference mChargingColor;
    private Preference mPercentInside;
    private Preference mShowBolt;
    private int mBatteryStyleValue;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.statusbar_battery_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mBatteryStyle = (ListPreference) findPreference(STATUSBAR_BATTERY_STYLE);
        mBatteryStyleValue = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_STYLE, 0);

        mBatteryStyle.setValue(Integer.toString(mBatteryStyleValue));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mChargingColor = (ColorSelectPreference) prefScreen.findPreference(STATUSBAR_CHARGING_COLOR);
        int chargingColor = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_CHARGING_COLOR, 0xFFFFFFFF);
        mChargingColor.setColor(chargingColor);
        String hexColor = String.format("#%08X", chargingColor);
        mChargingColor.setSummary(hexColor);
        mChargingColor.setOnPreferenceChangeListener(this);

        mPercentInside = findPreference(STATUSBAR_BATTERY_PERCENT_INSIDE);

        mBatteryPercent = (ListPreference) findPreference(STATUSBAR_BATTERY_PERCENT);
        final int systemShowPercent = Settings.System.getInt(resolver,
                Settings.System.SHOW_BATTERY_PERCENT, 0);
        int showPercent = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_PERCENT, systemShowPercent);
        int forceShowPercent = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_FORCE_PERCENT, 0);
        int batteryPercentValue = 0;
        if (showPercent == 1) {
            batteryPercentValue = 1;
        } else if (forceShowPercent == 1) {
            batteryPercentValue = 2;
        }
        mBatteryPercent.setValue(Integer.toString(batteryPercentValue));
        mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        mBatteryPercent.setOnPreferenceChangeListener(this);

        mShowBolt = findPreference(STATUSBAR_BATTERY_SHOW_BOLT);

        //updateEnablement();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBatteryStyle) {
            mBatteryStyleValue = Integer.valueOf((String) newValue);
            int index = mBatteryStyle.findIndexOfValue((String) newValue);
            mBatteryStyle.setSummary(
                    mBatteryStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_STYLE, mBatteryStyleValue);
        } else if (preference == mChargingColor) {
            String hexColor = String.format("#%08X", mChargingColor.getColor());
            mChargingColor.setSummary(hexColor);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_CHARGING_COLOR, mChargingColor.getColor());
        } else if (preference == mBatteryPercent) {
            int batteryPercentValue = Integer.valueOf((String) newValue);
            if (batteryPercentValue == 1) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_PERCENT, 1);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_FORCE_PERCENT, 0);
            } else if (batteryPercentValue == 2) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_PERCENT, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_FORCE_PERCENT, 1);
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_PERCENT, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_FORCE_PERCENT, 0);
            }
            mBatteryPercent.setValue(Integer.toString(batteryPercentValue));
            mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        }
        //updateEnablement();
        return true;
    }

    /*private void updateEnablement() {
        mPercentInside.setEnabled(mBatteryStyleValue != 3 && mBatteryStyleValue != 4);
        mShowBolt.setEnabled(mBatteryStyleValue != 3 && mBatteryStyleValue != 4);
        mBatteryPercent.setEnabled(mBatteryStyleValue != 3);
    }*/

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.statusbar_battery_settings;
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
