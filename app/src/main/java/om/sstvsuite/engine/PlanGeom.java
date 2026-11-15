package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
final class PlanGeom {
    final boolean alternating;
    final int countY0;
    final int decim;
    final int[][] len;
    final int lineSamples;
    final double lineSamplesExact;
    final SstvMode mode;
    final boolean pd;
    final boolean rgb;
    final int[][] rowDelta;
    final int sampleRate;
    final int[][] start;
    final int transmitLines;
    final boolean yuv;

    PlanGeom(SstvMode sstvMode, int i) {
        this(sstvMode, i, 1);
    }

    PlanGeom(SstvMode sstvMode, int i, int i2) {
        int i3;
        int i4;
        int i5;
        this.mode = sstvMode;
        int max = Math.max(1, i2);
        this.decim = max;
        this.sampleRate = i / max;
        double round = Math.round((sstvMode.lineMs * i) / 1000.0d) / max;
        this.lineSamplesExact = round;
        this.lineSamples = (int) Math.round(round);
        int length = sstvMode.plans.length;
        this.start = new int[length][];
        this.len = new int[length][];
        this.rowDelta = new int[length][];
        int i6 = 0;
        int i7 = 0;
        boolean z = false;
        boolean z2 = false;
        while (i6 < length) {
            LineRun[] lineRunArr = sstvMode.plans[i6];
            int length2 = lineRunArr.length;
            int[] iArr = new int[length2];
            int[] iArr2 = new int[length2];
            int[] iArr3 = new int[length2];
            int i8 = 0;
            int i9 = 0;
            while (i8 < length2) {
                iArr[i8] = workSample(lineRunArr[i8].startMs);
                int i10 = i8 + 1;
                if (i10 < length2) {
                    i3 = length;
                    i4 = i10;
                    i5 = workSample(lineRunArr[i10].startMs);
                } else {
                    i3 = length;
                    i4 = i10;
                    i5 = this.lineSamples;
                }
                int i11 = i5 - iArr[i8];
                iArr2[i8] = i11;
                if (i11 < 0) {
                    iArr2[i8] = 0;
                }
                if (lineRunArr[i8].kind == 3) {
                    char c = lineRunArr[i8].channel;
                    if (c == 'Y') {
                        iArr3[i8] = i9;
                        i9++;
                        if (i6 == 0) {
                            i7++;
                        }
                    } else if (c != 'U' && c != 'V') {
                        z2 = true;
                    }
                    z = true;
                }
                length = i3;
                i8 = i4;
            }
            this.start[i6] = iArr;
            this.len[i6] = iArr2;
            this.rowDelta[i6] = iArr3;
            i6++;
            length = length;
        }
        this.countY0 = i7;
        this.yuv = z;
        this.rgb = z2;
        boolean z3 = i7 >= 2;
        this.pd = z3;
        this.alternating = sstvMode.plans.length > 1 && sstvMode.plans[0] != sstvMode.plans[1];
        int i12 = sstvMode.height;
        this.transmitLines = z3 ? i12 / 2 : i12;
    }

    private int workSample(double d) {
        return (int) Math.round(Math.round(((d * this.sampleRate) * this.decim) / 1000.0d) / this.decim);
    }

    int syncSamples() {
        int[] iArr = this.len[0];
        if (iArr.length > 0) {
            return iArr[0];
        }
        return 0;
    }

    double porchFrequency() {
        LineRun[] lineRunArr = this.mode.plans[0];
        if (lineRunArr.length <= 1 || lineRunArr[1].kind == 3) {
            return 1500.0d;
        }
        return lineRunArr[1].frequency;
    }

    int porchSamples() {
        int[] iArr = this.len[0];
        if (iArr.length > 1) {
            return iArr[1];
        }
        return 0;
    }
}
