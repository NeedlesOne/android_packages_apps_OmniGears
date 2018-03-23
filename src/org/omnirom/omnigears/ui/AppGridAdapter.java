/*
 * Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnigears.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.omnirom.omnigears.R;

public class AppGridAdapter extends BaseAdapter {
    private static final String TAG = "AppGridAdapter";

    private LayoutInflater layoutinflater;
    private Object[] appList;
    private PackageManager mPm;

    public AppGridAdapter(Context context, Object[] customizedListView) {
        mPm = context.getPackageManager();
        layoutinflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        appList = customizedListView;
    }

    @Override
    public int getCount() {
        return appList.length;
    }

    @Override
    public Object getItem(int position) {
        return appList[position];
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder listViewHolder;

        if (convertView == null) {
            listViewHolder = new ViewHolder();
            convertView = layoutinflater.inflate(R.layout.app_grid_item, parent, false);
            listViewHolder.imageInListView = (ImageView) convertView.findViewById(R.id.appIcon);
            convertView.setTag(listViewHolder);
        } else {
            listViewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            ComponentName componentName = ComponentName.unflattenFromString((String) appList[position]);
            Drawable icon = mPm.getActivityIcon(componentName);
            listViewHolder.imageInListView.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Set app icon", e);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView imageInListView;
    }
}
