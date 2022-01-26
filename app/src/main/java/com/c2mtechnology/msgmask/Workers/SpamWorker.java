package com.c2mtechnology.msgmask.Workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.c2mtechnology.msgmask.Classes.DataBaseHelper;
import com.c2mtechnology.msgmask.Classes.URLS;
import com.c2mtechnology.msgmask.Classes.VolleySingleton;
import com.c2mtechnology.msgmask.MainActivity;
import com.c2mtechnology.msgmask.R;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.content.Context.NOTIFICATION_SERVICE;

public class SpamWorker extends Worker {
    //this worker is to save data to server of single suppliers
    NotificationManager manager;
    public static final String address = "";
    public static final String msg = "";

    public SpamWorker(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    public ArrayList<String> getLinks(String msg)
    {
        ArrayList<String> links = new ArrayList<>();
        LinkExtractor linkExtractor = LinkExtractor.builder()
                .linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW))
                .build();
        Iterable<LinkSpan> linksfound = linkExtractor.extractLinks(msg);

        Iterator<LinkSpan> iterator = linksfound.iterator();
        while (iterator.hasNext())
        {
            LinkSpan linkSpan = iterator.next();
            String l = msg.substring(linkSpan.getBeginIndex(), linkSpan.getEndIndex());
            links.add(l);
        }

        return links;
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {

        //getting the input data
        String inputMSG = getInputData().getString("msg");
        ArrayList<String> links = getLinks(inputMSG);
        String inputAddress = getInputData().getString("address");
        long timestamp      = getInputData().getLong("timestamp",0);


        showNotification("Checking for spam",inputMSG);
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(Request.Method.POST, URLS.URL_CHECK_SPAM, future,future){
            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params = new HashMap<>();
                params.put("message",inputMSG);
                params.put("address",inputAddress);
                Gson gson = new Gson();
                params.put("links",gson.toJson(links));
                Log.i("params : ",params.toString());
                return params;
            }
        };

        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
        try{
            String res = future.get(60, TimeUnit.SECONDS);
            JSONObject jsonObject = new JSONObject(res);
            manager.cancel(1);
            if(jsonObject.getInt("status") == 1)
            {
                DataBaseHelper.getInstance(getApplicationContext()).addMessageStats(jsonObject,timestamp);
                if(jsonObject.getBoolean("isSpam"))
                {
                    showSuspiciousMessageNotification(inputMSG);
                }
            }
            Log.i("from spam worker",res);

            return  Result.success();

        }catch (Exception e)
        {
            manager.cancel(1);
            Log.i("from supplier worker",e.getMessage());
            return  Result.retry();
        }
    }

    private void showNotification(String task, String desc) {
        manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        String channelId = "task_channel";
        String channelName = "task_name";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(task)
                .setContentText(desc)
                .setSmallIcon(R.mipmap.ic_launcher_logo);

        builder.setProgress(100,20,true);
        manager.notify(1, builder.build());
    }

    private void showSuspiciousMessageNotification(String msg)
    {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        String channelId = "default";
        NotificationCompat.Builder builder = new  NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_exclamation)
                .setContentTitle("Suspicious Incoming Message")
                .setContentText(msg).setAutoCancel(true).setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        manager.notify(0, builder.build());
    }
}
