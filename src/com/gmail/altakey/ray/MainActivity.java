package com.gmail.altakey.ray;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;
import java.util.LinkedList;
import android.net.Uri;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ListView;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.ListView;

public class MainActivity extends Activity
{
    public static final String ACTION_UPDATE = "com.gmail.altakey.ray.MainActivity.actions.UPDATE";

    private PlaylistAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mAdapter = new PlaylistAdapter(this);
        loadCurrentPlaylist();

        startServices();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mAdapter.listener, new IntentFilter(PlaybackService.ACTION_UPDATE_QUEUE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAdapter.listener);
    }

    private void startServices() {
        Intent intent = new Intent(MainActivity.this, AcceptService.class);
        startService(intent);
    }

    private void stopServices() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        stopService(intent);

        intent = new Intent(MainActivity.this, AcceptService.class);
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
            stopServices();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(mi);
    }

    private static class PlaylistAdapter extends ArrayAdapter<String> {
        public final PlaylistUpdateListener listener = new PlaylistUpdateListener();

        public PlaylistAdapter(Activity a) {
            super(a, android.R.layout.simple_list_item_1, new LinkedList<String>());
        }

        public void refresh() {
            clear();
            for (Uri uri : PlaybackService.getPlaylist()) {
                add(uri.toString());
            }
            notifyDataSetChanged();
        }

        private class PlaylistUpdateListener extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                refresh();
            }
        }
    }
}
