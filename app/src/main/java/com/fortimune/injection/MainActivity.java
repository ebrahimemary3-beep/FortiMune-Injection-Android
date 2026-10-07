package com.fortimune.injection;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;
import android.os.Handler;
import android.os.Looper;
import java.util.Collections;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import com.onesignal.Continue;
import com.onesignal.OneSignal;

import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends ComponentActivity {
    private static final String START_URL = "https://ebrahimemary3-beep.github.io/FortiMune-Injection-Management/?app=android";
    private static final int FILE_CHOOSER = 1001;
    private static final int CAMERA_PERMISSION = 1002;
    private static final int SAVE_EXCEL = 1003;
    private final ExcelTransfer excelTransfer = new ExcelTransfer();
    private byte[] pendingExcel;
    private boolean pendingCameraPermission;
    private boolean notificationPermissionRequested;
    private WebView webView;
    private boolean nativePageReady;
    private final Handler nativeHandler = new Handler(Looper.getMainLooper());
    private final Runnable routePush = new Runnable() {
        @Override public void run() {
            if(webView != null && nativePageReady && isTrustedPage(webView.getUrl())) {
                JSONObject data=FortiMuneApplication.takeOpen();
                if(data!=null) webView.evaluateJavascript("window.receiveNativeOpen63 && window.receiveNativeOpen63("+data.toString()+")",null);
            }
            nativeHandler.postDelayed(this,500);
        }
    };
    private boolean isTrustedPage(String url) {
        if(url==null) return false;
        Uri u=Uri.parse(url);
        return "https".equals(u.getScheme()) && "ebrahimemary3-beep.github.io".equals(u.getHost())
            && u.getPort()==-1 && u.getPath()!=null && u.getPath().startsWith("/FortiMune-Injection-Management/");
    }
    private void installNativeBridge() {
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return;
        WebViewCompat.addWebMessageListener(webView,"FortiMuneNative",
            Collections.singleton("https://ebrahimemary3-beep.github.io"),
            (view,message,origin,isMainFrame,reply) -> {
                if(!isMainFrame || !isTrustedPage(view.getUrl())) return;
                try {
                    String raw = message.getData();
                    if (raw == null || raw.length() > 180000) return;
                    JSONObject input=new JSONObject(raw);
                    String action=input.optString("action");
                    nativePageReady=true;
                    if(action.startsWith("excel-")) {
                        JSONObject result = new JSONObject().put("id", input.optString("id"));
                        try {
                            if("excel-begin".equals(action)) {
                                if(pendingExcel != null) throw new IllegalStateException("انتظر حفظ الملف الحالي أو ألغِه.");
                                excelTransfer.begin(input.optString("filename"), input.optInt("size", -1));
                            } else if("excel-chunk".equals(action)) {
                                excelTransfer.append(input.optInt("offset", -1), input.optString("data"));
                            } else if("excel-save".equals(action)) {
                                if(pendingExcel != null) throw new IllegalStateException("انتظر حفظ الملف الحالي.");
                                pendingExcel = excelTransfer.bytes();
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                                intent.putExtra(Intent.EXTRA_TITLE, excelTransfer.filename());
                                try { startActivityForResult(intent, SAVE_EXCEL); }
                                catch (ActivityNotFoundException e) { pendingExcel = null; throw new IllegalStateException("لا يوجد تطبيق لحفظ المستندات على هذا الجهاز."); }
                                excelTransfer.clear();
                            } else if("excel-cancel".equals(action)) {
                                if(pendingExcel == null) excelTransfer.clear();
                            } else throw new IllegalArgumentException("عملية حفظ غير مدعومة.");
                            result.put("ok", true);
                        } catch(Exception e) { result.put("error", e.getMessage() == null ? "تعذر تجهيز ملف Excel." : e.getMessage()); }
                        reply.postMessage(result.toString());return;
                    }
                    if("enable".equals(action)) {
                        OneSignal.getUser().getPushSubscription().optIn();
                        OneSignal.getNotifications().requestPermission(false,Continue.none());
                    } else if("disable".equals(action)) {
                        OneSignal.getUser().getPushSubscription().optOut();
                        getSharedPreferences("fortimune_push63",0).edit().remove("owner").apply();
                        FortiMuneApplication.clearProof();
                        OneSignal.getNotifications().clearAllNotifications();
                    }
                    if("bound".equals(action))getSharedPreferences("fortimune_push63",0).edit().putString("owner",input.optString("userId")).apply();
                    JSONObject result=new JSONObject();
                    result.put("id",input.optString("id"));
                    result.put("subscriptionId",OneSignal.getUser().getPushSubscription().getId());
                    result.put("onesignalId",OneSignal.getUser().getOnesignalId());
                    result.put("token",OneSignal.getUser().getPushSubscription().getToken());
                    result.put("permission",OneSignal.getNotifications().getPermission());
                    result.put("optedIn",OneSignal.getUser().getPushSubscription().getOptedIn());
                    result.put("proof",FortiMuneApplication.getProof());
                    result.put("boundUserId",getSharedPreferences("fortimune_push63",0).getString("owner",""));
                    result.put("excelExport",true);
                    reply.postMessage(result.toString());
                } catch(Exception ignored) { /* No tokens or payloads in logs. */ }
            });
    }

    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        installNativeBridge();
        nativeHandler.post(routePush);
        webView.loadUrl(START_URL);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon) {
                nativePageReady=false;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost();
                if (isTrustedPage(u.toString())) {
                    return false;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) {}
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                fileCallback = callback;
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    pendingCameraPermission = true;
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                }
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (ActivityNotFoundException e) { fileCallback = null; return false; }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Uri downloadUri = Uri.parse(url);
            if (!"https".equals(downloadUri.getScheme()) || !isTrustedPage(url)) {
                Toast.makeText(this, "استخدم زر تصدير Excel في آخر نسخة من الصفحة", Toast.LENGTH_LONG).show();
                return;
            }
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FortiMune-Injection-Report.xlsx");
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "تم بدء تنزيل ملف Excel", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!notificationPermissionRequested) {
            notificationPermissionRequested = true;
            // Ask from a visible Activity. Do not redirect to Settings after a denial.
            // The SDK keeps notification permission separate from the camera flow.
            OneSignal.getNotifications().requestPermission(false, Continue.none());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION && pendingCameraPermission) {
            pendingCameraPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم السماح باستخدام الكاميرا", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_EXCEL) {
            final byte[] bytes = pendingExcel; pendingExcel = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null || bytes == null) {
                Toast.makeText(this, "تم إلغاء حفظ Excel", Toast.LENGTH_SHORT).show();return;
            }
            final Uri destination = data.getData();
            new Thread(() -> {
                boolean saved = false;
                try (java.io.OutputStream stream = getContentResolver().openOutputStream(destination, "wt")) {
                    if(stream == null) throw new java.io.IOException();
                    stream.write(bytes);stream.flush();saved = true;
                } catch (Exception ignored) { /* No report data in logs. */ }
                final boolean success = saved;
                runOnUiThread(() -> Toast.makeText(this, success ? "تم حفظ ملف Excel" : "تعذر حفظ ملف Excel. أعد المحاولة واختر مجلدًا آخر.", Toast.LENGTH_LONG).show());
            }, "FortiMune-Excel").start();
            return;
        }
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        nativeHandler.removeCallbacksAndMessages(null);
        excelTransfer.clear();pendingExcel = null;
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
