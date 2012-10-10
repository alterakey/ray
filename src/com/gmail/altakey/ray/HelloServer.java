package com.gmail.altakey.ray;

import java.io.*;
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
		System.out.println( method + " '" + uri + "' " );
		String msg = "<html><body><h1>Air Intention</h1>\n";
		if ( files.getProperty("stream") == null ) {
			msg +=
				"<form action='.' enctype='multipart/form-data' method='post'>\n" +
				"  <p>Enqueue stream: <input type='file' name='stream'></p>\n" +
				"  <p><input type='submit' name='Go'></p>\n" +
				"</form>\n";
        } else {
			msg += "<p>Queued in playlist.</p>";
            new Enqueuer().enqueue(files.getProperty("stream"));
        }

		msg += "</body></html>\n";
		return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, msg );
	}

    private class Enqueuer {
        public void enqueue(String path) {
            Intent intent = new Intent(mContext, PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_ENQUEUE);
            intent.setData(Uri.parse(String.format("file://%s", path)));
            mContext.startService(intent);
        }
    }
}
