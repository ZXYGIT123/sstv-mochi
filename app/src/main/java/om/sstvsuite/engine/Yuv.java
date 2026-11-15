package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
final class Yuv {
    static int clamp255(int i) {
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    static int clampLevel(int i) {
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    static int toArgb(int i, int i2, int i3) {
        int i4 = i2 - 128;
        int i5 = i3 - 128;
        int i6 = (i - 16) * 298;
        int i7 = (((i5 * 409) + i6) + 128) >> 8;
        int i8 = (((i6 - (i4 * 100)) - (i5 * 208)) + 128) >> 8;
        int i9 = ((i6 + (i4 * 516)) + 128) >> 8;
        if (i7 < 0) {
            i7 = 0;
        } else if (i7 > 255) {
            i7 = 255;
        }
        if (i8 < 0) {
            i8 = 0;
        } else if (i8 > 255) {
            i8 = 255;
        }
        if (i9 < 0) {
            i9 = 0;
        } else if (i9 > 255) {
            i9 = 255;
        }
        return i9 | (-16777216) | (i7 << 16) | (i8 << 8);
    }

    private Yuv() {
    }

    static int toY(int i, int i2, int i3) {
        return clamp255((int) Math.round(((((i * 65.738d) + (i2 * 129.057d)) + (i3 * 25.064d)) / 256.0d) + 16.0d));
    }

    static int toU(int i, int i2, int i3) {
        return clamp255((int) Math.round(((((i * (-37.945d)) - (i2 * 74.494d)) + (i3 * 112.439d)) / 256.0d) + 128.0d));
    }

    static int toV(int i, int i2, int i3) {
        return clamp255((int) Math.round(((((i * 112.439d) - (i2 * 94.154d)) - (i3 * 18.285d)) / 256.0d) + 128.0d));
    }

    static int rgbToArgb(int i, int i2, int i3) {
        return (clamp255(i) << 16) | (-16777216) | (clamp255(i2) << 8) | clamp255(i3);
    }
}
