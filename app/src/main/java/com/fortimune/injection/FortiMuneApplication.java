package com.fortimune.injection;

import android.app.Application;
import com.onesignal.OneSignal;

/** Initializes push for foreground, background and notification cold starts. */
public class FortiMuneApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Public application identifier; API keys and Firebase private keys stay server-side.
        OneSignal.initWithContext(this, "027bf229-1372-4209-9ba5-e86ea84ebb74");
    }
}
