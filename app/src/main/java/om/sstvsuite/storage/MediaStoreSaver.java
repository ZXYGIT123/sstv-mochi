package om.sstvsuite.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class MediaStoreSaver {
    private MediaStoreSaver() {
    }

    public static String saveImage(Context context, Bitmap bitmap, String str) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str);
            contentValues.put("mime_type", "image/png");
            contentValues.put("relative_path", Environment.DIRECTORY_PICTURES + "/SSTV");
            contentValues.put("is_pending", (Integer) 1);
            ContentResolver contentResolver = context.getContentResolver();
            Uri insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (insert == null) {
                throw new IOException("MediaStore insert failed");
            }
            try {
                OutputStream openOutputStream = contentResolver.openOutputStream(insert);
                if (openOutputStream == null) {
                    throw new IOException("cannot open output");
                }
                try {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, openOutputStream)) {
                        throw new IOException("compress failed");
                    }
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("is_pending", (Integer) 0);
                    try {
                        contentResolver.update(insert, contentValues2, null, null);
                    } catch (Throwable unused) {
                    }
                    return Environment.DIRECTORY_PICTURES + "/SSTV/" + str;
                } finally {
                    try {
                        openOutputStream.close();
                    } catch (Throwable unused2) {
                    }
                }
            } catch (Throwable th) {
                try {
                    contentResolver.delete(insert, null, null);
                } catch (Throwable unused3) {
                }
                throw th;
            }
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SSTV");
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("cannot create " + file.getAbsolutePath());
        }
        File file2 = new File(file, str);
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                throw new IOException("compress failed");
            }
            MediaScannerConnection.scanFile(context, new String[]{file2.getAbsolutePath()}, null, null);
            return file2.getAbsolutePath();
        } finally {
            try {
                fileOutputStream.close();
            } catch (Throwable unused4) {
            }
        }
    }

    public static File writeImageToCache(Context context, Bitmap bitmap, String str) throws IOException {
        File file = new File(context.getCacheDir(), "images");
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("cannot create cache dir");
        }
        File file2 = new File(file, str);
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        try {
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                return file2;
            }
            throw new IOException("compress failed");
        } finally {
            try {
                fileOutputStream.close();
            } catch (Throwable unused) {
            }
        }
    }

    public static String publishAudio(Context context, File file, String str) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str);
            contentValues.put("mime_type", "audio/wav");
            contentValues.put("relative_path", Environment.DIRECTORY_DOWNLOADS + "/SSTV");
            boolean z = true;
            contentValues.put("is_pending", (Integer) 1);
            ContentResolver contentResolver = context.getContentResolver();
            Uri insert = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (insert != null) {
                try {
                    OutputStream openOutputStream = contentResolver.openOutputStream(insert);
                    if (openOutputStream != null) {
                        copyFile(file, openOutputStream);
                    } else {
                        z = false;
                    }
                    if (!z) {
                    }
                    if (z) {
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("is_pending", (Integer) 0);
                        try {
                            contentResolver.update(insert, contentValues2, null, null);
                        } catch (Throwable unused) {
                        }
                        return Environment.DIRECTORY_DOWNLOADS + "/SSTV/" + str;
                    }
                } finally {
                    try {
                        contentResolver.delete(insert, null, null);
                    } catch (Throwable unused2) {
                    }
                }
            }
        }
        return file.getAbsolutePath();
    }

    private static void copyFile(File file, OutputStream outputStream) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read <= 0) {
                    break;
                } else {
                    outputStream.write(bArr, 0, read);
                }
            }
            outputStream.flush();
            try {
                fileInputStream.close();
            } catch (Throwable unused) {
            }
            try {
                outputStream.close();
            } catch (Throwable unused2) {
            }
        } finally {
        }
    }

    public static File audioOutputDir(Context context) {
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (externalFilesDir == null) {
            externalFilesDir = new File(context.getFilesDir(), "audio");
        }
        File file = new File(externalFilesDir, "SSTV");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}
