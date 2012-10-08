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

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button button = (Button)findViewById(R.id.play);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    load();
                    Toast.makeText(MainActivity.this, "copied corpse", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, PlaybackService.class);
                    intent.setAction(PlaybackService.ACTION_ENQUEUE);
                    intent.setData(getCorpseUri());
                    startService(intent);
                    Toast.makeText(MainActivity.this, "playing corpse", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.d("MA", String.format("IOException: %s", e.getMessage()));
                }
            }

            private File getCorpseFile() throws IOException {
                File root = Environment.getExternalStorageDirectory();
                if (root == null) {
                    throw new IOException("cannot open external storage root");
                }
                return new File(root, "corpse.m4a");
            }

            private Uri getCorpseUri() throws IOException {
                File corpse = getCorpseFile();
                return Uri.parse(String.format("file://%s", corpse.getAbsolutePath()));
            }

            private void load() throws IOException {
                // copy stream
                AssetFileDescriptor fd = getAssets().openFd("corpse.m4a");
                FileChannel src = fd.createInputStream().getChannel();
                FileChannel dest = new FileOutputStream(getCorpseFile()).getChannel();

                dest.transferFrom(src, 0, fd.getLength());

                src.close();
                dest.close();
                fd.close();
            }
        });
    }
}
