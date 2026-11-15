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
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] bArr2 = new byte[8];
                    int i = AudioCapture.SAMPLE_RATE;
                    int i2 = 1;
                    int i3 = 16;
                    while (readFullyQuiet(inputStream, bArr2)) {
                        String str = new String(bArr2, 0, 4, StandardCharsets.US_ASCII);
                        char c3 = c;
                        char c4 = c2;
                        int i4 = ((bArr2[5] & 255) << 8) | (bArr2[4] & 255) | ((bArr2[6] & 255) << 16) | ((bArr2[7] & 255) << 24);
                        if (i4 < 0) {
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
                            byte[] bArr4 = new byte[8192];
                            int i8 = i4;
                            while (i8 > 0) {
                                int read = inputStream.read(bArr4, 0, Math.min(8192, i8));
                                if (read <= 0) {
                                    break;
                                }
                                byteArrayOutputStream.write(bArr4, 0, read);
                                i8 -= read;
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
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    int length = byteArray.length / 2;
                    short[] sArr = new short[length];
                    for (int i9 = 0; i9 < length; i9++) {
                        int i10 = i9 * 2;
                        sArr[i9] = (short) ((byteArray[i10 + 1] << 8) | (byteArray[i10] & 255));
                    }
                    return new Pcm(sArr, i, i2);
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
        int length = (int) ((sArr.length * i2) / i);
        short[] sArr2 = new short[length];
        double d = i / i2;
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
