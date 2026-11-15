package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
final class FrontEnd {
    static final double CENTER_HZ = 1700.0d;
    static final int N_TONES = 6;
    static final int TONE_1100 = 0;
    static final int TONE_1200 = 1;
    static final int TONE_1300 = 2;
    static final int TONE_1500 = 3;
    static final int TONE_1900 = 4;
    static final int TONE_2300 = 5;
    private static final double[] TONE_FREQS = {1100.0d, 1200.0d, 1300.0d, 1500.0d, 1900.0d, 2300.0d};
    private int decCount;
    private final float[] decHist;
    private int decPos;
    private final float[] decTaps;
    private final int decim;
    private boolean hasPrevPhase;
    private final float[] histI;
    private final float[] histQ;
    final int inputRate;
    private int lpfPos;
    private final float[] lpfTaps;
    private final double phStepIm;
    private final double phStepRe;
    private int phasorFix;
    private double power;
    private final double powerAlpha;
    private double prevPhase;
    private final double resDecay;
    private final Sink sink;
    final int workRate;
    private double phRe = 1.0d;
    private double phIm = 0.0d;
    private final double[] resRe = new double[6];
    private final double[] resIm = new double[6];
    private final double[] resCoefRe = new double[6];
    private final double[] resCoefIm = new double[6];
    private float[] toneOut = new float[6];

    interface Sink {
        void onSample(float f, float f2, float f3, float f4, float[] fArr);
    }

    FrontEnd(int i, Sink sink) {
        double d;
        double d2;
        double d3;
        this.inputRate = i;
        this.sink = sink;
        int chooseWorkRate = chooseWorkRate(i);
        this.workRate = chooseWorkRate;
        int i2 = i / chooseWorkRate;
        this.decim = i2;
        if (i2 > 1) {
            double d4 = chooseWorkRate * 0.5d;
            d = 1.0d;
            d3 = 4.0d;
            double min = Math.min(2700.0d, chooseWorkRate * 0.38d);
            double d5 = i;
            d2 = 0.5d;
            int max = Math.max(9, ((int) Math.ceil((d5 * 4.0d) / Math.max(200.0d, d4 - min))) | 1);
            max = max > 257 ? 257 : max;
            this.decTaps = designLowpass((min + d4) * 0.5d, d5, max);
            this.decHist = new float[max];
        } else {
            d = 1.0d;
            d2 = 0.5d;
            d3 = 4.0d;
            this.decTaps = null;
            this.decHist = null;
        }
        double intValue = Integer.getInteger("sstv.lpfpass", 750).intValue();
        double intValue2 = Integer.getInteger("sstv.lpfstop", 1600).intValue();
        int max2 = Math.max(9, ((int) Math.ceil((chooseWorkRate * d3) / (intValue2 - intValue))) | 1);
        max2 = max2 > 401 ? 401 : max2;
        this.lpfTaps = designLowpass((intValue + intValue2) * d2, chooseWorkRate, max2);
        this.histI = new float[max2];
        this.histQ = new float[max2];
        double d6 = (-10681.415022205296d) / chooseWorkRate;
        this.phStepRe = Math.cos(d6);
        this.phStepIm = Math.sin(d6);
        this.resDecay = Math.exp((-1.0d) / (chooseWorkRate * 0.004d));
        for (int i3 = 0; i3 < 6; i3++) {
            double d7 = ((TONE_FREQS[i3] - CENTER_HZ) * 6.283185307179586d) / this.workRate;
            this.resCoefRe[i3] = this.resDecay * Math.cos(d7);
            this.resCoefIm[i3] = this.resDecay * Math.sin(d7);
        }
        this.powerAlpha = d / (this.workRate * 0.01d);
    }

    void reset() {
        if (this.decHist != null) {
            int i = 0;
            while (true) {
                float[] fArr = this.decHist;
                if (i >= fArr.length) {
                    break;
                }
                fArr[i] = 0.0f;
                i++;
            }
        }
        this.decPos = 0;
        this.decCount = 0;
        this.phRe = 1.0d;
        this.phIm = 0.0d;
        int i2 = 0;
        while (true) {
            float[] fArr2 = this.histI;
            if (i2 >= fArr2.length) {
                break;
            }
            fArr2[i2] = 0.0f;
            this.histQ[i2] = 0.0f;
            i2++;
        }
        this.lpfPos = 0;
        this.hasPrevPhase = false;
        this.prevPhase = 0.0d;
        for (int i3 = 0; i3 < 6; i3++) {
            this.resRe[i3] = 0.0d;
            this.resIm[i3] = 0.0d;
        }
        this.power = 0.0d;
    }

