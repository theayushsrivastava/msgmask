package com.c2mtechnology.msgmask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.c2mtechnology.msgmask.Adapters.AllMsgAdapter;
import com.c2mtechnology.msgmask.Adapters.ChatAdapter;
import com.c2mtechnology.msgmask.Classes.DataBaseHelper;
import com.c2mtechnology.msgmask.Classes.URLS;
import com.c2mtechnology.msgmask.Classes.Utility;
import com.c2mtechnology.msgmask.Classes.VolleySingleton;
import com.c2mtechnology.msgmask.Models.AddressModel;
import com.c2mtechnology.msgmask.Models.Sms;
import com.c2mtechnology.msgmask.Models.SmsStats;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.ShowSmsCheckDialog {

    Long threadID;
    String address;

    ArrayList<Sms> smsArrayList;
    ChatAdapter chatAdapter;
    RecyclerView recyclerView;
    AddressModel addressModel;
    AlertDialog alertDialog = null;

    StringRequest request;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        address  = getIntent().getStringExtra("address");
        threadID = getIntent().getLongExtra("threadID",-1);

        initAddressModel();
        smsArrayList = new ArrayList<>();
        recyclerView    = findViewById(R.id.rv);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);

        if(threadID == -1)
        {
            Toast.makeText(this,"Thread not found",Toast.LENGTH_SHORT).show();
            finish();
        }else{
//            Toast.makeText(this,"Thread : " + threadID, Toast.LENGTH_SHORT).show();
        }
        getAllSms(this);

        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        TextView address_tv = findViewById(R.id.address_tv);
        address_tv.setText(address);
        initMenu();
    }

    public void initAddressModel()
    {
        addressModel = DataBaseHelper.getInstance(this).getAddressStats(address);
        if(addressModel != null)
        {
            if(addressModel.getIsReported() == 1)
            {
                findViewById(R.id.scam_cv).setVisibility(View.VISIBLE);
                findViewById(R.id.suspected_scam_cv1).setVisibility(View.GONE);
                return;

            }else{
                findViewById(R.id.scam_cv).setVisibility(View.GONE);
            }

            if(addressModel.getIs_spammer() == 1 && addressModel.getIsOverridden() == 0)
            {
                findViewById(R.id.suspected_scam_cv1).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.suspected_scam_cv1).setVisibility(View.GONE);
            }
        }
    }


    public void buttonOperations(View v)
    {
        String tag = v.getTag().toString();
        switch (tag){
            case "close1":
                findViewById(R.id.suspected_scam_cv1).setVisibility(View.GONE);
                break;
            case "close2":
                findViewById(R.id.scam_cv).setVisibility(View.GONE);
                break;
            case "report_not_spam":
                reportNotSpam(1);
                break;
            case "report_spam":
                reportSpam();
                break;
            case "not_spam":
                reportNotSpam(1);
                break;
        }
    }

    private void reportSpam()
    {
        //show alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report spam ?")
                .setMessage("The sender's number and recent text will go to admin and may go to your operator. This conversation will stay in 'Spam', without notifications")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Report spam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.dismiss();
                       sendReportSpamRequest();
                    }
                });

        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    public void reportNotSpam(int is_overriden)
    {
        //show alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report not spam ?")
                .setMessage("The sender's number will be reported as not spam and will be visible in your messages")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Report Not Spam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.dismiss();
                        sendReportNotSpamRequest(is_overriden);
                    }
                });

        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void sendReportNotSpamRequest(int is_overriden)
    {
        StringRequest request = new StringRequest(Request.Method.POST, URLS.URL_REPORT_NOT_SPAM, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("res",response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.getInt("status") == 1)
                    {
                        if(jsonObject.has("address_stats"))
                        {
                            JSONObject address_stats = jsonObject.getJSONObject("address_stats");
                            DataBaseHelper.getInstance(ChatActivity.this).add_update_address_stats(address_stats,0,is_overriden);
                            initAddressModel();
                            Toast.makeText(ChatActivity.this,"Reported as not spam !",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(ChatActivity.this,"Address stats not found",Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(ChatActivity.this,"An error occured",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params = new HashMap<>();
                params.put("address",address);
                return params;
            }
        };

        VolleySingleton.getInstance(ChatActivity.this).addToRequestQueue(request);
    }

    private void sendReportSpamRequest()
    {
        StringRequest request = new StringRequest(Request.Method.POST, URLS.URL_REPORT_SPAM, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("res",response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.getInt("status") == 1)
                    {
                        if(jsonObject.has("address_stats"))
                        {
                            JSONObject address_stats = jsonObject.getJSONObject("address_stats");
                            DataBaseHelper.getInstance(ChatActivity.this).add_update_address_stats(address_stats,1,0);
                            initAddressModel();
                        }else{
                            Toast.makeText(ChatActivity.this,"Address stats not found",Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(ChatActivity.this,"An error occured",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params = new HashMap<>();
                params.put("address",address);
                String msg = "";
                for(int i = 0 ; i  < smsArrayList.size() ; i++)
                {
                    if(smsArrayList.get(i).getMessageType() == Telephony.Sms.MESSAGE_TYPE_INBOX)
                    {
                        msg = smsArrayList.get(i).getMsg();
                    }
                }
                params.put("message",msg);
                return params;
            }
        };

        VolleySingleton.getInstance(ChatActivity.this).addToRequestQueue(request);
    }

    private void initMenu()
    {
        findViewById(R.id.imageButton3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initializing the popup menu and giving the reference as current context
                PopupMenu popupMenu = new PopupMenu(ChatActivity.this,view);

                // Inflating popup menu from popup_menu.xml file

                popupMenu.getMenuInflater().inflate(R.menu.menu1, popupMenu.getMenu());
                Menu menu = popupMenu.getMenu();
                if(addressModel != null && addressModel.getIsReported() == 1)
                {
                    menu.getItem(0).setVisible(false);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        // Toast message on menu item clicked
                        String title = menuItem.getTitle().toString();
                        if(title.equals("Report User"))
                        {
                            reportSpam();
                            return true;
                        }else if(title.equals("Get Report"))
                        {
                            //get report
                            getReport();
                            return true;
                        }
                        return true;
                    }
                });
                // Showing the popup menu
                popupMenu.show();
            }
        });
    }

    private void getReport()
    {

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this,R.style.BottomSheetDialogTheme);
        View v = getLayoutInflater().inflate(R.layout.bs_check_spam,null);

        EditText address_tv = v.findViewById(R.id.query_et);
        address_tv.setText(address);
        address_tv.setEnabled(false);
        TextView title_tv = v.findViewById(R.id.textView4);
        ImageButton close_button = v.findViewById(R.id.imageButton5);
        title_tv.setText("Generating User Report");

        if(request != null)
        {
            request.cancel();
        }

        request = new StringRequest(Request.Method.POST, URLS.URL_GET_REPORT, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("res",response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.getInt("status") == 1)
                    {
                        if(jsonObject.has("address_stats"))
                        {
                            JSONObject address_stats = jsonObject.getJSONObject("address_stats");
                            DataBaseHelper.getInstance(ChatActivity.this).add_update_address_stats(address_stats,null,null);
                            initAddressModel();
                            initGetReportView(v);
                        }else{
                            Toast.makeText(ChatActivity.this,"Address stats not found",Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(ChatActivity.this,"An error occured",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params = new HashMap<>();
                params.put("address",address);
                return params;
            }
        };

        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    request.cancel();
                }catch (Exception e){

                }
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.setContentView(v);
        bottomSheetDialog.show();
        VolleySingleton.getInstance(this).addToRequestQueue(request);

        Utility.hideKeyboard(this);
    }

    public void initGetReportView(View v)
    {

        ProgressBar progressBar = v.findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);
        try {
            v.findViewById(R.id.safe_rl).setVisibility(View.VISIBLE);
            CardView cardView       = v.findViewById(R.id.cv5);
            TextView title_tv       = v.findViewById(R.id.textView74);
            TextView loader_title   = v.findViewById(R.id.textView5);
            ImageView imageView     = v.findViewById(R.id.imageView75);
            CircularProgressIndicator cpi = v.findViewById(R.id.circular_progress2);

            cpi.setProgressTextAdapter(new CircularProgressIndicator.ProgressTextAdapter() {
                @NonNull
                @NotNull
                @Override
                public String formatText(double currentProgress) {
                    return (int)currentProgress + "%";
                }
            });

            int ham = addressModel.getHamCount();
            int spam = addressModel.getSpamCount();

            if(addressModel.getIs_spammer() == 1)
            {
                if(spam == 0){
                    cpi.setProgress(0,100);
                }else{
                    int progress = (spam * 100)/(spam+ham);
                    cpi.setProgress((double) progress,100);
                }
                loader_title.setText("Spam Messages Sent");
                cpi.setProgressColor(getResources().getColor(R.color.fail));

                title_tv.setText(addressModel.getReportCount() + " USERS REPORTED AS SPAM !");
                cardView.setCardBackgroundColor(getResources().getColor(R.color.card_fail));
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_shield));
            }else{

                if(ham == 0){
                    cpi.setProgress(100,100);
                }else{
                    int progress = (ham*100)/(spam+ham);
                    cpi.setProgress((double) progress,100);
                }
                cpi.setProgressColor(getResources().getColor(R.color.success));
                loader_title.setText("Safe message sent");
                title_tv.setText("LOOKS SAFE!\n" + addressModel.getReportCount() + " SPAM REPORTED");
                cardView.setCardBackgroundColor(getResources().getColor(R.color.card_success));
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.shield_green));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getAllSms(Context context) {
        ContentResolver cr = context.getContentResolver();
        String SORT_ORDER = "date DESC";
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, Telephony.Sms.THREAD_ID + " = ?",new String[]{String.valueOf(threadID)}, SORT_ORDER);
        int totalSMS = 0;
        if (c != null) {
            totalSMS = c.getCount();
            if (c.moveToFirst()) {
                for (int j = 0; j < totalSMS; j++) {
                    long smsDate = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));

                    int type = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)));

                    switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                        case Telephony.Sms.MESSAGE_TYPE_INBOX:
                            type = Telephony.Sms.MESSAGE_TYPE_INBOX;
                            dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_SENT:
                            type = Telephony.Sms.MESSAGE_TYPE_SENT;
                            dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                            type = Telephony.Sms.MESSAGE_TYPE_OUTBOX;
                            break;
                        default:
                            break;
                    }
                    try{
                        Sms sms = new Sms();
                        sms.setAddress(number);
                        sms.setMsg(body);
                        sms.setMessageType(type);
                        sms.set_threadId( c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)));
                        sms.setTime(dateSent);
