package com.c2mtechnology.msgmask.Models;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.text.format.DateUtils;

import com.c2mtechnology.msgmask.Classes.DataBaseHelper;

import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.function.Consumer;

public class Sms{
    private String _id;
    private String _address;
    private String _msg;
    private String _readState; //"0" for have not read sms and "1" for have read sms
    private long _time;
    private String _folderName;
    private Long   _threadId;
    private Integer messageType;
    ArrayList<String> links;

    private int isSpam = -1;
    private int isOverridden = -1;
    boolean isChecked = false;
    boolean data_fetched_from_internal_db = false;

    public String getId(){
        return _id;
    }
    public String getAddress(){
        return _address;
    }
    public String getMsg(){
        return _msg;
    }
    public String getReadState(){
        return _readState;
    }
    public long getTime(){
        return _time;
    }
    public String getFolderName(){
        return _folderName;
    }


    public void setId(String id){
        _id = id;
    }
    public void setAddress(String address){
        _address = address;
    }
    public void setMsg(String msg){
        _msg = msg;
    }
    public void setReadState(String readState){
        _readState = readState;
    }
    public void setTime(long time){
        _time = time;
    }
    public void setFolderName(String folderName){
        _folderName = folderName;
    }

    public Long get_threadId() {
        return _threadId;
    }

    public void set_threadId(Long _threadId) {
        this._threadId = _threadId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getRelativeTime()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        long now = System.currentTimeMillis();
        try {
            CharSequence ago = DateUtils.getRelativeTimeSpanString(this._time, now, DateUtils.MINUTE_IN_MILLIS);
            return ago.toString();
        }catch (Exception e){
            return "";
        }
    }

    public int getIsSpam(Context context)
    {
        if(!isChecked)
        {
            int[] msgStats = DataBaseHelper.getInstance(context).getMessageStats(this._address,this._time);
            isSpam = msgStats[0];
            isOverridden = msgStats[1];
            isChecked = true;
        }

        return isSpam;
    }

    public int getIsOverridden(Context context)
    {
        if(!isChecked)
        {
            int[] msgStats = DataBaseHelper.getInstance(context).getMessageStats(this._address,this._time);
            isSpam = msgStats[0];
            isOverridden = msgStats[1];
            isChecked = true;
        }
        return isOverridden;
    }

    public ArrayList<String> getLinks()
    {
        if(links != null)
        {
            return links;
        }else{
            LinkExtractor linkExtractor = LinkExtractor.builder()
                    .linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW))
                    .build();
            Iterable<LinkSpan> linksfound = linkExtractor.extractLinks(this._msg);

            Iterator<LinkSpan> iterator = linksfound.iterator();
            links = new ArrayList<>();
            while (iterator.hasNext())
            {
                LinkSpan linkSpan = iterator.next();
                String l = this._msg.substring(linkSpan.getBeginIndex(), linkSpan.getEndIndex());
                links.add(l);
            }
        }
        return links;
    }

    public String getLastMessageFromDB(Context context) {

        if(data_fetched_from_internal_db)
        {
            return this._msg;
        }else{
            ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, Telephony.Sms.ADDRESS + " = ?",new String[]{this._address}, Telephony.Sms.DEFAULT_SORT_ORDER);
            if(c.moveToFirst())
            {
                data_fetched_from_internal_db = true;
                this._msg = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                this._time = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                this.messageType    = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                this._threadId      = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));
                switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                    case Telephony.Sms.MESSAGE_TYPE_INBOX:
                        this.messageType = Telephony.Sms.MESSAGE_TYPE_INBOX;
                        this._time = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                        break;
                    case Telephony.Sms.MESSAGE_TYPE_SENT:
                        this.messageType = Telephony.Sms.MESSAGE_TYPE_SENT;
                        this._time = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                        break;
                    case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                        this.messageType = Telephony.Sms.MESSAGE_TYPE_OUTBOX;
                        this._time = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                        break;
                    default:
                        break;
                }
            }
        }
        return this._msg;
    }
}