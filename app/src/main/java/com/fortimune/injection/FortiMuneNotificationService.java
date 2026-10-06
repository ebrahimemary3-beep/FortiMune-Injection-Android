package com.fortimune.injection;
import androidx.annotation.Keep;
import com.onesignal.notifications.INotificationReceivedEvent;
import com.onesignal.notifications.INotificationServiceExtension;
import org.json.JSONObject;
@Keep public class FortiMuneNotificationService implements INotificationServiceExtension {
 @Override public void onNotificationReceived(INotificationReceivedEvent event) {
  JSONObject data=event.getNotification().getAdditionalData();
  if(data==null)return;
  if(data.optBoolean("nativeChallenge")) {
   event.preventDefault();FortiMuneApplication.captureProof(data);return;
  }
  String owner=event.getContext().getSharedPreferences("fortimune_push63",0).getString("owner","");
  if(!data.optString("userId").isEmpty() && !owner.equals(data.optString("userId")))event.preventDefault();
 }
}