    void push(float f) {
        float[] fArr = this.decTaps;
        if (fArr != null) {
            float[] fArr2 = this.decHist;
            int i = this.decPos;
            fArr2[i] = f;
            int i2 = i + 1;
            this.decPos = i2;
            if (i2 >= fArr.length) {
                this.decPos = 0;
            }
            int i3 = this.decCount + 1;
            this.decCount = i3;
            if (i3 < this.decim) {
                return;
            }
            this.decCount = 0;
            int i4 = this.decPos;
            double d = 0.0d;
            int i5 = 0;
            while (true) {
                float[] fArr3 = this.decTaps;
                if (i5 < fArr3.length) {
                    d += fArr3[i5] * this.decHist[i4];
                    i4++;
                    if (i4 >= fArr3.length) {
                        i4 = 0;
                    }
                    i5++;
                } else {
                    float f2 = (float) d;
                    processWork(f2, f2);
                    return;
                }
            }
        } else {
            processWork(f, f);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void processWork(float f, float f2) {
        double d;
        float f3;
        double d2 = f;
        double d3 = this.phRe;
        double d4 = d2 * d3;
        double d5 = this.phIm;
        double d6 = d2 * d5;
        double d7 = this.phStepRe;
        double d8 = this.phStepIm;
        double d9 = (d3 * d7) - (d5 * d8);
        double d10 = (d3 * d8) + (d5 * d7);
        this.phRe = d9;
        this.phIm = d10;
        double d11 = (d9 * d9) + (d10 * d10);
        double d12 = 1.0d;
        int i = 1;
        if (d11 > 0.999d && d11 < 1.001d) {
            double sqrt = 1.0d / Math.sqrt(d11);
            this.phRe *= sqrt;
            this.phIm *= sqrt;
        } else {
            int i2 = this.phasorFix + 1;
            this.phasorFix = i2;
            if ((i2 & 255) == 0) {
                double sqrt2 = 1.0d / Math.sqrt(d11);
                this.phRe *= sqrt2;
                this.phIm *= sqrt2;
            }
        }
        float[] fArr = this.histI;
        int i3 = this.lpfPos;
        fArr[i3] = (float) d4;
        this.histQ[i3] = (float) d6;
        int i4 = i3 + 1;
        this.lpfPos = i4;
        if (i4 >= this.lpfTaps.length) {
            this.lpfPos = 0;
        }
        int i5 = this.lpfPos;
        double d13 = 0.0d;
        int i6 = 0;
        double d14 = 0.0d;
        while (true) {
            float[] fArr2 = this.lpfTaps;
            d = d12;
            if (i6 >= fArr2.length) {
                break;
            }
            double d15 = fArr2[i6];
            double d16 = d4;
            int i7 = i;
            d14 += this.histI[i5] * d15;
            d13 += d15 * this.histQ[i5];
            i5 += i7;
            if (i5 >= fArr2.length) {
                i5 = 0;
            }
            i6++;
            i = i7;
            d12 = d;
            d4 = d16;
        }
        double d17 = d4;
        boolean z = i != 0;
        double atan2 = Math.atan2(d13, d14);
        if (this.hasPrevPhase) {
            double d18 = atan2 - this.prevPhase;
            if (d18 > 3.141592653589793d) {
                d18 -= 6.283185307179586d;
            } else if (d18 < -3.141592653589793d) {
                d18 += 6.283185307179586d;
            }
            f3 = (float) (((d18 * this.workRate) / 6.283185307179586d) + CENTER_HZ);
        } else {
            this.hasPrevPhase = z;
            f3 = 1700.0f;
        }
        float f4 = f3;
        this.prevPhase = atan2;
        float sqrt3 = (float) Math.sqrt((d14 * d14) + (d13 * d13));
        double d19 = this.power;
        double d20 = sqrt3;
        this.power = d19 + (this.powerAlpha * ((d20 * d20) - d19));
        float[] fArr3 = this.toneOut;
        double d21 = d - this.resDecay;
        for (int i8 = 0; i8 < 6; i8++) {
            double d22 = this.resCoefRe[i8];
            double[] dArr = this.resRe;
            double d23 = dArr[i8];
            double d24 = this.resCoefIm[i8];
            double[] dArr2 = this.resIm;
            double d25 = dArr2[i8];
            double d26 = (d17 + (d22 * d23)) - (d24 * d25);
            double d27 = (d22 * d25) + d6 + (d24 * d23);
            dArr[i8] = d26;
            dArr2[i8] = d27;
            fArr3[i8] = (float) (((d26 * d26) + (d27 * d27)) * d21 * d21);
        }
        this.sink.onSample(f4, sqrt3, (float) this.power, f2, fArr3);
    }

    int lpfGroupDelay() {
        return (this.lpfTaps.length - 1) / 2;
    }

    int decimationDelay() {
        if (this.decTaps == null) {
            return 0;
        }
        return (this.decTaps.length - 1) / (this.decim * 2);
    }

    static int chooseWorkRate(int i) {
        int i2;
        if (i <= 48000) {
            return i;
        }
        double d = Double.MAX_VALUE;
        int i3 = i;
        for (int i4 = 2; i4 <= i / 20000; i4++) {
            if (i % i4 == 0 && (i2 = i / i4) >= 20000 && i2 <= 48000) {
                double abs = Math.abs(i2 - 44100.0d);
                if (abs < d) {
                    i3 = i2;
                    d = abs;
                }
            }
        }
        return i3;
    }

    static float[] designLowpass(double d, double d2, int i) {
        double sin;
        if ((i & 1) == 0) {
            i++;
        }
        float[] fArr = new float[i];
        int i2 = i / 2;
        double d3 = d / d2;
        double d4 = 0.0d;
        for (int i3 = 0; i3 < i; i3++) {
            int i4 = i3 - i2;
            if (i4 == 0) {
                sin = 2.0d * d3;
            } else {
                double d5 = i4;
                sin = Math.sin((6.283185307179586d * d3) * d5) / (d5 * 3.141592653589793d);
            }
            float blackman = (float) (sin * Dsp.blackman(i3, i));
            fArr[i3] = blackman;
            d4 += blackman;
        }
        double d6 = 1.0d / d4;
        for (int i5 = 0; i5 < i; i5++) {
            fArr[i5] = (float) (fArr[i5] * d6);
        }
        return fArr;
    }
}
