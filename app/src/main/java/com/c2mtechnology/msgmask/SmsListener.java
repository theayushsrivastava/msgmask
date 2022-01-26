package com.c2mtechnology.msgmask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.c2mtechnology.msgmask.Callbacks.SMSCallback;
import com.c2mtechnology.msgmask.Workers.SpamWorker;

public class SmsListener extends BroadcastReceiver
{
    private final String TAG = this.getClass().getSimpleName();
    private static SMSCallback smsCallback = null;

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
                long timestamp = smsmsg.getTimestampMillis();

                strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;

                Log.i("new msg  12: " , strMessage);
//
                if(strMsgBody != null && !strMessage.equals("") && strMsgSrc != null)
                {
                    Data data = new Data.Builder()
                            .putString("msg", strMsgBody)
                            .putString("address",strMsgSrc)
                            .putLong("timestamp",timestamp)
                            .build();
                    final OneTimeWorkRequest workRequest =
                            new OneTimeWorkRequest.Builder(SpamWorker.class)
                                    .setInputData(data)
                                    .build();

                    WorkManager.getInstance(context)
                            .enqueue(workRequest);
                }

            }
        }
    }






}