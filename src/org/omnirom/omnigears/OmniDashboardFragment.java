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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OmniDashboardFragment extends DashboardFragment {

    private static final String TAG = "OmniDashboardFragment";
    public static final String CATEGORY_OMNI = "com.android.settings.category.ia.omni";
    private static final String KEY_DEVICE_PARTS = "device_parts";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        if (!isDevicePartsSupported(getContext())) {
            Preference pref = getPreferenceScreen().findPreference(KEY_DEVICE_PARTS);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.omni_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return null;
    }

    private static boolean isDevicePartsSupported(Context context) {
        boolean devicePartsSupported = false;
        try {
            devicePartsSupported = context.getPackageManager().getPackageInfo(
                    "org.omnirom.device", 0).versionCode > 0;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return devicePartsSupported;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.omni_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!isDevicePartsSupported(context)) {
                        result.add(KEY_DEVICE_PARTS);
                    }
                    return result;
                }
            };
}
