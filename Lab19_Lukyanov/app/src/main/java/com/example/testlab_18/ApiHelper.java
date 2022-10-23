package com.example.testlab_18;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiHelper {
    public String base = "http://89.108.78.244:5000";
    Activity ctx;
    public String sessionKey = "null";
    public boolean isEnd;

    public ApiHelper(Activity ctx)
    {
        this.ctx = ctx;
        if (g.graph.getEndPoint() != null)
        base = g.graph.getEndPoint();
    }

    public void on_ready(String res) throws Exception {
        /*
        if (res.equals("null"))
            sessionKey = res;
        else {
            sessionKey = res.replace("\"", "");
        }

         */
    }

    public void on_fail()
    {

    }

    public int send(Activity ctx, String method, String request)
    {
        isEnd = false;
        final int[] code = new int[1];
        Runnable r = new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    URL url = new URL(base + request);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    con.setRequestMethod(method);

                    code[0] = con.getResponseCode();
                    InputStream is = con.getInputStream();
                    BufferedInputStream inp = new BufferedInputStream(is);


                    byte[] buf = new byte[1024];
                    String str = "";

                    while (true)
                    {
                        int len = inp.read(buf);
                        if (len < 0) break;
                        str += new String(buf, 0, len);
                    }

                    con.disconnect();

                    final String res = str;
                    ctx.runOnUiThread(() ->
                    {
                        try {on_ready(res); }
                        catch (Exception e) {e.printStackTrace();}
                    });
                }
                catch (Exception ex)
                {
                    ctx.runOnUiThread(() ->
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle("ERROR").setMessage("Error: " + ex.getClass().getSimpleName()).setIcon(android.R.drawable.ic_dialog_alert).show();
                    });
                    on_fail();
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isEnd = true;
        return code[0];
    }
}
