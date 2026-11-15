package om.sstvsuite.audio;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class WavStreamWriter implements Closeable {
    private static final int HEADER_SIZE = 44;
    private boolean closed;
    private long dataBytes;
    private final RandomAccessFile file;
    private final int sampleRate;

    public WavStreamWriter(File file, int i) throws IOException {
        this.sampleRate = i;
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        this.file = randomAccessFile;
        randomAccessFile.setLength(0L);
        randomAccessFile.write(new byte[44]);
    }

    public void write(short[] sArr, int i, int i2) throws IOException {
        if (this.closed) {
            throw new IOException("writer closed");
        }
        int i3 = i2 * 2;
        byte[] bArr = new byte[i3];
        for (int i4 = 0; i4 < i2; i4++) {
            short s = sArr[i + i4];
            int i5 = i4 * 2;
            bArr[i5] = (byte) s;
            bArr[i5 + 1] = (byte) (s >> 8);
        }
        this.file.write(bArr);
        this.dataBytes += i3;
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            byte[] buildHeader = buildHeader((int) Math.min(this.dataBytes, 2147483639L));
            this.file.seek(0L);
            this.file.write(buildHeader);
            this.file.setLength(this.dataBytes + 44);
        } finally {
            try {
                this.file.close();
            } catch (Throwable unused) {
            }
        }
    }

    private byte[] buildHeader(int i) {
        byte[] bArr = new byte[44];
        putAscii(bArr, 0, "RIFF");
        putInt(bArr, 4, i + 36);
        putAscii(bArr, 8, "WAVE");
        putAscii(bArr, 12, "fmt ");
        putInt(bArr, 16, 16);
        putShort(bArr, 20, 1);
        putShort(bArr, 22, 1);
        putInt(bArr, 24, this.sampleRate);
        putInt(bArr, 28, this.sampleRate * 2);
        putShort(bArr, 32, 2);
        putShort(bArr, 34, 16);
        putAscii(bArr, 36, "data");
        putInt(bArr, 40, i);
        return bArr;
    }

    private static void putAscii(byte[] bArr, int i, String str) {
        for (int i2 = 0; i2 < str.length(); i2++) {
            bArr[i + i2] = (byte) str.charAt(i2);
        }
    }

    private static void putInt(byte[] bArr, int i, int i2) {
        bArr[i] = (byte) i2;
        bArr[i + 1] = (byte) (i2 >> 8);
        bArr[i + 2] = (byte) (i2 >> 16);
        bArr[i + 3] = (byte) (i2 >> 24);
    }

    private static void putShort(byte[] bArr, int i, int i2) {
        bArr[i] = (byte) i2;
        bArr[i + 1] = (byte) (i2 >> 8);
    }
}
