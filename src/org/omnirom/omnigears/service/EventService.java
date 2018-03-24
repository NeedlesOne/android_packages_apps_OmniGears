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

import android.app.ActivityManagerNative;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.IAudioService;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import org.omnirom.omnigears.preference.AppSelectListPreference;

import java.util.ArrayList;
import java.util.List;

public class EventService extends Service {
    private static final String TAG = "OmniEventService";
    private static final boolean DEBUG = true;
    private PowerManager.WakeLock mWakeLock;
    private static boolean mIsRunning;
    private Handler mHandler = new Handler();
    private boolean mWiredHeadsetConnected;
    private boolean mA2DPConnected;

    private BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mWakeLock.acquire();
            try {
                if (DEBUG) Log.d(TAG, "onReceive " + action);
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                        mA2DPConnected = false;
                    }
                }
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                }
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                }
                if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                            BluetoothProfile.STATE_CONNECTED);
                    if (state == BluetoothProfile.STATE_CONNECTED && !mA2DPConnected) {
                        mA2DPConnected = true;
                        if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = true" );
                        String app = getPrefs(context).getString(EventServiceSettings.EVENT_A2DP_CONNECT, null);
                        if (!TextUtils.isEmpty(app) && !app.equals(AppSelectListPreference.DISABLED_ENTRY)) {
                            if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG app = " + app);
                            try {
                                context.startActivityAsUser(createIntent(app), UserHandle.CURRENT);
                                if (getPrefs(context).getBoolean(EventServiceSettings.EVENT_MEDIA_PLAYER_START, false)) {
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            dispatchMediaKeyToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                        }
                                    }, 1000);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "BluetoothProfile.STATE_CONNECTED", e);
                            }
                        }
                    } else {
                        mA2DPConnected = false;
                        if (DEBUG) Log.d(TAG, "BluetoothProfile.STATE_CONNECTED = false" );
                    }
                }
                if (AudioManager.ACTION_HEADSET_PLUG.equals(action)) {
                    boolean useHeadset = intent.getIntExtra("state", 0) == 1;
                    if (useHeadset && !mWiredHeadsetConnected) {
                        mWiredHeadsetConnected = true;
                        if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG = true" );
                        String app = getPrefs(context).getString(EventServiceSettings.EVENT_WIRED_HEADSET_CONNECT, null);
                        if (!TextUtils.isEmpty(app) && !app.equals(AppSelectListPreference.DISABLED_ENTRY)) {
                            if (DEBUG) Log.d(TAG, "AudioManager.ACTION_HEADSET_PLUG app = " + app);
                            try {
                                context.startActivityAsUser(createIntent(app), UserHandle.CURRENT);
                                if (getPrefs(context).getBoolean(EventServiceSettings.EVENT_MEDIA_PLAYER_START, false)) {
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            dispatchMediaKeyToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                        }
                                    }, 1000);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "AudioManager.ACTION_HEADSET_PLUG", e);
                            }
                        }
                    } else {
                        mWiredHeadsetConnected = false;
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

    private Intent createIntent(String value) {
        ComponentName componentName = ComponentName.unflattenFromString(value);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(componentName);
        return intent;
    }

    private void dispatchMediaKeyToAudioService(int keycode) {
        if (ActivityManagerNative.isSystemReady()) {
            IAudioService audioService = IAudioService.Stub
                    .asInterface(ServiceManager.checkService(Context.AUDIO_SERVICE));
            if (audioService != null) {
                if (DEBUG) Log.d(TAG, "dispatchMediaKeyToAudioService " + keycode);

                KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                        keycode, 0);
                MediaSessionLegacyHelper.getHelper(this).sendMediaButtonEvent(event, true);
                event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
                MediaSessionLegacyHelper.getHelper(this).sendMediaButtonEvent(event, true);
            }
        }
    }
}
