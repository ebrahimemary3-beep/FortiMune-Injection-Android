package com.fortimune.injection;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/** Bounded, ordered transfer of an XLSX from the trusted page. No filesystem paths. */
final class ExcelTransfer {
    static final int MAX_BYTES = 32 * 1024 * 1024;
    private ByteArrayOutputStream output;
    private int expected;
    private String filename;
    void begin(String name, int size) {
        if (output != null) throw new IllegalStateException("انتظر حفظ الملف الحالي أو ألغِه.");
        if (!name.matches("[A-Za-z0-9_-]{1,120}\\.xlsx") || size < 4 || size > MAX_BYTES)
            throw new IllegalArgumentException("حجم أو اسم ملف Excel غير صالح.");
        expected = size; filename = name; output = new ByteArrayOutputStream(Math.min(size, 131072));
    }
    void append(int offset, String encoded) {
        if (output == null || offset != output.size() || encoded.length() > 174764)
            throw new IllegalArgumentException("ترتيب أجزاء ملف Excel غير صالح.");
        byte[] chunk;
        try { chunk = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("جزء ملف Excel غير صالح."); }
        if (chunk.length == 0 || chunk.length > 131072 || output.size() + chunk.length > expected)
            throw new IllegalArgumentException("حجم أجزاء ملف Excel غير صالح.");
        output.write(chunk, 0, chunk.length);
    }
    byte[] bytes() {
        if (output == null || output.size() != expected) throw new IllegalStateException("ملف Excel لم يكتمل.");
        byte[] bytes = output.toByteArray();
        if (bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4)
            throw new IllegalArgumentException("صيغة ملف Excel غير صالحة.");
        return bytes;
    }
    String filename() { return filename; }
    void clear() { output = null; expected = 0; filename = null; }
}
