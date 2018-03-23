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

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.Log;

import org.omnirom.omnigears.ui.MultiAppSelectorActivity;
import org.omnirom.omnigears.utils.SetStringPackUtils;

import java.util.Set;


public class EventService extends Service {
    private static final String TAG = "OmniEventService";
    private static final boolean DEBUG = false;
    private PowerManager.WakeLock mWakeLock;
    private static boolean mIsRunning;
    private static boolean mWiredHeadsetConnected;
    private static boolean mA2DPConnected;

    private BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mWakeLock.acquire();

            try {
                if (DEBUG) Log.d(TAG, "onReceive " + action);

                boolean disableIfMusicActive = getPrefs(context).getBoolean(EventServiceSettings.EVENT_MUSIC_ACTIVE, true);

                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                            mA2DPConnected = false;
                        }
                        break;
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                                BluetoothProfile.STATE_CONNECTED);
                        if (state == BluetoothProfile.STATE_CONNECTED && !mA2DPConnected) {
                            mA2DPConnected = true;
                            if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = true");

                            if (!(disableIfMusicActive && isMusicActive())) {
                                Set<String> apps = getPrefs(context).getStringSet(EventServiceSettings.EVENT_A2DP_CONNECT, null);
                                if (apps != null) {
                                    openMultiAppSelector(apps, context);
                                }
                            }
                        } else {
                            mA2DPConnected = false;
                            if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = false");
                        }
                        break;
                    case AudioManager.ACTION_HEADSET_PLUG:
                        boolean useHeadset = intent.getIntExtra("state", 0) == 1;
                        if (useHeadset && !mWiredHeadsetConnected) {
                            mWiredHeadsetConnected = true;
                            if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG = true");

                            if (!(disableIfMusicActive && isMusicActive())) {
                                Set<String> apps = getPrefs(context).getStringSet(EventServiceSettings.EVENT_WIRED_HEADSET_CONNECT, null);
                                if (apps != null) {
                                    openMultiAppSelector(apps, context);
                                }
                            }
                        } else {
                            mWiredHeadsetConnected = false;
                            if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG = false");
                        }
                        break;
                }

            } finally {
                mWakeLock.release();
            }
        }
    };

    private boolean isMusicActive() {
        if (AudioSystem.isStreamActive(AudioSystem.STREAM_MUSIC, 0)) {
            // local / wired / BT playback active
            if (DEBUG) Log.d(TAG, "isMusicActive(): local");
            return true;
        }
        if (AudioSystem.isStreamActiveRemotely(AudioSystem.STREAM_MUSIC, 0)) {
            // remote submix playback active
            if (DEBUG) Log.d(TAG, "isMusicActive(): remote submix");
            return true;
        }
        if (DEBUG) Log.d(TAG, "isMusicActive(): no");
        return false;
    }

    private void openMultiAppSelector(Set<String> apps, Context context) {
        Intent intent = new Intent(context, MultiAppSelectorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(MultiAppSelectorActivity.APPS, SetStringPackUtils.packSet(apps));
        intent.putExtra(MultiAppSelectorActivity.AUTORUN_SINGLE,
                getPrefs(context).getBoolean(EventServiceSettings.EVENT_AUTORUN_SINGLE, true));
        intent.putExtra(MultiAppSelectorActivity.MEDIA_PLAYER_START,
                getPrefs(context).getBoolean(EventServiceSettings.EVENT_MEDIA_PLAYER_START, false));
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

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
        mIsRunning = true;
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
        mIsRunning = false;
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

    public static boolean isRunning() {
        return mIsRunning;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(EventServiceSettings.EVENTS_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