//                            addresses.add(number);
                        smsArrayList.add(sms);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
//                    Log.i("msg from : ", number + " ->> smg : " + body);
                    c.moveToNext();
                }
            }
            Log.i("totalMsg",totalSMS + "");
            c.close();

        } else {
            Toast.makeText(this, "No message to show!", Toast.LENGTH_SHORT).show();
        }
        chatAdapter = new ChatAdapter(smsArrayList,this,this);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(0);
        Log.i("count : ", smsArrayList.get(0).getTime()+ "");
    }

    @Override
    public void showSmsCheckerDialog(Sms sms) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this,R.style.BottomSheetDialogTheme);
        View v = getLayoutInflater().inflate(R.layout.bs_check_msg,null);

        bottomSheetDialog.setCanceledOnTouchOutside(false);
        bottomSheetDialog.setCancelable(false);

        ProgressBar loader1     = v.findViewById(R.id.progressBar);
        ProgressBar loader2     = v.findViewById(R.id.progressBar3);
        ImageButton close_ib    = v.findViewById(R.id.imageButton2);
        TextView msg_tv         = v.findViewById(R.id.textView11);

        msg_tv.setText(sms.getMsg());
        ArrayList<String> linksFound = sms.getLinks();
        Log.i("links",linksFound.toString());


        if(request != null)
        {
            request.cancel();
        }
        SmsStats smsStats = DataBaseHelper.getInstance(ChatActivity.this).getMessage(sms.getAddress(),sms.getTime());
        if(smsStats != null)
        {
            loader1.setVisibility(View.INVISIBLE);
            loader2.setVisibility(View.INVISIBLE);
            initMessageBottomSheet(v,smsStats,linksFound);
        }else{
            request = new StringRequest(Request.Method.POST, URLS.URL_CHECK_SPAM, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("res",response);
                    loader1.setVisibility(View.INVISIBLE);
                    loader2.setVisibility(View.INVISIBLE);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response);
                        if(jsonObject.getInt("status") == 1)
                        {
                            DataBaseHelper.getInstance(getApplicationContext()).addMessageStats(jsonObject,sms.getTime());
                            SmsStats smsStats = DataBaseHelper.getInstance(ChatActivity.this).getMessage(sms.getAddress(),sms.getTime());
                            if(smsStats != null)
                            {
                                //error occurred
                                initMessageBottomSheet(v,smsStats,linksFound);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Nullable
                @org.jetbrains.annotations.Nullable
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String,String> params = new HashMap<>();
                    params.put("message",sms.getMsg());
                    params.put("address",sms.getAddress());
                    Gson gson = new Gson();
                    params.put("links",gson.toJson(linksFound));
                    Log.i("params",params.toString());
                    return params;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);
        }



        close_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
                try{
                    request.cancel();
                }catch (Exception e) {

                }
            }
        });

        bottomSheetDialog.setContentView(v);
        bottomSheetDialog.show();
    }

    private void initMessageBottomSheet(View v,SmsStats smsStats,ArrayList<String> links)
    {
        TextView title_tv       = v.findViewById(R.id.textView6);
        TextView is_spam_tv     = v.findViewById(R.id.is_spam_tv);
        TextView malicious_tv   = v.findViewById(R.id.textView14);
        TextView title_2        = v.findViewById(R.id.textView12);
        TextView title_3        = v.findViewById(R.id.textView123);
        title_2.setText("Message Content");
        title_3.setText("Malicious URL");

        title_tv.setText("Message Report");
        title_tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_report_file,0,0,0);
        if(smsStats.getIsSpam() == 1)
        {
            is_spam_tv.setText("Suspected Spam");
            is_spam_tv.setTextColor(getResources().getColor(R.color.fail));
            is_spam_tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_shield_unsafe,0,0,0);
        }

        is_spam_tv.setVisibility(View.VISIBLE);
        malicious_tv.setText(links.size() + "");

        HashMap<String,Boolean> links_result = smsStats.getMaliciousLinks();
        if(links_result != null && links_result.size() != 0)
        {
            LinearLayout  ll  = (LinearLayout) v.findViewById(R.id.ll);
            for (Map.Entry<String, Boolean> set :
                    links_result.entrySet()) {

                // Printing all elements of a Map
                TextView textView = new TextView(ChatActivity.this);
                textView.setText(set.getKey());
                textView.setTextColor(getResources().getColor(R.color.link_color_dark));
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                textView.setGravity(View.TEXT_ALIGNMENT_TEXT_START);
                textView.setCompoundDrawablePadding(10);
                if(set.getValue())
                {
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check,0,0,0);
                }else{
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_virus,0,0,0);
                }

                ((LinearLayout)ll).addView(textView);
            }
        }
    }
}