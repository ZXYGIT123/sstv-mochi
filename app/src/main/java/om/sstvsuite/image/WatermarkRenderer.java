package om.sstvsuite.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class WatermarkRenderer {
    private static final int[] COLORS = {-1, -16777216, -5317, -11677471, -1092784};
    private static final float[] SIZE_FACTORS = {0.045f, 0.065f, 0.09f};

    private WatermarkRenderer() {
    }

    public static int colorOf(int i) {
        if (i >= 0) {
            int[] iArr = COLORS;
            if (i < iArr.length) {
                return iArr[i];
            }
        }
        return COLORS[0];
    }

    public static void draw(Bitmap bitmap, String str, int i, int i2, int i3, boolean z) {
        if (bitmap == null || str == null || str.trim().length() == 0) {
            return;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float[] fArr = SIZE_FACTORS;
        float max = Math.max(10.0f, Math.min(width, height) * fArr[Math.max(0, Math.min(fArr.length - 1, i2))]);
        float min = Math.min(width, height) * 0.04f;
        int colorOf = colorOf(i3);
        Paint paint = new Paint(1);
        paint.setTextSize(max);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(colorOf);
        Paint paint2 = new Paint(1);
        paint2.setTextSize(max);
        paint2.setTypeface(Typeface.DEFAULT_BOLD);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(max * 0.09f);
        paint2.setColor((((Color.red(colorOf) * 299) + (Color.green(colorOf) * 587)) + (Color.blue(colorOf) * 114)) / 1000 > 150 ? -16777216 : -1);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float f = (fontMetrics.descent - fontMetrics.ascent) * 1.15f;
        String[] split = str.split("\n");
        float length = split.length * f;
        int i4 = i / 3;
        int i5 = i % 3;
        float f2 = i4 == 0 ? min : i4 == 1 ? (height - length) / 2.0f : (height - min) - length;
        if (f2 < min) {
            f2 = min;
        }
        if (i5 == 0) {
            paint.setTextAlign(Paint.Align.LEFT);
            paint2.setTextAlign(Paint.Align.LEFT);
        } else if (i5 == 1) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint2.setTextAlign(Paint.Align.CENTER);
            min = width / 2.0f;
        } else {
            paint.setTextAlign(Paint.Align.RIGHT);
            paint2.setTextAlign(Paint.Align.RIGHT);
            min = width - min;
        }
        Canvas canvas = new Canvas(bitmap);
        for (int i6 = 0; i6 < split.length; i6++) {
            float f3 = ((i6 * f) + f2) - fontMetrics.ascent;
            if (f3 > height) {
                return;
            }
            if (z) {
                canvas.drawText(split[i6], min, f3, paint2);
            }
            canvas.drawText(split[i6], min, f3, paint);
        }
    }
}
