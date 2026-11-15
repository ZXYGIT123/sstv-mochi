package om.sstvsuite;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import om.sstvsuite.audio.AudioCapture;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class AppPrefs {
    private static final String FILE = "sstv_settings";
    private static final String KEY_AUTO_DETECT = "auto_detect";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_DYNAMIC_COLOR = "dynamic_color";
    private static final String KEY_ENCODE_MODE = "encode_mode_id";
    private static final String KEY_LOCKED_MODE = "locked_mode_id";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_SAMPLE_RATE = "default_sample_rate";
    private static final String KEY_WATERFALL = "show_waterfall";
    private static final String KEY_WATERMARK_COLOR = "watermark_color";
    private static final String KEY_WATERMARK_ENABLED = "watermark_enabled";
    private static final String KEY_WATERMARK_POSITION = "watermark_position";
    private static final String KEY_WATERMARK_SIZE = "watermark_size";
    private static final String KEY_WATERMARK_STROKE = "watermark_stroke";
    private static final String KEY_WATERMARK_TEXT = "watermark_text";
    private static volatile AppPrefs instance;
    private final SharedPreferences sp;

    private AppPrefs(Context context) {
        this.sp = context.getApplicationContext().getSharedPreferences(FILE, 0);
    }

    public static AppPrefs get(Context context) {
        AppPrefs appPrefs;
        AppPrefs appPrefs2 = instance;
        if (appPrefs2 != null) {
            return appPrefs2;
        }
        synchronized (AppPrefs.class) {
            appPrefs = instance;
            if (appPrefs == null) {
                appPrefs = new AppPrefs(context);
                instance = appPrefs;
            }
        }
        return appPrefs;
    }

    public boolean isAutoDetect() {
        return this.sp.getBoolean(KEY_AUTO_DETECT, true);
    }

    public void setAutoDetect(boolean z) {
        this.sp.edit().putBoolean(KEY_AUTO_DETECT, z).apply();
    }

    public String getLockedModeId() {
        return this.sp.getString(KEY_LOCKED_MODE, "");
    }

    public void setLockedModeId(String str) {
        SharedPreferences.Editor edit = this.sp.edit();
        if (str == null) {
            str = "";
        }
        edit.putString(KEY_LOCKED_MODE, str).apply();
    }

    public boolean isAutoSave() {
        return this.sp.getBoolean(KEY_AUTO_SAVE, false);
    }

    public void setAutoSave(boolean z) {
        this.sp.edit().putBoolean(KEY_AUTO_SAVE, z).apply();
    }

    public int getDefaultSampleRate() {
        int i = this.sp.getInt(KEY_SAMPLE_RATE, AudioCapture.SAMPLE_RATE);
        return (i == 32000 || i == 44100 || i == 48000) ? i : AudioCapture.SAMPLE_RATE;
    }

    public void setDefaultSampleRate(int i) {
        this.sp.edit().putInt(KEY_SAMPLE_RATE, i).apply();
    }

    public int getNightMode() {
        return this.sp.getInt(KEY_NIGHT_MODE, -1);
    }

    public void setNightMode(int i) {
        this.sp.edit().putInt(KEY_NIGHT_MODE, i).apply();
    }

    public boolean isDynamicColor() {
        return this.sp.getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    public void setDynamicColor(boolean z) {
        this.sp.edit().putBoolean(KEY_DYNAMIC_COLOR, z).apply();
    }

    public boolean isWaterfallVisible() {
        return this.sp.getBoolean(KEY_WATERFALL, true);
    }

    public void setWaterfallVisible(boolean z) {
        this.sp.edit().putBoolean(KEY_WATERFALL, z).apply();
    }

    public String getEncodeModeId() {
        return this.sp.getString(KEY_ENCODE_MODE, "");
    }

    public void setEncodeModeId(String str) {
        SharedPreferences.Editor edit = this.sp.edit();
        if (str == null) {
            str = "";
        }
        edit.putString(KEY_ENCODE_MODE, str).apply();
    }

    public boolean isWatermarkEnabled() {
        return this.sp.getBoolean(KEY_WATERMARK_ENABLED, true);
    }

    public void setWatermarkEnabled(boolean z) {
        this.sp.edit().putBoolean(KEY_WATERMARK_ENABLED, z).apply();
    }

    public int getWatermarkPosition() {
        int i = this.sp.getInt(KEY_WATERMARK_POSITION, 6);
        if (i < 0 || i > 8) {
            return 6;
        }
        return i;
    }

    public void setWatermarkPosition(int i) {
        this.sp.edit().putInt(KEY_WATERMARK_POSITION, i).apply();
    }

    public int getWatermarkSize() {
        int i = this.sp.getInt(KEY_WATERMARK_SIZE, 1);
        if (i < 0 || i > 2) {
            return 1;
        }
        return i;
    }

    public void setWatermarkSize(int i) {
        this.sp.edit().putInt(KEY_WATERMARK_SIZE, i).apply();
    }

    public int getWatermarkColor() {
        int i = this.sp.getInt(KEY_WATERMARK_COLOR, 0);
        if (i < 0 || i > 4) {
            return 0;
        }
        return i;
    }

    public void setWatermarkColor(int i) {
        this.sp.edit().putInt(KEY_WATERMARK_COLOR, i).apply();
    }

    public boolean isWatermarkStroke() {
        return this.sp.getBoolean(KEY_WATERMARK_STROKE, true);
    }

    public void setWatermarkStroke(boolean z) {
        this.sp.edit().putBoolean(KEY_WATERMARK_STROKE, z).apply();
    }

    public String getWatermarkText(Context context) {
        String string = this.sp.getString(KEY_WATERMARK_TEXT, null);
        return (string == null || string.trim().length() <= 0) ? defaultWatermarkText(context) : string;
    }

    public static String defaultWatermarkText(Context context) {
        return "N0CALL" + "\n" + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    public void setWatermarkText(String str) {
        this.sp.edit().putString(KEY_WATERMARK_TEXT, str).apply();
    }

    public void resetWatermarkText() {
        this.sp.edit().remove(KEY_WATERMARK_TEXT).apply();
    }
}
