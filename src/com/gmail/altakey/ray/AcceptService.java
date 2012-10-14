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

            String msg = String.format("%s (%s)", getString(R.string.accepting_stream), mServer.getWifiInet4Address());

            Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(msg)
                .setContentIntent(PendingIntent.getActivity(this, 0, content, PendingIntent.FLAG_UPDATE_CURRENT))
                .setTicker(msg)
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
        private int mmWifiAddress = 0;

        public ServerController() {
            WifiManager wfm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            PowerManager pwm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            try {
                mmWifiLock = wfm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "com.gmail.altakey.ray.AcceptService");
            } catch (NoSuchFieldError e) {
                mmWifiLock = wfm.createWifiLock(WifiManager.WIFI_MODE_FULL, "com.gmail.altakey.ray.AcceptService");
            }
            mmWakeLock = pwm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.gmail.altakey.ray.AcceptService");

            if (wfm.isWifiEnabled()) {
                mmWifiAddress = wfm.getConnectionInfo().getIpAddress();
            }

        }

        public String getWifiInet4Address() {
            return String.format("%d.%d.%d.%d:8080", mmWifiAddress >> 0 & 0xff, mmWifiAddress >> 8 & 0xff, mmWifiAddress >> 16 & 0xff, mmWifiAddress >> 24 & 0xff);
        }

        public void start() throws IOException {
            mmWakeLock.acquire();
            mmWifiLock.acquire();
            if (mmServer == null) {
                mmServer = new HelloServer(AcceptService.this, 8080, getExternalFilesDir(null));
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
