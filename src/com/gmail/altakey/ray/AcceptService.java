package com.gmail.altakey.ray;

import android.app.Service;
import android.os.IBinder;
import android.net.Uri;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.Environment;
import android.util.Log;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.*;
import android.widget.Toast;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.os.PowerManager;

public class AcceptService extends Service {
    private ServerController mServer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mServer = new ServerController();

        Intent content = new Intent(this, MainActivity.class);
        content.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            mServer.start();

            Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.accepting_stream))
                .setContentIntent(PendingIntent.getActivity(this, 0, content, PendingIntent.FLAG_UPDATE_CURRENT))
                .setTicker(getString(R.string.accepting_stream))
                .setSmallIcon(R.drawable.icon)
                .getNotification();
            startForeground(1, noti);
        } catch (IOException e) {
            Log.e("AS", "cannot start NanoHTTPD", e);
            Toast.makeText(this, String.format("cannot start server: %s", e.getMessage()), Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServer.stop();
        stopForeground(true);
    }

    private class ServerController {
        private HelloServer mmServer = null;
        private WifiManager.WifiLock mmWifiLock = null;
        private PowerManager.WakeLock mmWakeLock = null;

        public ServerController() {
            WifiManager wfm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            PowerManager pwm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mmWifiLock = wfm.createWifiLock(WifiManager.WIFI_MODE_FULL, "com.gmail.altakey.ray.AcceptService");
            mmWakeLock = pwm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.gmail.altakey.ray.AcceptService");
        }

        public void start() throws IOException {
            mmWakeLock.acquire();
            mmWifiLock.acquire();
            if (mmServer == null) {
                mmServer = new HelloServer(8080, getExternalFilesDir(null));
            }
        }

        public void stop() {
            try {
                if (mmServer != null) {
                    mmServer.stop();
                    mmServer = null;
                }
            } finally {
                mmWifiLock.release();
                mmWakeLock.release();
            }
        }
    }
}
