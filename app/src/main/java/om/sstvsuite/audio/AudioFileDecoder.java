package om.sstvsuite.audio;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import om.sstvsuite.engine.WavIO;

/**
 * 音频文件解码。
 *
 * 说明：原工程里有一个 decodeWithMediaCodec(Context, Uri) 分支（用 MediaCodec 走系统
 * 解码器，可以吃 mp3/aac/flac 等压缩格式），但那份源码在导出/还原时方法体就丢了，
 * 1.1.1 的 APK 里它就是个 throw UnsupportedOperationException 的桩。所以本版本
 * 只支持 WAV（RIFF/PCM 16-bit）——与 1.1.1 的实际行为一致，只是把错误信息透出来。
 *
 * 大文件：WavIO.read 已改为按 data 块一次分配 short[]、流式填充（原来是
 * ByteArrayOutputStream 翻倍扩容 + toByteArray 复制，PD290/HF Fax 那种 28–58MB 的
 * 文件内存峰值可达 140–300MB，直接 OOM，再被 catch(Throwable) 吞掉，误报
 * "unsupported audio file"）。如果将来要支持压缩格式，把 decodeWithMediaCodec
 * 用 MediaCodec/MediaExtractor 实现回来即可。
 */
public final class AudioFileDecoder {

    private AudioFileDecoder() {
    }

    public static short[] decodeToMono(Context context, Uri uri, int targetRate) throws IOException {
        String mime = null;
        try {
            mime = context.getContentResolver().getType(uri);
        } catch (Throwable t) {
            // 忽略，靠扩展名判断
        }
        String name = queryDisplayName(context, uri);
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        boolean looksWav = (mime != null && (mime.contains("wav") || mime.contains("x-wav")))
                || lower.endsWith(".wav") || lower.endsWith(".wave");
        if (!looksWav) {
            throw new IOException("只支持 WAV 文件（16-bit PCM），收到的是: "
                    + (name != null ? name : (mime != null ? mime : "未知类型")));
        }
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) throw new IOException("无法打开文件: " + name);
            try {
                WavIO.Pcm pcm = WavIO.read(in);
                return WavIO.resampleMono(WavIO.toMono(pcm), pcm.sampleRate, targetRate);
            } finally {
                try { in.close(); } catch (Throwable ignored) {}
            }
        } catch (IOException e) {
            throw new IOException("WAV 解码失败: " + e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            throw new IOException("文件太大，内存不足（" + name + "），请用较短的片段", e);
        } catch (Throwable t) {
            android.util.Log.e("SSTVWav", "decode failed", t);
            throw new IOException("WAV 解码失败: " + t.getClass().getSimpleName()
                    + (t.getMessage() != null ? ": " + t.getMessage() : ""), t);
        }
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try {
            android.database.Cursor c = context.getContentResolver()
                    .query(uri, new String[]{"_display_name"}, null, null, null);
            if (c == null) return null;
            try {
                if (c.moveToFirst()) {
                    int idx = c.getColumnIndex("_display_name");
                    if (idx >= 0) return c.getString(idx);
                }
                return null;
            } finally {
                c.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
