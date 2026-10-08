package com.fortimune.injection;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
/** Ordered bounded images from the trusted main frame. No arbitrary file paths. */
final class ImageTransfer {
    static final int MAX_BYTES = 32 * 1024 * 1024;
    private ByteArrayOutputStream output;
    private int expected;
    private String filename, mime;
    void begin(String name, int size, String type) {
        if(output != null) throw new IllegalStateException("انتظر تجهيز الصورة الحالية أو ألغها.");
        String ext = "image/png".equals(type) ? "png" : "image/jpeg".equals(type) ? "jpg" : "image/webp".equals(type) ? "webp" : null;
        if(ext == null || name == null || !name.matches("[A-Za-z0-9_-]{1,120}\\." + ext) || size < 12 || size > MAX_BYTES)
            throw new IllegalArgumentException("حجم أو اسم أو نوع الصورة غير صالح.");
        expected=size; filename=name; mime=type; output=new ByteArrayOutputStream(Math.min(size,131072));
    }
    void append(int offset, String encoded) {
        if(output==null || offset!=output.size() || encoded==null || encoded.length()>174764)
            throw new IllegalArgumentException("ترتيب أجزاء الصورة غير صالح.");
        byte[] bytes=Base64.getDecoder().decode(encoded);
        if(bytes.length==0 || bytes.length>131072 || output.size()+bytes.length>expected)
            throw new IllegalArgumentException("حجم أجزاء الصورة غير صالح.");
        output.write(bytes,0,bytes.length);
    }
    byte[] bytes() {
        if(output==null || output.size()!=expected) throw new IllegalStateException("الصورة لم تكتمل.");
        byte[] b=output.toByteArray();
        boolean png=(b[0]&255)==137 && b[1]=='P' && b[2]=='N' && b[3]=='G' && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10;
        boolean jpeg=(b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255;
        boolean webp=b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F' && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P';
        if(!("image/png".equals(mime)&&png || "image/jpeg".equals(mime)&&jpeg || "image/webp".equals(mime)&&webp))
            throw new IllegalArgumentException("محتوى الصورة لا يطابق النوع المحدد.");
        return b;
    }
    String filename(){return filename;}
    String mime(){return mime;}
    void clear(){output=null;expected=0;filename=null;mime=null;}
}
