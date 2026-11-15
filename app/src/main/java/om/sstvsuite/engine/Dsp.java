package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
final class Dsp {
    static final double TWO_PI = 6.283185307179586d;

    private Dsp() {
    }

    static double blackman(int i, int i2) {
        if (i2 <= 1) {
            return 1.0d;
        }
        double d = (i * TWO_PI) / (i2 - 1);
        return (0.42d - (Math.cos(d) * 0.5d)) + (Math.cos(d * 2.0d) * 0.08d);
    }

    static float[] hilbertFir(int i) {
        if ((i & 1) == 0) {
            i++;
        }
        int i2 = i / 2;
        float[] fArr = new float[i];
        for (int i3 = 0; i3 < i; i3++) {
            int i4 = i3 - i2;
            fArr[i3] = (float) (((i4 == 0 || (i4 & 1) == 0) ? 0.0d : 2.0d / (i4 * 3.141592653589793d)) * blackman(i3, i));
        }
        return fArr;
    }

    static final class MovingAverage {
        private final float[] buf;
        private int count;
        private final int len;
        private int pos;
        private double sum;

        MovingAverage(int i) {
            int max = Math.max(1, i);
            this.len = max;
            this.buf = new float[max];
        }

        void reset() {
            int i = 0;
            this.pos = 0;
            this.count = 0;
            this.sum = 0.0d;
            while (true) {
                float[] fArr = this.buf;
                if (i >= fArr.length) {
                    return;
                }
                fArr[i] = 0.0f;
                i++;
            }
        }

        float push(float f) {
            int i = this.count;
            int i2 = this.len;
            if (i < i2) {
                float[] fArr = this.buf;
                int i3 = this.pos;
                fArr[i3] = f;
                double d = this.sum + f;
                this.sum = d;
                int i4 = i + 1;
                this.count = i4;
                this.pos = (i3 + 1) % i2;
                return (float) (d / i4);
            }
            double d2 = this.sum;
            float[] fArr2 = this.buf;
            int i5 = this.pos;
            double d3 = d2 - fArr2[i5];
            this.sum = d3;
            fArr2[i5] = f;
            double d4 = d3 + f;
            this.sum = d4;
            this.pos = (i5 + 1) % i2;
            return (float) (d4 / i2);
        }

        float value() {
            int i = this.count;
            if (i == 0) {
                return 0.0f;
            }
            return (float) (this.sum / i);
        }
    }

    static final class FmDemodulator {
        private final int center;
        private boolean hasPrev;
        private final float[] hist;
        private int histPos;
        private double lastPhaseDiff;
        private double prevPhase;
        private final double rate;
        private final double ratePerTwoPi;
        private final float[] taps;
        private final int tapsLen;

        FmDemodulator(int i) {
            double d = i;
            int round = (int) Math.round(0.00145d * d);
            round = round < 15 ? 15 : round;
            round = round > 71 ? 71 : round;
            float[] hilbertFir = Dsp.hilbertFir((round & 1) == 0 ? round + 1 : round);
            this.taps = hilbertFir;
            int length = hilbertFir.length;
            this.tapsLen = length;
            this.center = length / 2;
            this.hist = new float[length];
            this.rate = d;
            this.ratePerTwoPi = d / Dsp.TWO_PI;
        }

        void reset() {
            int i = 0;
            while (true) {
                float[] fArr = this.hist;
                if (i >= fArr.length) {
                    this.histPos = 0;
                    this.hasPrev = false;
                    this.prevPhase = 0.0d;
                    this.lastPhaseDiff = 0.0d;
                    return;
                }
                fArr[i] = 0.0f;
                i++;
            }
        }

        float push(float f) {
            int i;
            float[] fArr = this.hist;
            int i2 = this.histPos;
            fArr[i2] = f;
            int i3 = 0;
            double d = 0.0d;
            while (true) {
                i = this.tapsLen;
                if (i3 >= i) {
                    break;
                }
                d += this.taps[i3] * this.hist[i2];
                i2--;
                if (i2 < 0) {
                    i2 = i - 1;
                }
                i3++;
            }
            int i4 = this.histPos;
            int i5 = i4 - this.center;
            if (i5 < 0) {
                i5 += i;
            }
            double d2 = this.hist[i5];
            int i6 = i4 + 1;
            this.histPos = i6;
            if (i6 >= i) {
                this.histPos = 0;
            }
            double atan2 = Math.atan2(d, d2);
            if (!this.hasPrev) {
                this.hasPrev = true;
                this.prevPhase = atan2;
                this.lastPhaseDiff = 0.0d;
                return 0.0f;
            }
            double d3 = atan2 - this.prevPhase;
            if (d3 > 3.141592653589793d) {
                do {
                    d3 -= Dsp.TWO_PI;
                } while (d3 > 3.141592653589793d);
            } else if (d3 < -3.141592653589793d) {
                do {
                    d3 += Dsp.TWO_PI;
                } while (d3 < -3.141592653589793d);
            }
            this.prevPhase = atan2;
            this.lastPhaseDiff = d3;
            return (float) (d3 * this.ratePerTwoPi);
        }

        int groupDelay() {
            return this.center;
        }

        double rate() {
            return this.rate;
        }
    }
}
