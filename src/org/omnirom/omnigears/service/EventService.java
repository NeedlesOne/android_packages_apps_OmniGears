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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.HandlerThread;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EventService extends Service {
    private static final String TAG = "OmniEventService";
    private static final boolean DEBUG = true;
    private PowerManager.WakeLock mWakeLock;

    private BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mWakeLock.acquire();
            try {
                if (DEBUG) Log.d(TAG, "onReceive " + action);
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                    } else {
                    }
                }
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                }
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                }
                if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                            BluetoothProfile.STATE_CONNECTED);
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = true" );
                    } else {
                        if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = false" );
                    }
                }
                if (AudioManager.ACTION_HEADSET_PLUG.equals(action)) {
                    boolean useHeadset = intent.getIntExtra("state", 0) == 1;
                    if (useHeadset) {
                        if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG = true" );
                    } else {
                        if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG = false" );
                    }
                }
                
            } finally {
                mWakeLock.release();
            }
        }
    };

    public class LocalBinder extends Binder {
        public EventService getService() {
            return EventService.this;
        }
    }
    private final LocalBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(true);

        registerListener();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");
        unregisterListener();
    }

    private void registerListener() {
        if (DEBUG) Log.d(TAG, "registerListener");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        this.registerReceiver(mStateListener, filter);
    }

    private void unregisterListener() {
        if (DEBUG) Log.d(TAG, "unregisterListener");
        try {
            this.unregisterReceiver(mStateListener);
        } catch (Exception e) {
        }
    }
}
