package com.gmail.altakey.ray;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import java.nio.*;
import android.util.Log;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
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

        loadCurrentPlaylist();
        startPlaybackService();
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
}
