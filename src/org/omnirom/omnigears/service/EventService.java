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
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.omnirom.omnigears.R;

import java.util.Set;


public class EventService extends Service {
    private static final String TAG = "OmniEventService";
    private static final boolean DEBUG = false;
    private PowerManager.WakeLock mWakeLock;
    private static boolean mIsRunning;
    private static boolean mWiredHeadsetConnected;
    private static boolean mA2DPConnected;

    private WindowManager mWindowManager;
    private View mFloatingWidget = null;
    private Set<String> appList = null;
    private Handler mHandler = new Handler();
    private PackageManager mPm;

    private BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mWakeLock.acquire();

            try {
                if (DEBUG) Log.d(TAG, "onReceive " + action);

                boolean disableIfMusicActive = getPrefs(context).getBoolean(EventServiceSettings.EVENT_MUSIC_ACTIVE, true);
                boolean autoRun = getPrefs(context).getBoolean(EventServiceSettings.EVENT_AUTORUN_SINGLE, true);

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
                                appList = getPrefs(context).getStringSet(EventServiceSettings.EVENT_A2DP_CONNECT, null);
                                if (appList != null) {
                                    if (autoRun && appList.size() == 1) {
                                        openApp(appList.iterator().next(), context);
                                    } else {
                                        openAppChooserDialog(context);
                                    }
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
                                appList = getPrefs(context).getStringSet(EventServiceSettings.EVENT_WIRED_HEADSET_CONNECT, null);
                                if (appList != null) {
                                    if (autoRun && appList.size() == 1) {
                                        openApp(appList.iterator().next(), context);
                                    } else {
                                        openAppChooserDialog(context);
                                    }
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

    public void openAppChooserDialog(final Context context) {
        final LayoutInflater inflater = LayoutInflater.from(new ContextThemeWrapper(
                context, android.R.style.Theme_DeviceDefault_Light_Dialog));
        mFloatingWidget = inflater.inflate(R.layout.layout_floating_widget, null);

        if (mFloatingWidget.getWindowToken() == null) {
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.x = 25;
            params.windowAnimations = android.R.style.Animation_Dialog;

            int chooserPosition = getPrefs(context).getInt(EventServiceSettings.APP_CHOOSER_POSITION, 0);
            if (chooserPosition == 0) {
                params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
            } else {
                params.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
            }

            // Selected apps
            LinearLayout linearLayout = (LinearLayout) mFloatingWidget.findViewById(R.id.selected_apps);
            if (linearLayout.getChildCount() > 0) linearLayout.removeAllViews();

            for (final String value : appList) {
                try {
                    View v = inflater.inflate(R.layout.app_grid_item, null);
                    ComponentName componentName = ComponentName.unflattenFromString(value);
                    Drawable icon = mPm.getActivityIcon(componentName);
                    ((ImageView) v.findViewById(R.id.appIcon)).setImageDrawable(icon);
                    v.setPadding(30, 15, 30, 15);
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mWindowManager.removeViewImmediate(mFloatingWidget);
                            openApp(value, context);
                        }
                    });
                    linearLayout.addView(v);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Set app icon", e);
                }
            }

            // Close button
            View close = inflater.inflate(R.layout.app_grid_item, null);
            ((ImageView) close.findViewById(R.id.appIcon)).setImageResource(R.drawable.ic_disabled);
            close.setPadding(30, 15, 30, 15);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mWindowManager.removeViewImmediate(mFloatingWidget);
                }
            });
            linearLayout.addView(close);

            mWindowManager.addView(mFloatingWidget, params);

            final int timeout = getPrefs(context).getInt(EventServiceSettings.APP_CHOOSER_TIMEOUT, 15);
            if (timeout > 0) {
                Handler mCloseHandler = new Handler();
                mCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean stillThere = (mFloatingWidget != null
                                && mFloatingWidget.getWindowToken() != null);

                        if (stillThere) {
                            mWindowManager.removeViewImmediate(mFloatingWidget);
                        }
                    }
                }, timeout * 1000);
            }
        }
    }

    private void openApp(String app_uri, Context context) {
        try {
            startActivityAsUser(createIntent(app_uri), UserHandle.CURRENT);
            if (getPrefs(context).getBoolean(EventServiceSettings.EVENT_MEDIA_PLAYER_START, false)) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dispatchMediaKeyToAudioService(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                    }
                }, 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "MultiAppSelector.EVENT_MEDIA_PLAYER_START", e);
        }
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

    private Intent createIntent(String value) {
        ComponentName componentName = ComponentName.unflattenFromString(value);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(componentName);
        return intent;
    }

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
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mPm = getPackageManager();
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
            Log.e(TAG, "unregisterListener", e);
        }
    }

    public static boolean isRunning() {
        return mIsRunning;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(EventServiceSettings.EVENTS_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
