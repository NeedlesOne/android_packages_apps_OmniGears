package org.omnirom.omnigears.ui;


import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.IAudioService;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;

import org.omnirom.omnigears.R;
import org.omnirom.omnigears.utils.SetStringPackUtils;

public class MultiAppSelectorActivity extends Activity {
    private static final String TAG = "MultiAppSelector";
    private static final boolean DEBUG = false;

    public static final String APPS = "apps";
    public static final String MEDIA_PLAYER_START = "media_player_start";
    public static final String AUTORUN_SINGLE = "autorun_single";

    private Handler mHandler = new Handler();
    private Boolean mediaStart = false;
    private Boolean autoRun = false;
    private Object[] appList = null;
    private GridView gridview;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_grid_view);
        Intent intent = getIntent();

        mediaStart = intent.getBooleanExtra(MEDIA_PLAYER_START, false);
        autoRun = intent.getBooleanExtra(AUTORUN_SINGLE, false);
        appList = SetStringPackUtils.unpackString(intent.getStringExtra(APPS)).toArray();

        gridview = (GridView) findViewById(R.id.app_grid_view);
        gridview.setAdapter(new AppGridAdapter(MultiAppSelectorActivity.this, appList));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openApp((String) appList[position]);
            }
        });

        if (autoRun && appList.length == 1) { // If there is only one app open it
            openApp((String) appList[0]);
        }
    }

    private void openApp(String app_uri) {
        try {
            startActivityAsUser(createIntent(app_uri), UserHandle.CURRENT);
            if (mediaStart) {
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
