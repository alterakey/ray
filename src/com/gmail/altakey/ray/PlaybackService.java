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

import java.io.IOException;
import java.io.File;

public class PlaybackService extends Service {
    public static final String ACTION_ENQUEUE = "com.gmail.altakey.ray.PlaybackService.actions.ENQUEUE";

    private final Player mPlayer = new Player();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_ENQUEUE.equals(intent.getAction())) {
            enqueue(intent.getData());
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mPlayer.start();
        } catch (IOException e) {
            Log.d("PS", String.format("cannot start player: %s", e.toString()));
            stopSelf();
            return;
        }

        Notification noti = new NotificationCompat.Builder(this)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.accepting_stream))
            .setSmallIcon(R.drawable.icon)
            .getNotification();
        startForeground(1, noti);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    private void enqueue(Uri uri) {
        Log.d("PS", String.format("would enqueue: %s", uri.toString()));
    }

    private class Player {
        private MediaPlayer mmPlayer;

        public void start() throws IOException {
            if (mmPlayer == null) {
                mmPlayer = new MediaPlayer();
                mmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mmPlayer.setDataSource(getCorpseFile().getAbsolutePath());
                mmPlayer.prepare();
                mmPlayer.start();
            } else {
                mmPlayer.start();
            }
        }

        public void stop() {
            if (mmPlayer != null) {
                mmPlayer.release();
                mmPlayer = null;
            }
        }

        private File getCorpseFile() throws IOException {
            File root = Environment.getExternalStorageDirectory();
            if (root == null) {
                throw new IOException("cannot open external storage root");
            }
            return new File(root, "corpse.m4a");
        }
    }
}
