/*
 *  Copyright (C) 2017 The OmniROM Project
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
package org.omnirom.omnigears;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.util.omni.OmniSwitchConstants;
import com.android.internal.util.omni.PackageUtils;
import com.android.internal.util.omni.DeviceUtils;

public class ButtonSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, Indexable {

    private static final String CATEGORY_KEYS = "button_keys";
    private static final String KEYS_SHOW_NAVBAR_KEY = "navigation_bar_show";
    private static final String KEYS_DISABLE_HW_KEY = "hardware_keys_disable";
    private static final String NAVIGATION_BAR_RECENTS_STYLE = "navbar_recents_style";
    private static final String LONG_PRESS_RECENTS_ACTION = "long_press_recents_action";
    private static final String LONG_PRESS_HOME_ACTION = "long_press_home_action";
    private static final String DOUBLE_PRESS_HOME_ACTION = "double_press_home_action";
    private static final String BUTTON_BACK_KILL_TIMEOUT = "button_back_kill_timeout";
    private static final String KEY_BUTTON_LIGHT = "button_brightness";
    private static final String FINGERPRINT_VIB = "fingerprint_success_vib";

    private ListPreference mNavbarRecentsStyle;
    private ListPreference mLongPressRecentsAction;
    private ListPreference mLongPressHomeAction;
    private ListPreference mDoublePressHomeAction;
    private SwitchPreference mEnableNavBar;
    private SwitchPreference mDisabkeHWKeys;
    private ListPreference mBackKillTimeout;
    private Preference mButtonLight;
    private FingerprintManager mFingerprintManager;
    private SwitchPreference mFingerprintVib;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final ContentResolver resolver = getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final PreferenceCategory keysCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_KEYS);

        mEnableNavBar = (SwitchPreference) prefScreen.findPreference(KEYS_SHOW_NAVBAR_KEY);
        mDisabkeHWKeys = (SwitchPreference) prefScreen.findPreference(KEYS_DISABLE_HW_KEY);
        mButtonLight = prefScreen.findPreference(KEY_BUTTON_LIGHT);

        if (deviceKeys == 0) {
            keysCategory.removePreference(mDisabkeHWKeys);
            keysCategory.removePreference(mButtonLight);
        } else {
            boolean hardwareKeysDisable = Settings.System.getInt(resolver,
                    Settings.System.HARDWARE_KEYS_DISABLE, 0) == 1;
            mDisabkeHWKeys.setChecked(hardwareKeysDisable);
        }

        boolean showNavBarDefault = DeviceUtils.deviceSupportNavigationBar(getActivity());
        boolean showNavBar = Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_BAR_SHOW, showNavBarDefault ? 1 : 0) == 1;
        mEnableNavBar.setChecked(showNavBar);

        mNavbarRecentsStyle = (ListPreference) findPreference(NAVIGATION_BAR_RECENTS_STYLE);
        int recentsStyle = Settings.System.getInt(resolver,
                Settings.System.NAVIGATION_BAR_RECENTS, 0);

        mNavbarRecentsStyle.setValue(Integer.toString(recentsStyle));
        mNavbarRecentsStyle.setSummary(mNavbarRecentsStyle.getEntry());
        mNavbarRecentsStyle.setOnPreferenceChangeListener(this);

        mLongPressRecentsAction = (ListPreference) findPreference(LONG_PRESS_RECENTS_ACTION);
        int longPressRecentsAction = Settings.System.getInt(resolver,
                Settings.System.BUTTON_LONG_PRESS_RECENTS, 0);

        mLongPressRecentsAction.setValue(Integer.toString(longPressRecentsAction));
        mLongPressRecentsAction.setSummary(mLongPressRecentsAction.getEntry());
        mLongPressRecentsAction.setOnPreferenceChangeListener(this);

        // for navbar devices default is always assist LONG_PRESS_HOME_ASSIST = 2
        int defaultLongPressOnHomeBehavior = (deviceKeys == 0) ? 2 : getResources().getInteger(com.android.internal.R.integer.config_longPressOnHomeBehavior);
        mLongPressHomeAction = (ListPreference) findPreference(LONG_PRESS_HOME_ACTION);
        int longPressHomeAction = Settings.System.getInt(resolver,
                Settings.System.BUTTON_LONG_PRESS_HOME, defaultLongPressOnHomeBehavior);

        mBackKillTimeout = (ListPreference) findPreference(BUTTON_BACK_KILL_TIMEOUT);
        final int backKillTimeoutDefault = getResources().getInteger(com.android.internal.R.integer.config_backKillTimeout);
        final int backKillTimeout = Settings.System.getInt(resolver,
                Settings.System.BUTTON_BACK_KILL_TIMEOUT, backKillTimeoutDefault);

        mBackKillTimeout.setValue(Integer.toString(backKillTimeout));
        mBackKillTimeout.setSummary(mBackKillTimeout.getEntry());
        mBackKillTimeout.setOnPreferenceChangeListener(this);

        mLongPressHomeAction.setValue(Integer.toString(longPressHomeAction));
        mLongPressHomeAction.setSummary(mLongPressHomeAction.getEntry());
        mLongPressHomeAction.setOnPreferenceChangeListener(this);

        int defaultDoublePressOnHomeBehavior = getResources().getInteger(com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
        mDoublePressHomeAction = (ListPreference) findPreference(DOUBLE_PRESS_HOME_ACTION);
        int doublePressHomeAction = Settings.System.getInt(resolver,
                Settings.System.BUTTON_DOUBLE_PRESS_HOME, defaultDoublePressOnHomeBehavior);

        mDoublePressHomeAction.setValue(Integer.toString(doublePressHomeAction));
        mDoublePressHomeAction.setSummary(mDoublePressHomeAction.getEntry());
        mDoublePressHomeAction.setOnPreferenceChangeListener(this);

        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mFingerprintVib = (SwitchPreference) findPreference(FINGERPRINT_VIB);
        if (!mFingerprintManager.isHardwareDetected()){
            prefScreen.removePreference(mFingerprintVib);
        } else {
            mFingerprintVib.setChecked((Settings.System.getInt(getContentResolver(),
                    Settings.System.FINGERPRINT_SUCCESS_VIB, 1) == 1));
            mFingerprintVib.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mEnableNavBar) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW, checked ? 1:0);
            // remove hw button disable if we disable navbar
            if (!checked) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.HARDWARE_KEYS_DISABLE, 0);
                mDisabkeHWKeys.setChecked(false);
            }
            return true;
        } else if (preference == mDisabkeHWKeys) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HARDWARE_KEYS_DISABLE, checked ? 1:0);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavbarRecentsStyle) {
            int value = Integer.valueOf((String) newValue);
            if (value == 1) {
                if (!isOmniSwitchInstalled()){
                    doOmniSwitchUnavail();
                } else if (!OmniSwitchConstants.isOmniSwitchRunning(getActivity())) {
                    doOmniSwitchConfig();
                }
            }
            int index = mNavbarRecentsStyle.findIndexOfValue((String) newValue);
            mNavbarRecentsStyle.setSummary(mNavbarRecentsStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_RECENTS, value);
            return true;
        } else if (preference == mLongPressRecentsAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mLongPressRecentsAction.findIndexOfValue((String) newValue);
            mLongPressRecentsAction.setSummary(mLongPressRecentsAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.BUTTON_LONG_PRESS_RECENTS, value);
            return true;
        } else if (preference == mLongPressHomeAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mLongPressHomeAction.findIndexOfValue((String) newValue);
            mLongPressHomeAction.setSummary(mLongPressHomeAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.BUTTON_LONG_PRESS_HOME, value);
            return true;
        } else if (preference == mDoublePressHomeAction) {
            int value = Integer.valueOf((String) newValue);
            int index = mDoublePressHomeAction.findIndexOfValue((String) newValue);
            mDoublePressHomeAction.setSummary(mDoublePressHomeAction.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.BUTTON_DOUBLE_PRESS_HOME, value);
            return true;
        } else if (preference == mBackKillTimeout) {
            int value = Integer.valueOf((String) newValue);
            int index = mBackKillTimeout.findIndexOfValue((String) newValue);
            mBackKillTimeout.setSummary(mBackKillTimeout.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.BUTTON_BACK_KILL_TIMEOUT, value);
            return true;
        } else if (preference == mFingerprintVib) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FINGERPRINT_SUCCESS_VIB, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void checkForOmniSwitchRecents() {
        if (!isOmniSwitchInstalled()){
            doOmniSwitchUnavail();
        } else if (!OmniSwitchConstants.isOmniSwitchRunning(getActivity())) {
            doOmniSwitchConfig();
        }
    }

    private void doOmniSwitchConfig() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_running_new)
            .setPositiveButton(R.string.omniswitch_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    startActivity(OmniSwitchConstants.INTENT_LAUNCH_APP);
                }
            });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void doOmniSwitchUnavail() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.omniswitch_title);
        alertDialogBuilder.setMessage(R.string.omniswitch_dialog_unavail);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private boolean isOmniSwitchInstalled() {
        return PackageUtils.isAvailableApp(OmniSwitchConstants.APP_PACKAGE_NAME, getActivity());
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.button_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    final Resources res = context.getResources();
                    final int deviceKeys = res.getInteger(
                            com.android.internal.R.integer.config_deviceHardwareKeys);

                    if (deviceKeys == 0) {
                        result.add(CATEGORY_KEYS);
                    }
                    return result;
                }
            };
}
