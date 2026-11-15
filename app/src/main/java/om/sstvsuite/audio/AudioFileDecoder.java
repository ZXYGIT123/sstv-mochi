package om.sstvsuite.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import om.sstvsuite.engine.WavIO;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class AudioFileDecoder {
    private static final int MAX_IDLE_LOOPS = 600;
    private static final long MAX_ITERATIONS = 2000000;

    private AudioFileDecoder() {
    }

    public static short[] decodeToMono(Context context, Uri uri, int i) throws IOException {
        String str;
        ContentResolver contentResolver = context.getContentResolver();
        short[] sArr = null;
        try {
            str = contentResolver.getType(uri);
        } catch (Throwable unused) {
            str = null;
        }
        String queryDisplayName = queryDisplayName(context, uri);
        String lowerCase = queryDisplayName == null ? "" : queryDisplayName.toLowerCase(Locale.ROOT);
        if ((str != null && (str.contains("wav") || str.contains("x-wav"))) || lowerCase.endsWith(".wav") || lowerCase.endsWith(".wave")) {
            try {
                return readWav(contentResolver, uri, i);
            } catch (Throwable unused2) {
            }
        }
        try {
            sArr = decodeWithMediaCodec(context, uri);
        } catch (Throwable unused3) {
        }
        if (sArr != null && sArr.length > 0) {
            return sArr;
        }
        try {
            return readWav(contentResolver, uri, i);
        } catch (Throwable th) {
            throw new IOException("unsupported audio file", th);
        }
    }

    private static short[] readWav(ContentResolver contentResolver, Uri uri, int i) throws IOException {
        InputStream openInputStream = contentResolver.openInputStream(uri);
        if (openInputStream == null) {
            throw new IOException("cannot open stream");
        }
        try {
            WavIO.Pcm read = WavIO.read(openInputStream);
            return WavIO.resampleMono(WavIO.toMono(read), read.sampleRate, i);
        } finally {
            try {
                openInputStream.close();
            } catch (Throwable unused) {
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:57:0x013f A[Catch: all -> 0x01e7, TryCatch #2 {all -> 0x01e7, blocks: (B:31:0x0071, B:33:0x0088, B:37:0x009b, B:39:0x00a1, B:41:0x00a9, B:44:0x00e1, B:46:0x00e7, B:48:0x00ed, B:50:0x00f1, B:51:0x0103, B:57:0x013f, B:63:0x0148, B:64:0x014f, B:70:0x0114, B:72:0x011e, B:73:0x0122, B:75:0x0128, B:76:0x012c, B:78:0x0134, B:80:0x00b9, B:82:0x00c1, B:83:0x00cd, B:86:0x0155, B:87:0x015c), top: B:30:0x0071 }] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0150  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static short[] decodeWithMediaCodec(android.content.Context r27, android.net.Uri r28) throws java.io.IOException {
        /*
            Method dump skipped, instructions count: 549
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: om.sstvsuite.audio.AudioFileDecoder.decodeWithMediaCodec(android.content.Context, android.net.Uri):short[]");
    }

    private static short[] mixToMono(short[] sArr, int i) {
        if (i <= 1) {
            return sArr;
        }
        int length = sArr.length / i;
        short[] sArr2 = new short[length];
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = 0;
            for (int i4 = 0; i4 < i; i4++) {
                i3 += sArr[(i2 * i) + i4];
            }
            sArr2[i2] = (short) (i3 / i);
        }
        return sArr2;
    }

    private static String queryDisplayName(Context context, Uri uri) {
        int columnIndex;
        try {
            Cursor query = context.getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (query == null) {
                return null;
            }
            try {
                if (query.moveToFirst() && (columnIndex = query.getColumnIndex("_display_name")) >= 0) {
                    return query.getString(columnIndex);
                }
                query.close();
                return null;
            } finally {
                query.close();
            }
        } catch (Throwable unused) {
            return null;
        }
    }

    private static final class AudioFormatHelper {
        static final int ENCODING_PCM_16BIT = 2;
        static final int ENCODING_PCM_FLOAT = 4;

        private AudioFormatHelper() {
        }
    }
}
