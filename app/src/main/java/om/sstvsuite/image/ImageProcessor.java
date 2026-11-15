package om.sstvsuite.image;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import om.sstvsuite.engine.Frame;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class ImageProcessor {
    private ImageProcessor() {
    }

    public static Bitmap loadBitmap(Context context, Uri uri, int i) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        BitmapFactory.Options options = new BitmapFactory.Options();
        int i2 = 1;
        options.inJustDecodeBounds = true;
        InputStream openInputStream = contentResolver.openInputStream(uri);
        if (openInputStream == null) {
            throw new IOException("cannot open image");
        }
        try {
            BitmapFactory.decodeStream(openInputStream, null, options);
            closeQuietly(openInputStream);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                throw new IOException("cannot read image bounds");
            }
            while (Math.max(options.outWidth, options.outHeight) / i2 > i) {
                i2 *= 2;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i2;
            options2.inPreferredConfig = Bitmap.Config.ARGB_8888;
            openInputStream = contentResolver.openInputStream(uri);
            if (openInputStream == null) {
                throw new IOException("cannot open image");
            }
            try {
                Bitmap decodeStream = BitmapFactory.decodeStream(openInputStream, null, options2);
                if (decodeStream == null) {
                    throw new IOException("decode failed");
                }
                int readRotation = readRotation(context, uri);
                if (readRotation == 0) {
                    return decodeStream;
                }
                Bitmap rotate = rotate(decodeStream, readRotation);
                if (rotate != decodeStream) {
                    decodeStream.recycle();
                }
                return rotate;
            } finally {
            }
        } finally {
        }
    }

    private static int readRotation(Context context, Uri uri) {
        try {
            InputStream openInputStream = context.getContentResolver().openInputStream(uri);
            if (openInputStream != null) {
                int attributeInt = new ExifInterface(openInputStream).getAttributeInt("Orientation", 1);
                if (attributeInt == 3) {
                    closeQuietly(openInputStream);
                    return 180;
                }
                if (attributeInt == 6) {
                    closeQuietly(openInputStream);
                    return 90;
                }
                if (attributeInt != 8) {
                    closeQuietly(openInputStream);
                    return 0;
                }
                closeQuietly(openInputStream);
                return 270;
            }
            closeQuietly(openInputStream);
            return 0;
        } catch (Throwable unused) {
            closeQuietly(null);
            return 0;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int i) {
        if (i % 360 != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(i);
            try {
                try {
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                } catch (Throwable unused) {
                    return bitmap;
                }
            } catch (Throwable unused2) {
            }
        }
        return bitmap;
    }

    public static Bitmap cropAndScale(Bitmap bitmap, int i, int i2) {
        int max;
        int i3;
        int max2 = Math.max(1, bitmap.getWidth());
        int max3 = Math.max(1, bitmap.getHeight());
        float f = i / i2;
        float f2 = max2;
        float f3 = max3;
        if (f2 / f3 > f) {
            i3 = Math.max(1, Math.round(f3 * f));
            max = max3;
        } else {
            max = Math.max(1, Math.round(f2 / f));
            i3 = max2;
        }
        int i4 = (max2 - i3) / 2;
        int i5 = (max3 - max) / 2;
        try {
            Bitmap createBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
            new Canvas(createBitmap).drawBitmap(bitmap, new Rect(i4, i5, i3 + i4, max + i5), new Rect(0, 0, i, i2), new Paint(3));
            return createBitmap;
        } catch (OutOfMemoryError unused) {
            return Bitmap.createBitmap(bitmap, i4, i5, i3, max);
        }
    }

    public static Frame toFrame(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] iArr = new int[width * height];
        bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
        return new Frame(width, height, iArr);
    }

    private static void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (Throwable unused) {
        }
    }
}
