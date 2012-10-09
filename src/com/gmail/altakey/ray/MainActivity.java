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

        startServices();
    }

    private void startServices() {
        Intent intent = new Intent(MainActivity.this, PlaybackService.class);
        startService(intent);

        intent = new Intent(MainActivity.this, AcceptService.class);
        startService(intent);
    }

    private void stopServices() {
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
            stopServices();
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
