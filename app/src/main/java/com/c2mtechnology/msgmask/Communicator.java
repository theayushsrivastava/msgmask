package com.c2mtechnology.msgmask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.c2mtechnology.msgmask.Callbacks.SMSCallback;
import com.c2mtechnology.msgmask.Workers.SpamWorker;

import java.util.List;

public class Communicator extends Service
{

    private final String TAG = this.getClass().getSimpleName();

    private SMSReceiver mSMSreceiver;

    private IntentFilter mIntentFilter;

    @Override
    public void onCreate()
    {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Communicator started");
        //SMS event receiver
        mSMSreceiver = new SMSReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        mIntentFilter.setPriority(2147483647);

        try{
            unregisterReceiver(mSMSreceiver);
        }catch (Exception e){
            e.printStackTrace();
        }

        registerReceiver(mSMSreceiver, mIntentFilter);

        Intent intent1 = new Intent("android.provider.Telephony.SMS_RECEIVED");
        List<ResolveInfo> infos = getPackageManager().queryBroadcastReceivers(intent1, 0);
        for (ResolveInfo info : infos) {
            Log.i(TAG, "Receiver name:" + info.activityInfo.name + "; priority=" + info.priority);
        }
        return START_STICKY;
    }

    //    @Override
//    public void onDestroy()
//    {
//        super.onDestroy();
//
//        // Unregister the SMS receiver
//        unregisterReceiver(mSMSreceiver);
//
//    }



    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private class SMSReceiver extends BroadcastReceiver
    {
        private final String TAG = this.getClass().getSimpleName();
        private  SMSCallback smsCallback;

        @Override
        public void onReceive(Context context, Intent intent)
        {

            Log.i("tag","new message recieved");
            Bundle extras = intent.getExtras();
            String strMessage = "";

            if ( extras != null )
            {
                Object[] smsextras = (Object[]) extras.get( "pdus" );

                for ( int i = 0; i < smsextras.length; i++ )
                {
                    SmsMessage smsmsg = SmsMessage.createFromPdu((byte[])smsextras[i]);

                    String strMsgBody = smsmsg.getMessageBody().toString();
                    String strMsgSrc = smsmsg.getOriginatingAddress();

                    strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;

                    Log.i("new msg : " , strMessage);

                    Data data = new Data.Builder()
                            .putString(SpamWorker.msg, strMsgBody)
                            .putString(SpamWorker.address,strMsgSrc)
                            .build();
                    final OneTimeWorkRequest workRequest =
                            new OneTimeWorkRequest.Builder(SpamWorker.class)
                                    .setInputData(data)
                                    .build();

                    WorkManager.getInstance(getApplicationContext())
                            .enqueue(workRequest);
//                    smsCallback.messageReceived(strMessage);

//                    if (strMsgBody.contains(SyncStateContract.Constants.DELIMITER)) {
//
//                        Intent msgIntent = new Intent(SyncStateContract.Constants.INTENT_INCOMMING_SMS);
//                        msgIntent.putExtra(SyncStateContract.Constants.EXTRA_MESSAGE, strMsgBody);
//                        msgIntent.putExtra(Constants.EXTRA_SENDER, strMsgSrc);
//                        sendBroadcast(msgIntent);
//                        this.abortBroadcast();
//                    }
                }


            }

        }




    }
}