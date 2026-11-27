package om.sstvsuite.engine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import om.sstvsuite.audio.AudioCapture;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class WavIO {

    public static final class Pcm {
        public final int channels;
        public final int sampleRate;
        public final short[] samples;

        public Pcm(short[] sArr, int i, int i2) {
            this.samples = sArr;
            this.sampleRate = i;
            this.channels = i2;
        }
    }

    private WavIO() {
    }

    public static Pcm read(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            Pcm read = read(fileInputStream);
            fileInputStream.close();
            return read;
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static Pcm read(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[12];
        readFully(inputStream, bArr);
        if (bArr[0] == 82 && bArr[1] == 73) {
            char c = 2;
            if (bArr[2] == 70) {
                char c2 = 3;
                if (bArr[3] == 70 && bArr[8] == 87 && bArr[9] == 65 && bArr[10] == 86 && bArr[11] == 69) {
                    // 直接按 data 块大小分配 short[]，流式填充。
                    // 旧实现先堆进 ByteArrayOutputStream（扩容翻倍，PD290/HF Fax 的大文件峰值上百 MB）
                    // 再复制成 short[]，大文件直接 OOM，被上层 catch 吞掉后误报"unsupported audio file"。
                    short[] accumulated = null;
                    int accumulatedCount = 0;
                    int pendingSkip = 0;
                    byte[] bArr2 = new byte[8];
                    int i = AudioCapture.SAMPLE_RATE;
                    int i2 = 1;
                    int i3 = 16;
                    while (readFullyQuiet(inputStream, bArr2)) {
                        String str = new String(bArr2, 0, 4, StandardCharsets.US_ASCII);
                        char c3 = c;
                        char c4 = c2;
                        int i4 = ((bArr2[5] & 255) << 8) | (bArr2[4] & 255) | ((bArr2[6] & 255) << 16) | ((bArr2[7] & 255) << 24);
                        // 注意：i4 为负（0x80000000 以上，如 0xFFFFFFFF 流式标记或损坏值）不再直接
                        // break——那是旧实现丢 data 块的元凶之一。data 分支里有 unknownSize 处理；
                        // 其他块遇到负大小没法安全跳过，此时才退出循环。
                        if (i4 < 0 && !str.equals("data")) {
                            break;
                        }
                        if (str.equals("fmt ")) {
                            int min = Math.min(i4, 40);
                            byte[] bArr3 = new byte[min];
                            readFully(inputStream, bArr3);
                            if (i4 > min) {
                                skipFully(inputStream, i4 - min);
                            }
                            int i5 = (bArr3[0] & 255) | ((bArr3[1] & 255) << 8);
                            i2 = ((bArr3[c4] & 255) << 8) | (bArr3[c3] & 255);
                            int i6 = (bArr3[4] & 255) | ((bArr3[5] & 255) << 8) | ((bArr3[6] & 255) << 16) | ((bArr3[7] & 255) << 24);
                            int i7 = ((bArr3[15] & 255) << 8) | (bArr3[14] & 255);
                            if (i5 != 1 && i5 != 65534) {
                                throw new IOException("unsupported WAV encoding: " + i5);
                            }
                            i3 = i7;
                            i = i6;
                        } else if (str.equals("data")) {
                            // 非法 data 大小：0xFFFFFFFF（流式标记）、损坏文件的高位为 1 的值等。
                            // 统一当"未知大小"处理——流式读到 EOF，每 1M 样本扩一次容。
                            // 负数直接 new short[i4/2] 会抛 NegativeArraySizeException。
                            final int MAX_SAMPLES = 200_000_000; // ≈ 400MB PCM，足够 PD290/HF Fax
                            boolean unknownSize = i4 < 0 || i4 == 0xFFFFFFFF;
                            if (unknownSize) {
                                // 读到 EOF 为止；while 循环用 i8>0 判断，Integer.MAX_VALUE 够大
                                i4 = Integer.MAX_VALUE - 1;
                            }
                            if ((i4 & 1) != 0) {
                                throw new IOException("odd data chunk: " + i4);
                            }
                            int need = unknownSize ? 65536 : i4 / 2; // 未知大小时先给个初始容量，读满再扩
                            if (need > MAX_SAMPLES) {
                                throw new IOException("WAV data too large: " + need + " samples");
                            }
                            if (accumulated == null) {
                                accumulated = new short[Math.max(need, 1)];
                                accumulatedCount = 0;
                            } else if (!unknownSize && accumulatedCount + need > accumulated.length) {
                                int grow = accumulatedCount + need;
                                if (grow > MAX_SAMPLES) grow = MAX_SAMPLES;
                                short[] bigger = new short[grow];
                                System.arraycopy(accumulated, 0, bigger, 0, accumulatedCount);
                                accumulated = bigger;
                            }
                            byte[] bArr4 = new byte[8192];
                            int i8 = i4;
                            while (i8 > 0) {
                                int read = inputStream.read(bArr4, 0, Math.min(8192, Math.min(i8, bArr4.length)));
                                if (read <= 0) {
                                    break; // EOF：未知大小时自然结束
                                }
                                if (accumulatedCount + read / 2 > accumulated.length) {
                                    // 动态扩容（未知大小时走到这里）
                                    int grow = Math.max(accumulated.length * 2, accumulatedCount + read / 2);
                                    if (grow > MAX_SAMPLES) grow = MAX_SAMPLES;
                                    if (grow > accumulated.length) {
                                        short[] bigger = new short[grow];
                                        System.arraycopy(accumulated, 0, bigger, 0, accumulatedCount);
                                        accumulated = bigger;
                                    }
                                }
                                for (int p = 0; p + 1 < read; p += 2) {
                                    accumulated[accumulatedCount++] = (short) ((bArr4[p + 1] << 8) | (bArr4[p] & 255));
                                }
                                if ((read & 1) != 0) {
                                    pendingSkip = 1;
                                    i8 -= read;
                                    break;
                                }
                                i8 -= read;
                            }
                            if (pendingSkip != 0) {
                                skipFully(inputStream, 1L);
                                pendingSkip = 0;
                            }
                        } else {
                            skipFully(inputStream, i4);
                        }
                        if ((i4 & 1) != 0) {
                            skipFully(inputStream, 1L);
                        }
                        c = c3;
                        c2 = c4;
                    }
                    if (i3 != 16) {
                        throw new IOException("only 16-bit PCM supported, got " + i3);
                    }
                    if (accumulated == null) {
                        throw new IOException("no data chunk");
                    }
                    if (accumulatedCount != accumulated.length) {
                        short[] exact = new short[accumulatedCount];
                        System.arraycopy(accumulated, 0, exact, 0, accumulatedCount);
                        accumulated = exact;
                    }
                    return new Pcm(accumulated, i, i2);
                }
            }
        }
        throw new IOException("not a RIFF/WAVE file");
    }

    public static void write(File file, short[] sArr, int i) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            write(fileOutputStream, sArr, i);
            fileOutputStream.close();
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static void write(OutputStream outputStream, short[] sArr, int i) throws IOException {
        int length = sArr.length * 2;
        byte[] bArr = new byte[44];
        putAscii(bArr, 0, "RIFF");
        putIntLE(bArr, 4, length + 36);
        putAscii(bArr, 8, "WAVE");
        putAscii(bArr, 12, "fmt ");
        putIntLE(bArr, 16, 16);
        putShortLE(bArr, 20, 1);
        putShortLE(bArr, 22, 1);
        putIntLE(bArr, 24, i);
        putIntLE(bArr, 28, i * 2);
        putShortLE(bArr, 32, 2);
        putShortLE(bArr, 34, 16);
        putAscii(bArr, 36, "data");
        putIntLE(bArr, 40, length);
        outputStream.write(bArr);
        byte[] bArr2 = new byte[8192];
        int i2 = 0;
        while (i2 < sArr.length) {
            int min = Math.min(4096, sArr.length - i2);
            for (int i3 = 0; i3 < min; i3++) {
                short s = sArr[i2 + i3];
                int i4 = i3 * 2;
                bArr2[i4] = (byte) s;
                bArr2[i4 + 1] = (byte) (s >> 8);
            }
            outputStream.write(bArr2, 0, min * 2);
            i2 += min;
        }
        outputStream.flush();
    }

    public static short[] toMono(Pcm pcm) {
        if (pcm.channels <= 1) {
            return pcm.samples;
        }
        int length = pcm.samples.length / pcm.channels;
        short[] sArr = new short[length];
        for (int i = 0; i < length; i++) {
            int i2 = 0;
            for (int i3 = 0; i3 < pcm.channels; i3++) {
                i2 += pcm.samples[(pcm.channels * i) + i3];
            }
            sArr[i] = (short) (i2 / pcm.channels);
        }
        return sArr;
    }

    public static short[] resampleMono(short[] sArr, int i, int i2) {
        if (i == i2) {
            return sArr;
        }
        // sArr.length * i2 用 int 乘会溢出（PD120 6M 样本 × 44100 ≈ 2.7e11），
        // 得到负数长度 → new short[负数] 抛 NegativeArraySizeException。必须用 long。
        int length = (int) (((long) sArr.length * i2) / i);
        short[] sArr2 = new short[length];
        // i / i2 是 int 整数除法，48000/44100 恒等于 1——重采样步进错误，
        // 等于把音频拉长 8.8%，SSTV 频率整体下移 8%，解码必然失败。必须浮点除。
        double d = (double) i / i2;
        double d2 = 0.0d;
        for (int i3 = 0; i3 < length; i3++) {
            float f = (float) (d2 - ((int) d2));
            sArr2[i3] = (short) Math.max(-32768.0f, Math.min(32767.0f, (sArr[(int) d2] * (1.0f - f)) + (sArr[Math.min(((int) d2) + 1, sArr.length - 1)] * f)));
            d2 += d;
        }
        return sArr2;
    }

    private static void putAscii(byte[] bArr, int i, String str) {
        for (int i2 = 0; i2 < str.length(); i2++) {
            bArr[i + i2] = (byte) str.charAt(i2);
        }
    }

    private static void putIntLE(byte[] bArr, int i, int i2) {
        bArr[i] = (byte) i2;
        bArr[i + 1] = (byte) (i2 >> 8);
        bArr[i + 2] = (byte) (i2 >> 16);
        bArr[i + 3] = (byte) (i2 >> 24);
    }

    private static void putShortLE(byte[] bArr, int i, int i2) {
        bArr[i] = (byte) i2;
        bArr[i + 1] = (byte) (i2 >> 8);
    }

    private static void readFully(InputStream inputStream, byte[] bArr) throws IOException {
        if (!readFullyQuiet(inputStream, bArr)) {
            throw new IOException("unexpected end of stream");
        }
    }

    private static boolean readFullyQuiet(InputStream inputStream, byte[] bArr) throws IOException {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (read < 0) {
                return false;
            }
            i += read;
        }
        return true;
    }

    private static void skipFully(InputStream inputStream, long j) throws IOException {
        while (j > 0) {
            long skip = inputStream.skip(j);
            if (skip <= 0) {
                if (inputStream.read() < 0) {
                    return;
                } else {
                    skip = 1;
                }
            }
            j -= skip;
        }
    }
}
