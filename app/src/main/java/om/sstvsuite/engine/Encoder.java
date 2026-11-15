package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class Encoder {
    private static final double AMP = 29490.3d;
    private static final double TWO_PI = 6.283185307179586d;
    private final int[] argb;
    private boolean bodyStarted;
    private int emitted;
    private int hdrIndex;
    private final int[] headerFreq;
    private final int[] headerLen;
    private final int headerSamples;
    private final int height;
    private int lineIdx;
    private final int lineSamples;
    private final SstvMode mode;
    private final boolean pd;
    private double phase;
    private int pixel;
    private final double rate;
    private int runIdx;
    private final int[][] runLen;
    private final int[][] runRowDelta;
    private final int[][] runStart;
    private final int sampleRate;
    private int segEnd;
    private double segFreq;
    private int segStart;
    private final int totalSamples;
    private final int transmitLines;
    private final byte[] uPlane;
    private final byte[] vPlane;
    private final int width;
    private final byte[] yPlane;
    private final boolean yuv;

    private static int bilerp(int i, int i2, int i3, int i4, double d, double d2) {
        double d3 = i + ((i2 - i) * d);
        int i5 = (int) (d3 + (((i3 + ((i4 - i3) * d)) - d3) * d2) + 0.5d);
        if (i5 < 0) {
            return 0;
        }
        if (i5 > 255) {
            return 255;
        }
        return i5;
    }

    public Encoder(Frame frame, SstvMode sstvMode, int i) {
        char c;
        int[] iArr;
        int[] iArr2;
        char c2;
        if (frame == null) {
            throw new IllegalArgumentException("image == null");
        }
        if (sstvMode == null) {
            throw new IllegalArgumentException("mode == null");
        }
        if (i <= 0) {
            throw new IllegalArgumentException("bad sampleRate: " + i);
        }
        this.mode = sstvMode;
        this.sampleRate = i;
        this.rate = i;
        int i2 = sstvMode.width;
        this.width = i2;
        int i3 = sstvMode.height;
        this.height = i3;
        this.argb = scaleImage(frame, i2, i3);
        int i4 = 0;
        LineRun[] lineRunArr = sstvMode.plans[0];
        int i5 = 0;
        boolean z = false;
        int i6 = 0;
        while (true) {
            c = 'Y';
            if (i5 >= lineRunArr.length) {
                break;
            }
            if (lineRunArr[i5].kind == 3) {
                if (lineRunArr[i5].channel == 'Y') {
                    i6++;
                } else if (lineRunArr[i5].channel != 'U' && lineRunArr[i5].channel != 'V') {
                }
                z = true;
            }
            i5++;
        }
        this.yuv = z;
        char c3 = 2;
        this.pd = i6 >= 2;
        if (z) {
            int i7 = this.width;
            int i8 = this.height;
            byte[] bArr = new byte[i7 * i8];
            byte[] bArr2 = new byte[i7 * i8];
            byte[] bArr3 = new byte[i7 * i8];
            int i9 = 0;
            while (true) {
                int[] iArr3 = this.argb;
                if (i9 >= iArr3.length) {
                    break;
                }
                int i10 = iArr3[i9];
                int i11 = (i10 >> 16) & 255;
                int i12 = (i10 >> 8) & 255;
                int i13 = i10 & 255;
                bArr[i9] = (byte) Yuv.toY(i11, i12, i13);
                bArr2[i9] = (byte) Yuv.toU(i11, i12, i13);
                bArr3[i9] = (byte) Yuv.toV(i11, i12, i13);
                i9++;
            }
            this.yPlane = bArr;
            this.uPlane = bArr2;
            this.vPlane = bArr3;
        } else {
            this.yPlane = null;
            this.uPlane = null;
            this.vPlane = null;
        }
        int length = sstvMode.plans.length;
        this.runStart = new int[length][];
        this.runLen = new int[length][];
        this.runRowDelta = new int[length][];
        double d = 1000.0d;
        int round = (int) Math.round((sstvMode.lineMs * this.rate) / 1000.0d);
        int i14 = 0;
        while (i14 < length) {
            LineRun[] lineRunArr2 = sstvMode.plans[i14];
            int length2 = lineRunArr2.length;
            int[] iArr4 = new int[length2];
            int[] iArr5 = new int[length2];
            char c4 = c3;
            int[] iArr6 = new int[length2];
            int i15 = i4;
            int i16 = i15;
            while (i4 < length2) {
                double d2 = d;
                iArr4[i4] = (int) Math.round((lineRunArr2[i4].startMs * this.rate) / d2);
                int i17 = i4 + 1;
                int round2 = (i17 < length2 ? (int) Math.round((lineRunArr2[i17].startMs * this.rate) / d2) : round) - iArr4[i4];
                iArr5[i4] = round2;
                if (round2 < 0) {
                    iArr5[i4] = i15;
                }
                if (lineRunArr2[i4].kind == 3) {
                    c2 = 'Y';
                    if (lineRunArr2[i4].channel == 'Y') {
                        iArr6[i4] = i16;
                        i16++;
                        c = c2;
                        i4 = i17;
                        d = d2;
                    }
                } else {
                    c2 = 'Y';
                }
                iArr6[i4] = i15;
                c = c2;
                i4 = i17;
                d = d2;
            }
            this.runStart[i14] = iArr4;
            this.runLen[i14] = iArr5;
            this.runRowDelta[i14] = iArr6;
            i14++;
            c3 = c4;
            i4 = i15;
        }
        char c5 = c3;
        int i18 = i4;
        double d3 = d;
        int i19 = 1;
        this.lineSamples = round;
        this.transmitLines = this.pd ? this.height / 2 : this.height;
        int round3 = (int) Math.round((this.rate * 300.0d) / d3);
        int round4 = (int) Math.round((this.rate * 10.0d) / d3);
        int round5 = (int) Math.round((this.rate * 30.0d) / d3);
        int i20 = sstvMode.vis;
        if (i20 >= 0) {
            iArr = new int[13];
            iArr2 = new int[13];
            iArr[i18] = 1900;
            iArr2[i18] = round3;
            iArr[1] = 1200;
            iArr2[1] = round4;
            iArr[c5] = 1900;
            iArr2[c5] = round3;
            iArr[3] = 1200;
            iArr2[3] = round5;
            int i21 = 4;
            int i22 = i18;
            int i23 = i22;
            while (true) {
                if (i22 >= 7) {
                    break;
                }
                int i24 = (i20 >> i22) & 1;
                i23 ^= i24;
                int i25 = i19;
                if (i24 != i25) {
                    r10 = 1100;
                }
                iArr[i21] = r10;
                iArr2[i21] = round5;
                i22++;
                i21++;
                i19 = i25;
            }
            iArr[i21] = i23 != i19 ? 1100 : 1300;
            int i26 = i21 + 1;
            iArr2[i21] = round5;
            iArr[i26] = 1200;
            iArr2[i26] = round5;
        } else {
            iArr = new int[3];
            iArr2 = new int[3];
            iArr[i18] = 1900;
            iArr2[i18] = round3;
            iArr[1] = 1200;
            iArr2[1] = round4;
            iArr[c5] = 1900;
            iArr2[c5] = round3;
        }
        this.headerFreq = iArr;
        this.headerLen = iArr2;
        int i27 = i18;
        for (int i28 = i27; i28 < iArr2.length; i28++) {
            i27 += iArr2[i28];
        }
        this.headerSamples = i27;
        this.totalSamples = i27 + (this.transmitLines * this.lineSamples);
        resetRenderState();
    }

    public int getHeaderSamples() {
        return this.headerSamples;
    }

    public int getTotalSamples() {
        return this.totalSamples;
    }

    public int render(int i, short[] sArr, int i2, int i3) {
        if (sArr == null) {
            throw new IllegalArgumentException("out == null");
        }
        if (i2 < 0 || i3 < 0 || i2 + i3 > sArr.length) {
            throw new IllegalArgumentException("bad out range");
        }
        int i4 = i < 0 ? 0 : i;
        int i5 = this.totalSamples;
        if (i4 > i5) {
            i4 = i5;
        }
        if (i4 != this.emitted) {
            seek(i4);
        }
        int min = Math.min(i3, this.totalSamples - this.emitted);
        int i6 = 0;
        while (i6 < min && (this.emitted < this.segEnd || advanceSegment())) {
            int min2 = Math.min(min - i6, this.segEnd - this.emitted);
            double d = (this.segFreq * TWO_PI) / this.rate;
            int i7 = i2 + i6;
            for (int i8 = 0; i8 < min2; i8++) {
                sArr[i7 + i8] = (short) (Math.sin(this.phase) * AMP >= 0.0d ? r12 + 0.5d : r12 - 0.5d);
                this.phase += d;
                while (true) {
                    double d2 = this.phase;
                    if (d2 >= TWO_PI) {
                        this.phase = d2 - TWO_PI;
                    }
                }
            }
            i6 += min2;
            this.emitted += min2;
        }
        return i6;
    }

    public short[] renderAll() {
        int render;
        short[] sArr = new short[this.totalSamples];
        int i = 0;
        while (true) {
            int i2 = this.totalSamples;
            if (i >= i2 || (render = render(i, sArr, i, i2 - i)) <= 0) {
                break;
            }
            i += render;
        }
        return sArr;
    }

    private void resetRenderState() {
        this.emitted = 0;
        this.bodyStarted = false;
        this.hdrIndex = 0;
        this.lineIdx = 0;
        this.runIdx = 0;
        this.pixel = 0;
        this.segStart = 0;
        this.segEnd = 0;
        this.segFreq = 0.0d;
        this.phase = 0.0d;
    }

    private void seek(int i) {
        resetRenderState();
        while (true) {
            int i2 = this.emitted;
            if (i2 >= i) {
                return;
            }
            if (i2 >= this.segEnd && !advanceSegment()) {
                return;
            }
            int i3 = this.emitted;
            int min = Math.min(i - i3, this.segEnd - i3);
            this.phase += ((this.segFreq * TWO_PI) / this.rate) * min;
            while (true) {
                double d = this.phase;
                if (d >= TWO_PI) {
                    this.phase = d - TWO_PI;
                }
            }
            this.emitted += min;
        }
    }

    private boolean advanceSegment() {
        while (true) {
            if (!this.bodyStarted) {
                int i = this.hdrIndex;
                int[] iArr = this.headerLen;
                if (i >= iArr.length) {
                    this.bodyStarted = true;
                    this.lineIdx = 0;
                    this.runIdx = 0;
                    this.pixel = 0;
                } else {
                    int i2 = this.emitted;
                    this.segStart = i2;
                    this.segEnd = i2 + iArr[i];
                    this.segFreq = this.headerFreq[i];
                    this.hdrIndex = i + 1;
                    return true;
                }
            } else {
                int i3 = this.lineIdx;
                if (i3 >= this.transmitLines) {
                    return false;
                }
                LineRun[] planForLine = this.mode.planForLine(i3);
                int length = (this.lineIdx & 1) % this.mode.plans.length;
                int i4 = this.runIdx;
                if (i4 >= planForLine.length) {
                    this.lineIdx++;
                    this.runIdx = 0;
                    this.pixel = 0;
                } else {
                    int i5 = this.headerSamples + (this.lineIdx * this.lineSamples) + this.runStart[length][i4];
                    int i6 = this.runLen[length][i4];
                    LineRun lineRun = planForLine[i4];
                    if (lineRun.kind == 3) {
                        double d = i6;
                        int round = ((int) Math.round((this.pixel * d) / this.width)) + i5;
                        int round2 = i5 + ((int) Math.round(((this.pixel + 1) * d) / this.width));
                        int i7 = (this.pd ? this.lineIdx * 2 : this.lineIdx) + this.runRowDelta[length][this.runIdx];
                        int i8 = this.height;
                        if (i7 >= i8) {
                            i7 = i8 - 1;
                        }
                        this.segStart = round;
                        this.segEnd = round2;
                        this.segFreq = toneFor(lineRun.channel, i7, this.pixel);
                        int i9 = this.pixel + 1;
                        this.pixel = i9;
                        if (i9 >= this.width) {
                            this.pixel = 0;
                            this.runIdx++;
                        }
                        return true;
                    }
                    this.segStart = i5;
                    this.segEnd = i5 + i6;
                    this.segFreq = lineRun.frequency;
                    this.runIdx++;
                    return true;
                }
            }
        }
    }

    private double toneFor(char c, int i, int i2) {
        int i3;
        int i4;
        byte[] bArr;
        int i5 = (i * this.width) + i2;
        if (this.yuv) {
            if (c == 'Y') {
                bArr = this.yPlane;
            } else if (c == 'U') {
                bArr = this.uPlane;
            } else if (c == 'V') {
                bArr = this.vPlane;
            } else {
                bArr = this.yPlane;
            }
            i4 = bArr[i5];
        } else {
            int i6 = this.argb[i5];
            if (c == 'R') {
                i4 = i6 >> 16;
            } else {
                if (c != 'G') {
                    i3 = i6 & 255;
                    return ((i3 * 800.0d) / 255.0d) + 1500.0d;
                }
                i4 = i6 >> 8;
            }
        }
        i3 = i4 & 255;
        return ((i3 * 800.0d) / 255.0d) + 1500.0d;
    }

    static int[] scaleImage(Frame frame, int i, int i2) {
        int i3 = i;
        int i4 = i2;
        int[] iArr = new int[i3 * i4];
        int i5 = frame.width;
        int i6 = frame.height;
        int[] iArr2 = frame.argb;
        double d = i5 / i3;
        double d2 = i6 / i4;
        int i7 = 0;
        while (i7 < i4) {
            double d3 = ((i7 + 0.5d) * d2) - 0.5d;
            if (d3 < 0.0d) {
                d3 = 0.0d;
            }
            int i8 = i7;
            int floor = (int) Math.floor(d3);
            double d4 = d3 - floor;
            int i9 = floor + 1;
            if (i9 >= i6) {
                i9 = i6 - 1;
            }
            if (floor >= i6) {
                floor = i6 - 1;
            }
            int i10 = i8 * i3;
            int i11 = floor * i5;
            int i12 = i9 * i5;
            int i13 = 0;
            while (i13 < i3) {
                double d5 = ((i13 + 0.5d) * d) - 0.5d;
                if (d5 < 0.0d) {
                    d5 = 0.0d;
                }
                int[] iArr3 = iArr2;
                int floor2 = (int) Math.floor(d5);
                double d6 = d5 - floor2;
                int i14 = floor2 + 1;
                if (i14 >= i5) {
                    i14 = i5 - 1;
                }
                if (floor2 >= i5) {
                    floor2 = i5 - 1;
                }
                int i15 = iArr3[i11 + floor2];
                int i16 = iArr3[i11 + i14];
                int i17 = iArr3[floor2 + i12];
                int i18 = iArr3[i14 + i12];
                int[] iArr4 = iArr;
                iArr4[i10 + i13] = bilerp(i15 & 255, i16 & 255, i17 & 255, i18 & 255, d6, d4) | (-16777216) | (bilerp((i15 >> 16) & 255, (i16 >> 16) & 255, (i17 >> 16) & 255, (i18 >> 16) & 255, d6, d4) << 16) | (bilerp((i15 >> 8) & 255, (i16 >> 8) & 255, (i17 >> 8) & 255, (i18 >> 8) & 255, d6, d4) << 8);
                i13++;
                i3 = i;
                iArr2 = iArr3;
                iArr = iArr4;
            }
            i7 = i8 + 1;
            i3 = i;
            i4 = i2;
        }
        return iArr;
    }
}
