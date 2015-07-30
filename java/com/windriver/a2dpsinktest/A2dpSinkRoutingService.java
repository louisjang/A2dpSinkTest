package com.windriver.a2dpsinktest;

import android.app.Service;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDevicePort;
import android.media.AudioDevicePortConfig;
import android.media.AudioManager;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.AudioSystem;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class A2dpSinkRoutingService extends Service {
    public static final String TAG = "A2dpSinkRoutingService";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED)) {
                boolean connected = (intent.getIntExtra(BluetoothA2dpSink.EXTRA_STATE, -1) == BluetoothA2dpSink.STATE_CONNECTED);
                if (connected)
                    connectAudioPatch();
                else
                    disconnectAudioPatch();
            } else {
                Log.v(TAG, "Unknown intent: action = " + action);
            }
        }
    };

    AudioPatch mAudioPatch;

    void connectAudioPatch() {
        Log.v(TAG, "connectAudioPatch");
        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        //FIXME: AudioManager.listAudioDevicePorts caches AudioPort array, and donot invalid it
        //when A2DP sink state is changed.
        ArrayList<AudioPort> ports = new ArrayList<AudioPort>();
        audio.listAudioDevicePorts(ports);

        AudioDevicePort sourcePort = null;
        AudioDevicePort sinkPort = null;

        for (AudioPort port : ports) {
            Log.v(TAG, "port = " + port);
            int type = ((AudioDevicePort) port).type();
            if (type == AudioSystem.DEVICE_IN_BLUETOOTH_A2DP) {
                sourcePort = (AudioDevicePort) port;
            } else if (type == AudioSystem.DEVICE_OUT_SPEAKER) {
                sinkPort = (AudioDevicePort) port;
            }
        }

        if (sourcePort != null && sinkPort != null) {
            Log.v(TAG, "ready to make AudioPatch");

            AudioDevicePortConfig sourceConfig = (AudioDevicePortConfig) sourcePort.activeConfig();
            AudioDevicePortConfig sinkConfig = (AudioDevicePortConfig) sinkPort.activeConfig();
            AudioPatch[] audioPatchArray = new AudioPatch[] {null};
            int ret = audio.createAudioPatch(audioPatchArray,
                    new AudioPortConfig[] {sourceConfig},
                    new AudioPortConfig[] {sinkConfig});
            if (ret != 0) {
                Log.e(TAG, "Can't create AudioPatch: ret = " + ret);
                return;
            }
            mAudioPatch = audioPatchArray[0];
        }
    }

    void disconnectAudioPatch() {
        Log.v(TAG, "disconnectAudioPatch");
        if (mAudioPatch != null) {
            AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audio.releaseAudioPatch(mAudioPatch);
            mAudioPatch = null;
        }
    }

    public A2dpSinkRoutingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
