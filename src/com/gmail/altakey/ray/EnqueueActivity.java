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
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.*;

public class EnqueueActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String action = getIntent().getAction();
        final Bundle extras = getIntent().getExtras();

        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                new EnqueueTaskInvoker((Uri)extras.getParcelable(Intent.EXTRA_STREAM)).invokeOnFriend();
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                for (Parcelable p : extras.getParcelableArrayList(Intent.EXTRA_STREAM)) {
                    new EnqueueTaskInvoker((Uri)p).invokeOnFriend();
                }
            }
        } else {
            finish();
        }
    }

    private class EnqueueTaskInvoker {
        private Uri mmForUri;
        private List<String> mmOptions = new LinkedList<String>();

        public EnqueueTaskInvoker(Uri forUri) {
            mmForUri = forUri;
            mmOptions.add("(local)");
            mmOptions.add("10.0.0.50");
            mmOptions.add("10.0.0.52");
            mmOptions.add("192.168.1.15");
            mmOptions.add("192.168.1.17");
            mmOptions.add("Other...");
        }

        public void invokeOnFriend() {
            AlertDialog.Builder builder = new AlertDialog.Builder(EnqueueActivity.this);
            builder
                .setTitle(R.string.dialog_title_send_to)
                .setOnCancelListener(new CancelAction())
                .setItems(mmOptions.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String choice = mmOptions.get(which);
                        if (choice != null) {
                            if ("Other...".equals(choice)) {
                                LayoutInflater inflater = getLayoutInflater();
                                View layout = inflater.inflate(
                                    R.layout.friend,
                                    (ViewGroup)findViewById(R.id.root));

                                AlertDialog.Builder builder = new AlertDialog.Builder(EnqueueActivity.this);
                                EditText field = (EditText)layout.findViewById(R.id.name);
                                builder
                                    .setTitle(R.string.dialog_title_send_to)
                                    .setView(layout)
                                    .setOnCancelListener(new CancelAction())
                                    .setNegativeButton(android.R.string.cancel, new CancelAction())
                                    .setPositiveButton(android.R.string.ok, new ConfirmAction(field));

                                dialog.dismiss();
                                builder.create().show();
                            } else if ("(local)".equals(choice)) {
                                dialog.dismiss();
                                new EnqueueToFriendTask("localhost:8080", mmForUri).execute();
                                finish();
                            } else {
                                dialog.dismiss();
                                new EnqueueToFriendTask(String.format("%s:8080", choice), mmForUri).execute();
                                finish();
                            }
                        } else {
                            dialog.dismiss();
                        }
                    }
                });

            builder.create().show();
        }

        private class CancelAction implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finish();
            }
        }

        private class ConfirmAction implements DialogInterface.OnClickListener {
            private EditText mmmField;

            public ConfirmAction(EditText field) {
                mmmField = field;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = mmmField.getText().toString();
                dialog.dismiss();
                new EnqueueToFriendTask(name, mmForUri).execute();
                finish();
            }
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
