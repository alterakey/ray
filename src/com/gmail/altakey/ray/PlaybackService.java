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
import android.widget.Toast;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.support.v4.content.LocalBroadcastManager;

public class PlaybackService extends Service {
    public static final String ACTION_ENQUEUE = "com.gmail.altakey.ray.PlaybackService.actions.ENQUEUE";
    public static final String ACTION_CLEAR = "com.gmail.altakey.ray.PlaybackService.actions.CLEAR";
    public static final String ACTION_UPDATE_QUEUE = "com.gmail.altakey.ray.PlaybackService.actions.UPDATE_QUEUE";

    private final Player mPlayer = new Player();
    private static final Queue<Uri> sQueue = new ConcurrentLinkedQueue<Uri>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_ENQUEUE.equals(intent.getAction())) {
                try {
                    mPlayer.enqueue(intent.getData());
                    Toast.makeText(this, "Queued in playlist.", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.d("PS", String.format("cannot start player: %s", e.toString()));
                    stopSelf();
                }
            }
            if (ACTION_CLEAR.equals(intent.getAction())) {
                mPlayer.stop();
                Toast.makeText(this, "Cleared playlist.", Toast.LENGTH_SHORT).show();
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
            .setContentText("Playing...")
            .setContentIntent(PendingIntent.getActivity(this, 0, content, PendingIntent.FLAG_UPDATE_CURRENT))
            .setSmallIcon(R.drawable.icon)
            .getNotification();
        startForeground(1, noti);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.stop();
        stopForeground(true);
    }

    public static Queue<Uri> getPlaylist() {
        return sQueue;
    }

    private class Player {
        private MediaPlayer mmPlayer;
        private boolean mmPlaying = false;
        private MediaPlayerEventListener mmListener = new MediaPlayerEventListener();

        public void start() throws IOException {
            mmPlaying = false;
            if (mmPlayer == null) {
                mmPlayer = new MediaPlayer();
                mmPlayer.setOnCompletionListener(mmListener);
            } else {
                mmPlayer.reset();
            }
            if (!sQueue.isEmpty()) {
                Uri uri = sQueue.peek();
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
            purgeAll();
            sQueue.clear();
            notifyQueueUpdate();
        }

        public void enqueue(Uri uri) throws IOException {
            sQueue.add(uri);
            Log.d("PS", String.format("queued: %s", uri.toString()));
            notifyQueueUpdate();
            if (!mmPlaying)
                start();
        }

        public void skip() throws IOException {
            Uri played = sQueue.poll();
            purge(played);
            notifyQueueUpdate();
            start();
        }

        private void purgeAll() {
            for (Uri uri : sQueue) {
                purge(uri);
            }
        }
        private void purge(Uri uri) {
            if ("file".equals(uri.getScheme())) {
                Log.d("PS", String.format("deleting: %s", uri.getPath()));
                new File(uri.getPath()).delete();
            }
        }

        private void notifyQueueUpdate() {
            Intent intent = new Intent(PlaybackService.ACTION_UPDATE_QUEUE);
            LocalBroadcastManager.getInstance(PlaybackService.this).sendBroadcast(intent);
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
