package com.gmail.altakey.ray;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.net.Uri;
import android.content.Intent;
import android.widget.Toast;

public class EnqueueActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String action = getIntent().getAction();
        final Bundle extras = getIntent().getExtras();

        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                enqueueToService((Uri)extras.getParcelable(Intent.EXTRA_STREAM));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                for (Parcelable p : extras.getParcelableArrayList(Intent.EXTRA_STREAM)) {
                    enqueueToService((Uri)p);
                }
            }
        }
        finish();
    }

    private void enqueueToService(Uri uri) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_ENQUEUE);
        intent.setData(uri);
        startService(intent);
    }
}
