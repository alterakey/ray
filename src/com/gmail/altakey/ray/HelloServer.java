package com.gmail.altakey.ray;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class HelloServer extends NanoHTTPD
{
    private Context mContext;

	public HelloServer(Context context, int port, File root) throws IOException
	{
		super(port, root);
        mContext = context;
	}

	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
        String msg = "";
		if ( files.getProperty("stream") == null ) {
			msg =
                "<html>" +
                "<head><script src='//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js'></script></head>" +
                "<body><h1>Air Intention</h1>" +
				"<form action='.' enctype='multipart/form-data' method='post'>" +
				"<p>Enqueue stream: <input type='file' name='stream'></p>" +
				"<p><input type='submit' name='Go'></p>\n" +
				"</form>" +
                "</body></html>";
        } else {
            msg = "<html><body>Queued in playlist.</body></html>";
            new Enqueuer(files.getProperty("stream")).enqueue();
        }
		return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, msg );
	}

    private class Enqueuer {
        private String mmPath;

        public Enqueuer(String path) {
            mmPath = path;
        }

        public void enqueue() {
            seize();

            Intent intent = new Intent(mContext, PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_ENQUEUE);
            intent.setData(Uri.parse(String.format("file://%s", mmPath)));
            mContext.startService(intent);
        }

        private void seize() {
            FileChannel src = null;
            FileChannel dest = null;

            try {
                File srcFile = new File(mmPath);
                File destFile = new File(mContext.getExternalFilesDir(null), srcFile.getName());
                src = new FileInputStream(srcFile).getChannel();
                dest = new FileOutputStream(destFile).getChannel();

                dest.transferFrom(src, 0, src.size());

                mmPath = destFile.getAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (src != null) {
                        src.close();
                    }
                } catch (IOException e) {
                }
                try {
                    if (dest != null) {
                        dest.close();
                    }
                } catch (IOException e) {
                }
            }
        }
    }
}
