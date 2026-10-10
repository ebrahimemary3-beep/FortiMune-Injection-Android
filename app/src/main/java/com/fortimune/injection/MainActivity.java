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
    private static final int SAVE_IMAGE = 1004;
    private final ImageTransfer imageTransfer = new ImageTransfer();
    private byte[] pendingImage;
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
                    if("app-version".equals(action)) {
                        JSONObject result=new JSONObject().put("id",input.optString("id"))
                            .put("versionName",BuildConfig.VERSION_NAME).put("versionCode",BuildConfig.VERSION_CODE);
                        reply.postMessage(result.toString());return;
                    }
                    if(action.startsWith("image-")) {
                        JSONObject result=new JSONObject().put("id",input.optString("id"));
                        try {
                            if("image-begin".equals(action)) {
                                if(pendingImage!=null) throw new IllegalStateException("انتظر حفظ الصورة الحالية أو ألغها.");
                                imageTransfer.begin(input.optString("filename"),input.optInt("size",-1),input.optString("mime"));
                            } else if("image-chunk".equals(action)) {
                                imageTransfer.append(input.optInt("offset",-1),input.optString("data"));
                            } else if("image-save".equals(action)) {
                                if(pendingImage!=null) throw new IllegalStateException("انتظر حفظ الصورة الحالية.");
                                pendingImage=imageTransfer.bytes();
                                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType(imageTransfer.mime());intent.putExtra(Intent.EXTRA_TITLE,imageTransfer.filename());
                                try {startActivityForResult(intent,SAVE_IMAGE);}
                                catch(ActivityNotFoundException e){pendingImage=null;throw new IllegalStateException("لا يوجد تطبيق لحفظ الصور على هذا الجهاز.");}
                                imageTransfer.clear();
                            } else if("image-share".equals(action)) {
                                byte[] bytes=imageTransfer.bytes();
                                java.io.File dir=new java.io.File(getCacheDir(),"image-exports");
                                if(!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("تعذر تجهيز المشاركة.");
                                // Only our private image export folder; no report paths or storage permission.
                                java.io.File[] previous=dir.listFiles();
                                if(previous!=null) for(java.io.File f:previous) if(System.currentTimeMillis()-f.lastModified()>24L*60*60*1000) f.delete();
                                java.io.File file=new java.io.File(dir,java.util.UUID.randomUUID().toString()+"_"+imageTransfer.filename());
                                try(java.io.OutputStream stream=new java.io.FileOutputStream(file)){stream.write(bytes);}
                                Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".imageprovider",file);
                                Intent intent=new Intent(Intent.ACTION_SEND);intent.setType(imageTransfer.mime());
                                intent.putExtra(Intent.EXTRA_STREAM,uri);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setClipData(android.content.ClipData.newRawUri("FortiMune image",uri));
                                startActivity(Intent.createChooser(intent,"مشاركة صورة FortiMune"));imageTransfer.clear();
                            } else if("image-cancel".equals(action)) {imageTransfer.clear();}
                            else throw new IllegalArgumentException("عملية صورة غير مدعومة.");
                            result.put("ok",true);
                        }catch(Exception e){result.put("error",e.getMessage()==null?"تعذر تجهيز الصورة.":e.getMessage());}
                        reply.postMessage(result.toString());return;
                    }
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
                    result.put("imageExport",true);
                    reply.postMessage(result.toString());
                } catch(Exception ignored) { /* No tokens or payloads in logs. */ }
            });
    }

    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraOutput;
    private java.io.File cameraFile;
    private boolean capturePending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.addView(webView,new android.widget.FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        if(savedInstanceState == null) {
            android.widget.FrameLayout intro = new android.widget.FrameLayout(this);
            intro.setBackgroundColor(android.graphics.Color.WHITE);
            intro.setClickable(true);
            android.widget.ImageView logo = new android.widget.ImageView(this);
            logo.setImageResource(R.drawable.fortimune_intro);
            logo.setContentDescription("FortiMune");
            logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            int size = (int)(240 * getResources().getDisplayMetrics().density);
            android.widget.FrameLayout.LayoutParams logoParams = new android.widget.FrameLayout.LayoutParams(size,size);
            logoParams.gravity=android.view.Gravity.CENTER;
            intro.addView(logo,logoParams);root.addView(intro,new android.widget.FrameLayout.LayoutParams(-1,-1));
            nativeHandler.postDelayed(() -> intro.animate().alpha(0f).setDuration(250)
                .withEndAction(() -> root.removeView(intro)).start(),1250);
        }
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
                if ("mailto".equalsIgnoreCase(u.getScheme())) {
                    Intent mail = new Intent(Intent.ACTION_SENDTO, u);
                    mail.setPackage("com.microsoft.office.outlook");
                    try { startActivity(mail); }
                    catch (ActivityNotFoundException missingOutlook) {
                        mail.setPackage(null);
                        try { startActivity(Intent.createChooser(mail, "فتح رسالة البريد")); }
                        catch (ActivityNotFoundException missingMail) {
                            android.widget.Toast.makeText(MainActivity.this, "ثبّت Outlook أو افتح الرسالة من زر Outlook على الويب", android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                    return true;
                }

                if (isTrustedPage(u.toString())) {
                    return false;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) {}
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (!isTrustedPage(webView.getUrl())) { callback.onReceiveValue(null); return true; }
                finishFileChooser(null);
                fileCallback = callback;
                capturePending = params.isCaptureEnabled();
                if (capturePending) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        pendingCameraPermission = true;
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                    } else launchCamera();
                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (ActivityNotFoundException e) { finishFileChooser(null); Toast.makeText(MainActivity.this,"لا يوجد تطبيق لاختيار الصور",Toast.LENGTH_LONG).show(); }
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
                if (capturePending && fileCallback != null) launchCamera();
            } else {
                finishFileChooser(null);
                Toast.makeText(this,"لتصوير التقرير، فعّل إذن الكاميرا من إعدادات التطبيق، أو اختر صورة من المعرض.",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SAVE_IMAGE) {
            final byte[] bytes=pendingImage;pendingImage=null;
            if(resultCode!=RESULT_OK || data==null || data.getData()==null || bytes==null){
                Toast.makeText(this,"تم إلغاء حفظ الصورة",Toast.LENGTH_SHORT).show();return;
            }
            final Uri destination=data.getData();
            new Thread(() -> {
                boolean saved=false;
                try(java.io.OutputStream stream=getContentResolver().openOutputStream(destination,"wt")){
                    if(stream==null) throw new java.io.IOException();stream.write(bytes);stream.flush();saved=true;
                }catch(Exception ignored){ saved=false; /* No image data in logs. */ }
                final boolean success=saved;
                runOnUiThread(() -> Toast.makeText(this,success?"تم حفظ الصورة":"تعذر حفظ الصورة. اختر مجلدًا آخر.",Toast.LENGTH_LONG).show());
            },"FortiMune-Image").start();return;
        }
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
                } catch (Exception ignored) { saved=false; /* No report data in logs. */ }
                final boolean success = saved;
                runOnUiThread(() -> Toast.makeText(this, success ? "تم حفظ ملف Excel" : "تعذر حفظ ملف Excel. أعد المحاولة واختر مجلدًا آخر.", Toast.LENGTH_LONG).show());
            }, "FortiMune-Excel").start();
            return;
        }
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK) {
                if (capturePending && cameraOutput != null && cameraFile != null && cameraFile.length() > 0) result = new Uri[]{cameraOutput};
                else if (!capturePending && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            }
            finishFileChooser(result);
        }
    }

    private void launchCamera() {
        try {
            java.io.File dir = new java.io.File(getCacheDir(),"report-camera");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException();
            java.io.File[] old = dir.listFiles();
            if(old != null) for(java.io.File f:old) if(System.currentTimeMillis()-f.lastModified()>24L*60*60*1000) f.delete();
            cameraFile = java.io.File.createTempFile("report-", ".jpg",dir);
            cameraOutput = androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".imageprovider",cameraFile);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,cameraOutput);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setClipData(android.content.ClipData.newRawUri("FortiMune report",cameraOutput));
            startActivityForResult(intent,FILE_CHOOSER);
        } catch(Exception e) {
            finishFileChooser(null);
            Toast.makeText(this,"تعذر فتح الكاميرا. يمكنك اختيار صورة من المعرض.",Toast.LENGTH_LONG).show();
        }
    }
    private void finishFileChooser(Uri[] result) {
        ValueCallback<Uri[]> callback = fileCallback; fileCallback = null;
        if (result == null && cameraFile != null) cameraFile.delete();
        if (callback != null) callback.onReceiveValue(result);
        cameraOutput = null; cameraFile = null; capturePending = false;
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        nativeHandler.removeCallbacksAndMessages(null);
        finishFileChooser(null);
        excelTransfer.clear();pendingExcel = null;
        imageTransfer.clear();pendingImage=null;
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
