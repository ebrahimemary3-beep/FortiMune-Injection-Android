package com.fortimune.injection;

import android.app.Application;
import org.json.JSONObject;
import com.onesignal.OneSignal;

public class FortiMuneApplication extends Application {
    // Held in memory; never log or persist a device possession challenge.
    private static JSONObject proof;
    private static JSONObject pendingOpen;
    static synchronized void captureProof(JSONObject data) { proof=data; }
    static synchronized void clearProof() { proof=null; }
    static synchronized JSONObject getProof() { return proof; }
    static synchronized JSONObject takeOpen() { JSONObject p=pendingOpen; pendingOpen=null; return p; }
    @Override public void onCreate() {
        super.onCreate();
        OneSignal.initWithContext(this,"027bf229-1372-4209-9ba5-e86ea84ebb74");
        OneSignal.getNotifications().addForegroundLifecycleListener(event -> {
            JSONObject data=event.getNotification().getAdditionalData();
            if(data!=null && data.optBoolean("nativeChallenge")) {
                event.preventDefault();
                synchronized(FortiMuneApplication.class) { proof=data; }
            }
        });
        OneSignal.getNotifications().addClickListener(event -> {
            JSONObject data=event.getNotification().getAdditionalData();
            if(data==null || data.optBoolean("nativeChallenge")) return;
            synchronized(FortiMuneApplication.class) { pendingOpen=data; }
        });
    }
}
