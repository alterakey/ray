package com.gmail.altakey.ray;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.net.Uri;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

import android.net.http.AndroidHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.*;
import android.os.AsyncTask;

public class EnqueueActivity extends Activity {
    private static final String[] FRIENDS = { "192.168.1.17:8080", "192.168.1.15:8080", "10.0.0.52:8080" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String action = getIntent().getAction();
        final Bundle extras = getIntent().getExtras();

        try {
            if (Intent.ACTION_SEND.equals(action)) {
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    new EnqueueToFriendTask(FRIENDS[0], (Uri)extras.getParcelable(Intent.EXTRA_STREAM)).execute();
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                if (extras.containsKey(Intent.EXTRA_STREAM)) {
                    for (Parcelable p : extras.getParcelableArrayList(Intent.EXTRA_STREAM)) {
                        new EnqueueToFriendTask(FRIENDS[0], (Uri)p).execute();
                    }
                }
            }
        } finally {
            finish();
        }
    }

    private class EnqueueToFriendTask extends AsyncTask<Void, Void, Throwable> {
        private Uri mmUri;
        private String mmFriendAddress;

        public EnqueueToFriendTask(String friendAddress, Uri uri) {
            mmUri = uri;
            mmFriendAddress = friendAddress;
        }

        @Override
        public void onPreExecute() {
            Toast.makeText(EnqueueActivity.this, "Sending...", Toast.LENGTH_LONG).show();
        }

        @Override
        public Throwable doInBackground(Void... args) {
            File tempFile = null;
            try {
                tempFile = new Cacher().cache();
                new Enqueuer(tempFile).enqueue();
                return null;
            } catch (IOException e) {
                Log.e("EA", "Cannot send to remote playlist", e);
                return e;
            } finally {
                if (tempFile != null) {
                    tempFile.delete();
                }
            }
        }

        @Override
        public void onPostExecute(Throwable ret) {
            if (ret == null) {
                Toast.makeText(EnqueueActivity.this, "Sent to remote playlist.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EnqueueActivity.this, "Cannot send to remote playlist", Toast.LENGTH_SHORT).show();
            }
        }

        private class Enqueuer {
            private AndroidHttpClient mmmHttpClient;
            private File mmmBlob;

            public Enqueuer(File blob) {
                mmmHttpClient = AndroidHttpClient.newInstance(getUserAgent());
                mmmBlob = blob;
            }

            private String getUserAgent() {
                return String.format("%s/%s", getString(R.string.app_name), "0.0.1");
            }

            public void enqueue() throws IOException {
                HttpPost req = new HttpPost(String.format("http://%s", mmFriendAddress));
                MultipartEntity entity = new MultipartEntity();
                entity.addPart("stream", new FileBody(mmmBlob, "application/octet-stream"));
                req.setEntity(entity);
                int code = mmmHttpClient.execute(req).getStatusLine().getStatusCode();
                Log.d("EA", String.format("posted, code=%d", code));
            }
        }

        private class Cacher {
            public File cache() throws IOException {
                FileChannel src = null;
                FileChannel dest = null;
                File destFile = null;

                try {
                    destFile = new File(root(), randomName());
                    src = new FileInputStream(getContentResolver().openFileDescriptor(mmUri, "r").getFileDescriptor()).getChannel();
                    dest = new FileOutputStream(destFile).getChannel();

                    dest.transferFrom(src, 0, Integer.MAX_VALUE);

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
}
