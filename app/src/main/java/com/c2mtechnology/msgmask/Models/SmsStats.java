package com.c2mtechnology.msgmask.Models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class SmsStats {
    public String address,malicious_links;
    public long timestamp;
    public int isSpam,maliciousLinksCount;
    public HashMap<String,Boolean> links_result;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMalicious_links() {
        return malicious_links;
    }

    public void setMalicious_links(String malicious_links) {
        this.malicious_links = malicious_links;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getIsSpam() {
        return isSpam;
    }

    public void setIsSpam(int isSpam) {
        this.isSpam = isSpam;
    }

    public int getMaliciousLinksCount() {
        return maliciousLinksCount;
    }

    public void setMaliciousLinksCount(int maliciousLinksCount) {
        this.maliciousLinksCount = maliciousLinksCount;
    }

    public HashMap<String,Boolean> getMaliciousLinks()
    {
        if(links_result != null){
            return links_result;
        }

        if(malicious_links == null)
        {
            return null;
        }else{
            try {
                JSONObject links = new JSONObject(malicious_links);
                Iterator<String> keys = links.keys();

                links_result = new HashMap<>();
                while(keys.hasNext()) {
                    String key = keys.next();
                    if (links.get(key) instanceof String) {
                        // do something with jsonObject here
                        String urlSafe = links.getString(key);
//                    Log.i("url safe " , urlSafe);
                        if(urlSafe.equals("bad"))
                        {
                            links_result.put(key,false);

                        }else{
                            links_result.put(key,true);
                        }
                    }
                }
                return links_result;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
