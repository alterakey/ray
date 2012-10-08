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
            try {
                mPlayer.enqueue(intent.getData());
            } catch (IOException e) {
                Log.d("PS", String.format("cannot start player: %s", e.toString()));
                stopSelf();
            }
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

        Intent content = new Intent(this, MainActivity.class);
        content.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification noti = new NotificationCompat.Builder(this)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.accepting_stream))
            .setContentIntent(PendingIntent.getActivity(this, 0, content, PendingIntent.FLAG_UPDATE_CURRENT))
            .setTicker(getString(R.string.accepting_stream))
            .setSmallIcon(R.drawable.icon)
            .getNotification();
        startForeground(1, noti);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    private class Player {
        private MediaPlayer mmPlayer;
        private boolean mmPlaying = false;
        private Queue<Uri> mmQueue = new ConcurrentLinkedQueue<Uri>();
        private MediaPlayerEventListener mmListener = new MediaPlayerEventListener();

        public void start() throws IOException {
            if (mmPlayer == null) {
                mmPlayer = new MediaPlayer();
                mmPlayer.setOnCompletionListener(mmListener);
            } else {
                mmPlayer.reset();
            }
            if (!mmQueue.isEmpty()) {
                Uri uri = mmQueue.peek();
                mmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mmPlayer.setDataSource(PlaybackService.this, uri);
                mmPlayer.prepare();
                mmPlayer.start();
                mmPlaying = true;
            }
        }

        public void stop() {
            if (mmPlayer != null) {
                mmPlayer.release();
                mmPlayer = null;
                mmPlaying = false;
            }
        }

        public void enqueue(Uri uri) throws IOException {
            mmQueue.add(uri);
            Log.d("PS", String.format("queued: %s", uri.toString()));
            if (!mmPlaying)
                start();
        }

        public void skip() throws IOException {
            mmQueue.poll();
            start();
        }

        private class MediaPlayerEventListener implements MediaPlayer.OnCompletionListener {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    skip();
                } catch (IOException e) {
                    onCompletion(mp);
                }
            }
        }
    }
}
