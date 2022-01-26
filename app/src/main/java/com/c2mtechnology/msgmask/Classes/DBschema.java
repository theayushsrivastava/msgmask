package com.c2mtechnology.msgmask.Classes;

public class DBschema {
    public static String DB = "spam.db";

    //TABLE NAMES
    public static String TABLE_MESSAGES = "messages";
    public static String TABLE_ADDRESS_STATS = "address_stats";

    //Common attributes
    public static String KEY_ID = "id";
    public static String KEY_ADDRESS = "address";


    //message table attribute names
    public static String KEY_TIMESTAMP = "timestamp";           //timestamp of a message
    public static String KEY_IS_SPAM = "isSpam";                //contains 0 or 1
    public static String KEY_IS_OVERRIDDEN = "isOverridden";    //default is 0 and if isSpam is 1 then it can be 1
    public static String KEY_MALICIOUS_LINK_FOUND = "malicious_links_found";
    public static String KEY_MALICIOUS_LINKS = "malicious_links";

    //addresses table attribute names
    //this table contains the statistics for the addresses in the users phone

    public static String KEY_SPAM_MSG_COUNTS = "spam_msg_count";            //this is the global spam message counts for a given address
    public static String KEY_HAM_MSG_COUNTS = "ham_msg_counts";             //this is the global ham msg count for a address
    public static String KEY_REPORTED_COUNTS = "reported_counts";           //no. of times the address is reported
    public static String KEY_IS_REPORTED    = "is_reported";                // 0 or 1  0 => not reported  1=> reported by the user
    public static String KEY_IS_SPAMMER     = "is_spammer";                 // 0 or 1, 0=>not spammer and 1=> spammer


    //Create table messages
    public static String CREATE_TABLE_MESSAGES = "CREATE TABLE "
            + TABLE_MESSAGES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_ADDRESS
            + " TEXT," + KEY_TIMESTAMP + " LONG," + KEY_IS_SPAM + " INTEGER," + KEY_IS_OVERRIDDEN
            + " INTEGER," + KEY_MALICIOUS_LINK_FOUND + " INTEGER," + KEY_MALICIOUS_LINKS + " TEXT" +" )";


    //CREATE TABLE addresses

    public static String CREATE_TABLE_ADDRESSES = "CREATE TABLE "
            +TABLE_ADDRESS_STATS  + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_ADDRESS
            + " TEXT UNIQUE," + KEY_SPAM_MSG_COUNTS + " INTEGER," + KEY_HAM_MSG_COUNTS
            + " INTEGER," + KEY_REPORTED_COUNTS + " INTEGER," + KEY_IS_SPAMMER + " INTEGER," + KEY_IS_REPORTED
            + " INTEGER," + KEY_IS_OVERRIDDEN + " INTEGER" + " )";

}
