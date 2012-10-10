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

import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.*;

public class PlaybackService extends Service {
    public static final String ACTION_ENQUEUE = "com.gmail.altakey.ray.PlaybackService.actions.ENQUEUE";

    private final Player mPlayer = new Player();

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

    private class Player {
        private MediaPlayer mmPlayer;
        private boolean mmPlaying = false;
        private Queue<Uri> mmQueue = new ConcurrentLinkedQueue<Uri>();
        private MediaPlayerEventListener mmListener = new MediaPlayerEventListener();

        public void start() throws IOException {
            mmPlaying = false;
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

    private class Cacher {
        private Uri mmUri;

        public Cacher(Uri uri) {
            mmUri = uri;
        }

        public File cache() throws IOException {
            ReadableByteChannel src = null;
            FileChannel dest = null;
            File destFile = null;

            try {
                destFile = new File(root(), randomName());
                src = Channels.newChannel(getContentResolver().openInputStream(mmUri));
                dest = new FileOutputStream(destFile).getChannel();

                ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
                while (src.read(buffer) != -1) {
                    buffer.flip();
                    dest.write(buffer);
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    dest.write(buffer);
                }

                Log.d("MA", String.format("cached %s as %s", mmUri.toString(), destFile.getName()));
                return destFile;
            } catch (IOException e) {
                Log.e("MA", "cannot cache", e);
                destFile.delete();
                throw e;
            } finally {
                if (src != null) {
                    try {
                        src.close();
                    } catch (IOException e) {
                    }
                }
                if (dest != null) {
                    try {
                        dest.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        private String randomName() {
            byte[] buffer = new byte[32];
            new Random().nextBytes(buffer);

            StringBuilder sb = new StringBuilder();
            try {
                for (byte b : MessageDigest.getInstance("MD5").digest(buffer)) {
                    sb.append(Integer.toHexString(((int)b) & 0xff));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private File root() {
            return getExternalFilesDir(null);
        }
    }

}
