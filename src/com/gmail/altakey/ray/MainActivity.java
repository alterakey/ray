package com.gmail.altakey.ray;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import android.util.Log;
import android.os.Environment;
import android.widget.Toast;
import android.content.res.AssetFileDescriptor;
import android.content.Intent;
import android.net.Uri;
import java.net.URI;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Parcelable;

public class MainActivity extends Activity
{
    private ListAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mAdapter = new MockAdapter();

        Intent intent = getIntent();

        startPlaybackService();

        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            loadCurrentPlaylist();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())
                   || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            new Enqueuer(intent).queue();
            Toast.makeText(this, "Queued in playlist.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startPlaybackService() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        startService(intent);
    }

    private void stopPlaybackService() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        stopService(intent);
    }

    private void loadCurrentPlaylist() {
        ListView lv = (ListView)findViewById(R.id.view);
        lv.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.main, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        switch (mi.getItemId()) {
        case R.id.menu_main_config:
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_main_quit:
            stopPlaybackService();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(mi);
    }

    private class MockAdapter extends ArrayAdapter<String> {
        public MockAdapter() {
            super(MainActivity.this, android.R.layout.simple_list_item_1, new String[] {
                    "file 1", "file 2", "file 3"
                });
        }
    }

    private class Enqueuer {
        private Intent mmIntent;

        public Enqueuer(Intent intent) {
            mmIntent = intent;
        }

        public void queue() {
            final String action = mmIntent.getAction();
            final Bundle extras = mmIntent.getExtras();

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
        }

        private void enqueueToService(Uri uri) {
            Intent intent = new Intent(MainActivity.this, PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_ENQUEUE);
            intent.setData(uri);
            startService(intent);
        }
    }
}
