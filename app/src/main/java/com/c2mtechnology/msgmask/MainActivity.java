package com.c2mtechnology.msgmask;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.c2mtechnology.msgmask.Adapters.AllMsgAdapter;
import com.c2mtechnology.msgmask.Callbacks.SMSCallback;
import com.c2mtechnology.msgmask.Classes.DataBaseHelper;
import com.c2mtechnology.msgmask.Classes.URLS;
import com.c2mtechnology.msgmask.Classes.VolleySingleton;
import com.c2mtechnology.msgmask.Models.AddressModel;
import com.c2mtechnology.msgmask.Models.Sms;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;

public class MainActivity extends AppCompatActivity implements SMSCallback {

    ActivityResultLauncher<Intent> msgPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            final String myPackageName = getPackageName();
                            if (Telephony.Sms.getDefaultSmsPackage(MainActivity.this).equals(myPackageName)) {
                                 getAllSms(MainActivity.this);
                            }
                        }
                    }
                }
            });

    ArrayList<Sms> allSms;
    HashSet<String> addresses;
    RecyclerView recyclerView;
    AllMsgAdapter allMsgAdapter;
    StringRequest request;
    String address = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allSms = new ArrayList<>();
        addresses = new HashSet<>();
        int[] smsStats = DataBaseHelper.getInstance(this).getAllSmsStats();
        requestSmsPermission();

        TextView progress = findViewById(R.id.progress_tv);
        recyclerView        = findViewById(R.id.rv);
        CircularProgressIndicator circularProgress = findViewById(R.id.circular_progress);

        circularProgress.setMaxProgress(100);
        int prog = 0;
        if(smsStats[0] == 0)
        {
            prog = 100;
        }else if(smsStats[1] == 0)
        {
            prog = 0;
        }else{
            prog = (smsStats[1] * 100)/smsStats[0] ;
            TextView progress_title = findViewById(R.id.textView7);
            progress_title.setText("Analysis - " + smsStats[1] + "/" + smsStats[0]);
            progress_title.setVisibility(View.VISIBLE);
        }

        Log.i("total msg : ", + smsStats[0] + " ham : " + smsStats[1]);

        circularProgress.setCurrentProgress(prog);

        circularProgress.setProgressTextAdapter(new CircularProgressIndicator.ProgressTextAdapter() {
            @Override
            public String formatText(double currentProgress) {
                progress.setText((int)currentProgress  + "%");
                return "";
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.RECEIVE_SMS") == PackageManager.PERMISSION_GRANTED)
            {
//                startService(new Intent(this, Communicator.class));
                getAllSms(this);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.RECEIVE_SMS"},129);
                getAllSms(this);
            }

        }else{
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS","android.permission.RECEIVE_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

        findViewById(R.id.imageView3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        findViewById(R.id.textView3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCheckSpamBottomSheet();
            }
        });

        findViewById(R.id.all_spam_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,SpamActivity.class));
            }
        });

    }

    private void requestSmsPermission() {
        String permission = Manifest.permission.RECEIVE_SMS;
        int grant = ContextCompat.checkSelfPermission(this, permission);
        if ( grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(this, permission_list, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        getAllSms(MainActivity.this);
    }

    private void showCheckSpamBottomSheet()
    {

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this,R.style.BottomSheetDialogTheme);
        View v = getLayoutInflater().inflate(R.layout.bs_check_spam,null);

        ImageButton search_ib   = v.findViewById(R.id.query_button);
        EditText query_et       = v.findViewById(R.id.query_et);

        if(request != null){
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
                            DataBaseHelper.getInstance(MainActivity.this).add_update_address_stats(address_stats,null,null);
                            AddressModel addressModel = DataBaseHelper.getInstance(MainActivity.this).getAddressStats(address);
                            initGetReportView(v,addressModel);
                        }else{
                            Toast.makeText(MainActivity.this,"Address stats not found",Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(MainActivity.this,"An error occured",Toast.LENGTH_SHORT).show();
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

        search_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search_ib.setEnabled(false);
                address = query_et.getText().toString();
                if(address.equals(""))
                {
                    search_ib.setEnabled(true);
                    Toast.makeText(MainActivity.this,"Enter phone to continue",Toast.LENGTH_SHORT).show();
                }else{
                    //
                    ProgressBar progressBar = v.findViewById(R.id.progressBar2);
                    progressBar.setVisibility(View.VISIBLE);
                    VolleySingleton.getInstance(MainActivity.this).addToRequestQueue(request);
                }
            }
        });
        bottomSheetDialog.setContentView(v);
        bottomSheetDialog.show();
    }

    private void initGetReportView(View v, AddressModel addressModel)
    {
        ProgressBar progressBar = v.findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.GONE);
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
    private void openSettings()
    {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this,R.style.BottomSheetDialogTheme);
        View v = getLayoutInflater().inflate(R.layout.bs_settings,null);
        bottomSheetDialog.setContentView(v);
        bottomSheetDialog.show();
    }


    public void getAllSms(Context context) {

        ContentResolver cr = context.getContentResolver();
        String SORT_ORDER = "date DESC";
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, SORT_ORDER);
        int totalSMS = 0;
        if (c != null) {
            totalSMS = c.getCount();
            if (c.moveToFirst()) {
                allSms.clear();
                for (int j = 0; j < totalSMS; j++) {
                    long smsDate = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));

                    String type;
                    switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                        case Telephony.Sms.MESSAGE_TYPE_INBOX:
                            type = "inbox";
                            dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_SENT:
                            type = "sent";
                            dateSent = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                            type = "outbox";
                            break;
                        default:
                            break;
                    }
                    try{
                        if(!addresses.contains(number))
                        {
                            Sms sms = new Sms();
                            sms.setAddress(number);
                            sms.setMsg(body);
                            sms.set_threadId( c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)));
                            sms.setTime(dateSent);
                            addresses.add(number);
                            allSms.add(sms);
                        }
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
        allMsgAdapter = new AllMsgAdapter(allSms,this);
        recyclerView.setAdapter(allMsgAdapter);
        Log.i("count : ", allSms.get(0).getTime()+ "");
    }

    @Override
    public void messageReceived(String message) {
        Toast.makeText(this, "New Message Received: " + message, Toast.LENGTH_SHORT).show();
    }
}