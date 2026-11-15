package om.sstvsuite.engine;

import java.lang.reflect.Array;
import om.sstvsuite.engine.Dsp;
import om.sstvsuite.engine.FrontEnd;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class Decoder {
    private static final double BLACK_FREQ = 1500.0d;
    private static final int CSMOOTH_EDGE = 26;
    private static final int CSMOOTH_PASSES = 2;
    private static final boolean CSMOOTH_WIDE = true;
    private static final double LEADER_FREQ = 1900.0d;
    private static final int ST_DECODE = 5;
    private static final int ST_DONE = 6;
    private static final int ST_FIND_LINE_START = 2;
    private static final int ST_SEARCH_LEADER = 0;
    private static final int ST_SEARCH_SYNC = 4;
    private static final int ST_VIS_PARSE = 1;
    private static final int ST_WRAASE_CHECK = 3;
    private static final double SYNC_FREQ = 1200.0d;
    private static final double TONE_TH = 0.12d;
    private static final double TONE_TH_LOW = 0.08d;
    private static final double VIS_BIT0_START = 340.0d;
    private static final double VIS_BIT_MS = 30.0d;
    private static final double VIS_PARITY_START = 550.0d;
    private static final double VIS_STOP_START = 580.0d;
    private static final double WHITE_FREQ = 2300.0d;
    private final float[] ampRing;
    private boolean complete;
    private boolean endOfStream;
    private final Dsp.MovingAverage freqAvg;
    private double freqOffset;
    private final float[] freqRing;
    private final FrontEnd front;
    private PlanGeom geom;
    private boolean hasPrevChroma;
    private int height;
    private final float[] hilbRing;
    private final int hilbShift;
    private final Dsp.MovingAverage hilbertAvg;
    private final Dsp.FmDemodulator hilbertDemod;
    private int[] image;
    private float lastFreq;
    private int lastProgressLines;
    private int lastSearchedTo;
    private int leaderEnd;
    private int lineIndex;
    private int lineStart;
    private int linesDone;
    private final Listener listener;
    private SstvMode lockedMode;
    private final int maDelay;
    private SstvMode mode;
    private boolean modeNotified;
    private double nextLineExact;
    private double noiseFloor;
    private int pendingNext;
    private final float[] powerRing;
    private int[] prevU;
    private int[] prevV;
    private int progressEvery;
    private final int rate;
    private final int rawAdvance;
    private final float[] rawRing;
    private boolean rawUsed;
    private final int ringCap;
    private final int ringMask;
    private final int sampleRate;
    private int sampleShift;
    private int scanPos;
    private float signalLevel;
    private final FrontEnd.Sink sink;
    private double snrDb = -30.0d;
    private int state;
    private int[] tempB;
    private int[] tempG;
    private int[] tempR;
    private int[] tempU;
    private int[] tempV;
    private int[] tempY;
    private int[] tempY2;
    private int toneEnd;
    private int toneLen;
    private final float[][] toneRing;
    private int toneStart;
    private int width;
    private int writePos;

    public interface Listener {
        void onComplete(Frame frame, SstvMode sstvMode);

        void onError(String str);

        void onModeDetected(SstvMode sstvMode);

        void onProgress(Frame frame, int i, SstvMode sstvMode);
    }

    private static double clampD(double d, double d2, double d3) {
        return d < d2 ? d2 : d > d3 ? d3 : d;
    }

    public Decoder(int i, Listener listener) {
        FrontEnd.Sink sink = new FrontEnd.Sink() { // from class: om.sstvsuite.engine.Decoder.1
            @Override // om.sstvsuite.engine.FrontEnd.Sink
            public void onSample(float f, float f2, float f3, float f4, float[] fArr) {
                Decoder.this.onFrontSample(f, f2, f3, f4, fArr);
            }
        };
        this.sink = sink;
        if (i <= 0) {
            throw new IllegalArgumentException("bad sampleRate: " + i);
        }
        this.sampleRate = i;
        this.listener = listener;
        FrontEnd frontEnd = new FrontEnd(i, sink);
        this.front = frontEnd;
        int i2 = frontEnd.workRate;
        this.rate = i2;
        int max = Math.max(3, (int) Math.round(i2 * 0.001d));
        this.freqAvg = new Dsp.MovingAverage(max);
        int i3 = (max - 1) / 2;
        this.maDelay = i3;
        this.rawAdvance = frontEnd.lpfGroupDelay() + i3;
        Dsp.FmDemodulator fmDemodulator = new Dsp.FmDemodulator(i);
        this.hilbertDemod = fmDemodulator;
        int max2 = Math.max(3, (int) Math.round(i2 * 5.0E-4d));
        this.hilbertAvg = new Dsp.MovingAverage(max2);
        this.hilbShift = ((fmDemodulator.groupDelay() - frontEnd.lpfGroupDelay()) - frontEnd.decimationDelay()) + (((max2 - 1) / 2) - i3);
        int i4 = 1;
        while (i4 < (this.rate * 4) + 4096) {
            i4 <<= 1;
        }
        this.ringCap = i4;
        this.ringMask = i4 - 1;
        this.hilbRing = new float[i4];
        this.freqRing = new float[i4];
        this.rawRing = new float[i4];
        this.ampRing = new float[i4];
        this.powerRing = new float[i4];
        this.toneRing = (float[][]) Array.newInstance((Class<?>) Float.TYPE, 6, i4);
        reset();
    }

    public void setLockedMode(SstvMode sstvMode) {
        this.lockedMode = sstvMode;
        reset();
    }

    public void reset() {
        this.front.reset();
        this.freqAvg.reset();
        for (int i = 0; i < this.ringCap; i++) {
            this.freqRing[i] = 0.0f;
            this.rawRing[i] = 0.0f;
            this.hilbRing[i] = 0.0f;
            this.ampRing[i] = 0.0f;
            this.powerRing[i] = 0.0f;
        }
        for (int i2 = 0; i2 < 6; i2++) {
            for (int i3 = 0; i3 < this.ringCap; i3++) {
                this.toneRing[i2][i3] = 0.0f;
            }
        }
        this.writePos = 0;
        this.lastFreq = 0.0f;
        this.signalLevel = 0.0f;
        this.noiseFloor = 0.0d;
        this.freqOffset = 0.0d;
        this.sampleShift = -Math.max(0, (int) Math.round(this.sampleRate * 4.5E-5d));
        this.snrDb = -30.0d;
        this.scanPos = 0;
        this.leaderEnd = 0;
        this.lastSearchedTo = 0;
        this.toneStart = 0;
        this.toneEnd = 0;
        this.toneLen = 0;
        this.lineStart = 0;
        this.pendingNext = -1;
        this.lineIndex = 0;
        this.linesDone = 0;
        this.width = 0;
        this.height = 0;
        PlanGeom planGeom = null;
        this.image = null;
        this.progressEvery = 16;
        this.lastProgressLines = 0;
        this.complete = false;
        this.modeNotified = false;
        this.endOfStream = false;
        SstvMode sstvMode = this.lockedMode;
        this.mode = sstvMode;
        if (sstvMode != null) {
            SstvMode sstvMode2 = this.lockedMode;
            int i4 = this.sampleRate;
            planGeom = new PlanGeom(sstvMode2, i4, i4 / this.rate);
        }
        this.geom = planGeom;
        this.state = this.lockedMode != null ? 4 : 0;
    }

    public void feed(short[] sArr, int i, int i2) {
        if (sArr == null) {
            throw new IllegalArgumentException("pcm == null");
        }
        if (i < 0 || i2 < 0 || i + i2 > sArr.length) {
            throw new IllegalArgumentException("bad pcm range");
        }
        if (this.complete) {
            return;
        }
        for (int i3 = 0; i3 < i2; i3++) {
            float f = sArr[i + i3] * 3.0517578E-5f;
            this.front.push(f);
            this.hilbRing[this.writePos & this.ringMask] = this.hilbertAvg.push(this.hilbertDemod.push(f));
            if (this.complete) {
                return;
            }
        }
        processAvailable();
    }

    public void flush() {
        if (this.complete) {
            return;
        }
        this.endOfStream = CSMOOTH_WIDE;
        processAvailable();
        if (this.complete || this.image == null || this.linesDone < this.height) {
            return;
        }
        finishImage();
    }

    public boolean isComplete() {
        return this.complete;
    }

    public SstvMode getMode() {
        return this.mode;
    }

    public Frame getImage() {
        if (this.image == null || this.width <= 0) {
            return null;
        }
        return new Frame(this.width, this.height, this.image);
    }

    public float getSignalFrequency() {
        return this.lastFreq;
    }

    public float getSignalLevel() {
        return this.signalLevel;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFrontSample(float f, float f2, float f3, float f4, float[] fArr) {
        float push = this.freqAvg.push(f);
        this.lastFreq = push;
        int i = this.writePos & this.ringMask;
        this.freqRing[i] = push;
        this.rawRing[i] = f4;
        this.ampRing[i] = f2;
        this.powerRing[i] = f3;
        for (int i2 = 0; i2 < 6; i2++) {
            this.toneRing[i2][i] = fArr[i2];
        }
        double d = f3;
        double d2 = this.noiseFloor;
        if (d < d2) {
            this.noiseFloor = d2 + ((d - d2) * 0.02d);
        } else {
            this.noiseFloor = d2 + ((d - d2) * 5.0E-9d);
        }
        int i3 = this.writePos + 1;
        this.writePos = i3;
        if ((i3 & 63) == 0) {
            this.signalLevel = (float) Math.min(1.0d, f2 * 2.2d);
            processAvailable();
        }
    }

    private float freqAt(int i) {
        return this.freqRing[i & this.ringMask];
    }

    private float powerAt(int i) {
        return this.powerRing[i & this.ringMask];
    }

    private double ratioAt(int i, int i2) {
        int i3 = i2 & this.ringMask;
        return this.toneRing[i][i3] / (this.powerRing[i3] + 1.0E-12f);
    }

    private double avgRatio(int i, int i2, int i3) {
        int ringFirst = ringFirst();
        if (i2 < ringFirst) {
            i3 -= ringFirst - i2;
            i2 = ringFirst;
        }
        int i4 = this.writePos - i2;
        if (i3 <= 0 || i4 <= 0) {
            return 0.0d;
        }
        if (i3 > i4) {
            i3 = i4;
        }
        double d = 0.0d;
        double d2 = 0.0d;
        for (int i5 = 0; i5 < i3; i5++) {
            int i6 = (i2 + i5) & this.ringMask;
            d2 += this.toneRing[i][i6];
            d += this.powerRing[i6];
        }
        if (d <= 1.0E-12d) {
            return 0.0d;
        }
        return d2 / d;
    }

    private double weightedMeanFreq(int i, int i2) {
        int ringFirst = ringFirst();
        if (i < ringFirst) {
            i2 -= ringFirst - i;
            i = ringFirst;
        }
        int i3 = this.writePos - i;
        if (i2 <= 0 || i3 <= 0) {
            return 0.0d;
        }
        if (i2 > i3) {
            i2 = i3;
        }
        double d = 0.0d;
        double d2 = 0.0d;
        for (int i4 = 0; i4 < i2; i4++) {
            float f = this.ampRing[(i + i4) & this.ringMask];
            double d3 = (f * f) + 1.0E-9d;
            d2 += this.freqRing[r7] * d3;
            d += d3;
        }
        if (d <= 1.0E-12d) {
            return 0.0d;
        }
        return d2 / d;
    }

    private int ringFirst() {
        int i = this.writePos;
        return (i - Math.min(i, this.ringCap)) + 2;
    }

    private double meanPower(int i, int i2) {
        int ringFirst = ringFirst();
        if (i < ringFirst) {
            i2 -= ringFirst - i;
            i = ringFirst;
        }
        int i3 = this.writePos - i;
        double d = 0.0d;
        if (i2 <= 0 || i3 <= 0) {
            return 0.0d;
        }
        if (i2 > i3) {
            i2 = i3;
        }
        for (int i4 = 0; i4 < i2; i4++) {
            d += powerAt(i + i4);
        }
        return d / i2;
    }

    private boolean signalPresent() {
        int max = Math.max(8, this.rate / 200);
        if (meanPower(this.writePos - max, max) > this.noiseFloor * 2.0d) {
            return CSMOOTH_WIDE;
        }
        return false;
    }

    private void processAvailable() {
        boolean stepSearchLeader;
        int i = 0;
        while (true) {
            int i2 = this.state;
            if (i2 == 6) {
                return;
            }
            int i3 = i + 1;
            if (i >= 8192) {
                return;
            }
            if (i2 == 0) {
                stepSearchLeader = stepSearchLeader();
            } else if (i2 == 1) {
                stepSearchLeader = stepVisParse();
            } else if (i2 == 2) {
                stepSearchLeader = stepFindLineStart();
            } else if (i2 == 3) {
                stepSearchLeader = stepWraaseCheck();
            } else if (i2 == 4) {
                stepSearchLeader = stepSearchSync();
            } else if (i2 != 5) {
                return;
            } else {
                stepSearchLeader = stepDecode();
            }
            if (!stepSearchLeader) {
                return;
            } else {
                i = i3;
            }
        }
    }

    private int findToneRunEnergy(int i, int i2, int i3, int i4, int i5, boolean z) {
        int ringFirst = ringFirst();
        if (i < ringFirst) {
            i = ringFirst;
        }
        int min = Math.min(i5, this.writePos);
        while (i + i3 <= min) {
            if (ratioAt(i2, i) > TONE_TH) {
                int i6 = i;
                while (i6 < this.writePos && ratioAt(i2, i6) > TONE_TH && (i4 <= 0 || i6 - i < i4)) {
                    i6++;
                }
                int i7 = i6 - i;
                if (i7 >= i3) {
                    if (z && i6 >= this.writePos - 2) {
                        this.toneStart = i;
                        this.toneEnd = i6;
                        this.toneLen = i7;
                        this.lastSearchedTo = i;
                        return -1;
                    }
                    this.toneStart = i;
                    this.toneEnd = i6;
                    this.toneLen = i7;
                    return i;
                }
                if (i6 >= this.writePos - 1) {
                    this.toneStart = i;
                    this.toneEnd = i6;
                    this.toneLen = i7;
                    this.lastSearchedTo = i;
                    return -1;
                }
                i = i6;
            } else {
                i++;
            }
        }
        this.lastSearchedTo = i;
        return -1;
    }

    private void updateSnrEstimate(int i, int i2, double d) {
        int i3 = i - this.rawAdvance;
        int i4 = i3 + i2;
        int ringFirst = ringFirst();
        if (i3 < ringFirst) {
            i3 = ringFirst;
        }
        int i5 = this.writePos;
        if (i4 > i5) {
            i4 = i5;
        }
        int i6 = i4 - i3;
        if (i6 < 8) {
            return;
        }
        int i7 = i3 + 2;
        double d2 = 0.0d;
        double d3 = 0.0d;
        for (int i8 = i7; i8 < i4; i8++) {
            double d4 = this.rawRing[(i8 - 1) & this.ringMask];
            d3 += (r14[i8 & r5] + r14[r5 & (i8 - 2)]) * d4;
            d2 += d4 * 2.0d * d4;
        }
        double d5 = 1.0E-12d;
        if (d2 <= 1.0E-12d) {
            return;
        }
        double d6 = d3 / d2;
        double d7 = 0.0d;
        while (i7 < i4) {
            float[] fArr = this.rawRing;
            int i9 = this.ringMask;
            double d8 = d5;
            double d9 = (fArr[i7 & i9] - ((d6 * 2.0d) * fArr[(i7 - 1) & i9])) + fArr[(i7 - 2) & i9];
            d7 += d9 * d9;
            i7++;
            d5 = d8;
        }
        double d10 = d5;
        double d11 = (d7 / (i6 - 2)) / (((4.0d * d6) * d6) + 2.0d);
        double d12 = 0.0d;
        for (int i10 = 0; i10 < i6; i10++) {
            double d13 = this.rawRing[(i3 + i10) & this.ringMask];
            d12 += d13 * d13;
        }
        double d14 = (d12 / i6) - d11;
        if (d11 < d10) {
            this.snrDb = 90.0d;
            return;
        }
        if (d14 <= 0.0d) {
            return;
        }
        double log10 = Math.log10(d14 / d11) * 10.0d;
        double d15 = log10 <= 90.0d ? log10 : 90.0d;
        if (d15 < -30.0d) {
            d15 = -30.0d;
        }
        double d16 = this.snrDb;
        if (d16 > -29.0d) {
            d15 = (d16 + d15) * 0.5d;
        }
        this.snrDb = d15;
    }

    private int findFreqTransition(int i, int i2, double d, double d2) {
        int i3 = this.writePos;
        boolean z = CSMOOTH_WIDE;
        int min = Math.min(i2, i3 - 1);
        double d3 = ((d + d2) * 0.5d) + this.freqOffset;
        if (d2 <= d) {
            z = false;
        }
        for (int max = Math.max(i, ringFirst()); max <= min; max++) {
            double freqAt = freqAt(max);
            if (z) {
                if (freqAt > d3) {
                    return max;
                }
            } else {
                if (freqAt < d3) {
                    return max;
                }
            }
        }
        return -1;
    }

    private boolean stepSearchLeader() {
        if (!signalPresent()) {
            this.scanPos = Math.max(this.scanPos, this.writePos - ((int) (this.rate * TONE_TH)));
            return false;
        }
        int max = Math.max(8, (int) Math.round(this.rate * 0.15d));
        if (findToneRunEnergy(this.scanPos, 4, max, Math.max(max + 1, (int) Math.round(this.rate * 0.45d)), this.writePos, false) < 0) {
            this.scanPos = Math.max(this.scanPos, this.lastSearchedTo);
            return false;
        }
        int i = this.toneStart;
        int i2 = this.toneLen;
        double weightedMeanFreq = weightedMeanFreq(i + (i2 / 4), Math.max(4, i2 / 2)) - LEADER_FREQ;
        if (Math.abs(weightedMeanFreq) > 220.0d) {
            this.scanPos = this.toneEnd;
            return CSMOOTH_WIDE;
        }
        this.freqOffset = clampD(weightedMeanFreq, -320.0d, 320.0d);
        int i3 = this.toneStart;
        int i4 = this.toneLen;
        updateSnrEstimate(i3 + (i4 / 4), i4 / 2, LEADER_FREQ);
        int findFreqTransition = findFreqTransition((this.toneLen / 2) + this.toneStart, ((int) (this.rate * 0.03d)) + this.toneEnd, LEADER_FREQ, SYNC_FREQ);
        if (findFreqTransition < 0) {
            this.scanPos = Math.max(this.scanPos, this.toneStart);
            return false;
        }
        this.leaderEnd = findFreqTransition;
        this.state = 1;
        return CSMOOTH_WIDE;
    }

    private boolean stepVisParse() {
        int i;
        double d = 0.62d;
        int i2 = 0;
        if (this.writePos < this.leaderEnd + ((int) Math.round(this.rate * 0.62d))) {
            return false;
        }
        double avgRatio = avgRatio(4, this.leaderEnd + ((int) Math.round(this.rate * 0.06d)), (int) Math.round(this.rate * 0.2d));
        double d2 = TONE_TH_LOW;
        if (avgRatio < TONE_TH_LOW) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.3d)));
            return CSMOOTH_WIDE;
        }
        if (avgRatio(1, this.leaderEnd + ((int) Math.round(this.rate * 0.312d)), Math.max(3, (int) Math.round(this.rate * 0.016d))) < TONE_TH_LOW) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.32d)));
            return CSMOOTH_WIDE;
        }
        int max = Math.max(4, (int) Math.round(this.rate * 0.016d));
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while (i3 < 7) {
            double d3 = d;
            double d4 = d2;
            int round = (this.leaderEnd + ((int) Math.round((Math.round(((i3 * VIS_BIT_MS) + VIS_BIT0_START) + 15.0d) * this.rate) / 1000.0d))) - (max / 2);
            double avgRatio2 = avgRatio(2, round, max);
            double avgRatio3 = avgRatio(0, round, max);
            if (avgRatio2 > avgRatio3 && avgRatio2 > d4) {
                i = 1;
            } else {
                if (avgRatio3 <= avgRatio2 || avgRatio3 <= d4) {
                    failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.36d)));
                    return CSMOOTH_WIDE;
                }
                i = 0;
            }
            i5 |= i << i3;
            i4 ^= i;
            i3++;
            d = d3;
            d2 = d4;
        }
        double d5 = d;
        double d6 = d2;
        int i6 = max / 2;
        int round2 = (this.leaderEnd + ((int) Math.round((this.rate * 565.0d) / 1000.0d))) - i6;
        double avgRatio4 = avgRatio(2, round2, max);
        double avgRatio5 = avgRatio(0, round2, max);
        if (avgRatio4 > avgRatio5 && avgRatio4 > d6) {
            i2 = 1;
        } else if (avgRatio5 <= avgRatio4 || avgRatio5 <= d6) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.56d)));
            return CSMOOTH_WIDE;
        }
        if (i2 != i4) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.57d)));
            return CSMOOTH_WIDE;
        }
        if (avgRatio(1, (this.leaderEnd + ((int) Math.round((this.rate * 595.0d) / 1000.0d))) - i6, max) < 0.064d) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * 0.6d)));
            return CSMOOTH_WIDE;
        }
        SstvMode byVis = ModeRegistry.byVis(i5);
        if (byVis == null) {
            failVis(this.leaderEnd + ((int) Math.round(this.rate * d5)));
            return CSMOOTH_WIDE;
        }
        this.mode = byVis;
        this.state = 2;
        return CSMOOTH_WIDE;
    }

    private boolean stepFindLineStart() {
        int round = (int) Math.round((this.mode.plans[0][0].durationMs * this.rate) / 1000.0d);
        int round2 = this.leaderEnd + ((int) Math.round((this.rate * VIS_STOP_START) / 1000.0d));
        int findFreqTransition = findFreqTransition(round2, round2 + ((int) Math.round(this.rate * 0.15d)), SYNC_FREQ, (this.mode.plans[0].length <= 1 || this.mode.plans[0][1].kind == 3) ? BLACK_FREQ : this.mode.plans[0][1].frequency);
        if (findFreqTransition < 0) {
            if (this.writePos - round2 <= ((int) Math.round(this.rate * 0.15d))) {
                return false;
            }
            failVis(round2);
            return CSMOOTH_WIDE;
        }
        int i = findFreqTransition - round;
        this.lineStart = i >= 0 ? i : 0;
        if (this.mode.vis == 55 && this.mode.id.equals("sc2_180")) {
            this.state = 3;
            return CSMOOTH_WIDE;
        }
        beginDecode(this.mode);
        this.state = 5;
        return CSMOOTH_WIDE;
    }

    private boolean stepWraaseCheck() {
        SstvMode byId;
        if (this.writePos < this.lineStart + ((int) Math.round(this.rate * 0.82d))) {
            return false;
        }
        int round = this.lineStart + ((int) Math.round(this.rate * 0.35d));
        int min = Math.min(this.writePos, this.lineStart + ((int) Math.round(this.rate * 0.85d)));
        int max = Math.max(3, (int) Math.round(this.rate * 0.004d));
        int findToneRunEnergy = findToneRunEnergy(round, 1, max, Math.max(max + 1, (int) Math.round(this.rate * 0.03d)), min, CSMOOTH_WIDE);
        SstvMode sstvMode = this.mode;
        if (findToneRunEnergy >= 0 && ((this.toneStart - this.lineStart) * 1000.0d) / this.rate < 590.0d && (byId = ModeRegistry.byId("sc2_120")) != null) {
            sstvMode = byId;
        }
        this.mode = sstvMode;
        beginDecode(sstvMode);
        this.state = 5;
        return CSMOOTH_WIDE;
    }

    private boolean stepSearchSync() {
        double d;
        int i;
        if (!signalPresent()) {
            this.scanPos = Math.max(this.scanPos, this.writePos - ((int) (this.rate * 0.15d)));
            return false;
        }
        SstvMode sstvMode = this.mode;
        if (sstvMode == null) {
            return false;
        }
        LineRun[] lineRunArr = sstvMode.plans[0];
        int round = (int) Math.round((lineRunArr[0].durationMs * this.rate) / 1000.0d);
        int max = Math.max(3, (int) Math.round(round * 0.55d));
        if (findToneRunEnergy(this.scanPos, 1, max, Math.max(max + 1, (int) Math.round(this.rate * 0.06d)), this.writePos, CSMOOTH_WIDE) < 0) {
            this.scanPos = Math.max(this.scanPos, this.lastSearchedTo);
            return false;
        }
        if (lineRunArr.length <= 1 || lineRunArr[1].kind == 3) {
            d = BLACK_FREQ;
            i = 0;
        } else {
            d = lineRunArr[1].frequency;
            i = (int) Math.round((lineRunArr[1].durationMs * this.rate) / 1000.0d);
        }
        double d2 = d;
        int findFreqTransition = findFreqTransition(this.toneStart + Math.max(2, this.toneLen / 2), this.toneEnd + ((int) (this.rate * 0.03d)), SYNC_FREQ, d2);
        if (findFreqTransition < 0) {
            this.scanPos = Math.max(this.scanPos, this.toneStart);
            return false;
        }
        if (i > 0) {
            int i2 = i / 2;
            if (Math.max(3, i2) + findFreqTransition + 2 > this.writePos) {
                return false;
            }
            if (Math.abs(weightedMeanFreq(Math.max(2, i / 4) + findFreqTransition, Math.max(3, i2)) - d2) > 320.0d) {
                this.scanPos = findFreqTransition;
                return CSMOOTH_WIDE;
            }
        }
        double weightedMeanFreq = weightedMeanFreq(this.toneStart + Math.max(2, this.toneLen / 4), Math.max(3, this.toneLen / 2));
        if (Math.abs(weightedMeanFreq - SYNC_FREQ) < 260.0d) {
            updateOffset(weightedMeanFreq);
        }
        updateSnrEstimate(this.toneStart + Math.max(4, this.toneLen / 4), Math.max(8, this.toneLen / 2), SYNC_FREQ);
        int i3 = findFreqTransition - round;
        if (i3 < 0) {
            i3 = 0;
        }
        this.lineStart = i3;
        beginDecode(this.mode);
        int min = Math.min(this.linesDone, this.geom.transmitLines - 1);
        this.lineIndex = min;
        if (min < 0) {
            this.lineIndex = 0;
        }
        this.state = 5;
        return CSMOOTH_WIDE;
    }

    private static int toneForFreq(double d) {
        if (Math.abs(d - 1100.0d) < 1.0d) {
            return 0;
        }
        if (Math.abs(d - SYNC_FREQ) < 1.0d) {
            return 1;
        }
        if (Math.abs(d - 1300.0d) < 1.0d) {
            return 2;
        }
        if (Math.abs(d - BLACK_FREQ) < 1.0d) {
            return 3;
        }
        return Math.abs(d - LEADER_FREQ) < 1.0d ? 4 : 5;
    }

    private boolean stepDecode() {
        int syncSamples = this.geom.syncSamples();
        int i = this.pendingNext;
        if (i >= 0) {
            if (this.writePos < i + syncSamples) {
                return false;
            }
            this.pendingNext = -1;
            int i2 = (syncSamples / 4) + i;
            int i3 = syncSamples / 2;
            if (avgRatio(1, i2, Math.max(4, i3)) > TONE_TH_LOW) {
                double weightedMeanFreq = weightedMeanFreq(i2, Math.max(4, i3));
                if (Math.abs(weightedMeanFreq - SYNC_FREQ) < 260.0d) {
                    updateOffset(weightedMeanFreq);
                }
                this.lineStart = i;
                return CSMOOTH_WIDE;
            }
            int findSyncPulse = findSyncPulse(i - ((int) Math.round(this.rate * 0.02d)), ((int) Math.round(this.geom.lineSamples * 0.5d)) + i);
            if (findSyncPulse >= 0) {
                this.lineStart = findSyncPulse;
                this.nextLineExact = findSyncPulse + this.geom.lineSamplesExact;
                return CSMOOTH_WIDE;
            }
            if (findSyncPulse == -2) {
                this.pendingNext = i;
                return CSMOOTH_WIDE;
            }
            this.state = 4;
            this.scanPos = i;
            return CSMOOTH_WIDE;
        }
        int i4 = this.lineStart + this.geom.lineSamples;
        if (this.writePos < i4) {
            if (!this.endOfStream) {
                return false;
            }
            if (this.writePos < i4 - Math.max(8, (int) Math.round(this.rate * 0.004d))) {
                return false;
            }
        }
        decodeLine();
        int i5 = this.lineIndex + 1;
        this.lineIndex = i5;
        if (i5 >= this.geom.transmitLines) {
            finishImage();
            return CSMOOTH_WIDE;
        }
        this.pendingNext = (int) Math.round(this.nextLineExact);
        this.nextLineExact += this.geom.lineSamplesExact;
        return CSMOOTH_WIDE;
    }

    private void decodeLine() {
        int i;
        LineRun[] planForLine = this.mode.planForLine(this.lineIndex);
        int length = (this.lineIndex & 1) % this.mode.plans.length;
        int i2 = this.geom.pd ? this.lineIndex * 2 : this.lineIndex;
        for (int i3 = 0; i3 < planForLine.length; i3++) {
            if (planForLine[i3].kind == 3) {
                int i4 = this.lineStart + this.geom.start[length][i3];
                int i5 = this.geom.len[length][i3];
                int[] targetFor = targetFor(planForLine[i3].channel, length, i3);
                sampleScan(i4, i5, targetFor);
                char c = planForLine[i3].channel;
                if ((c == 'U' || c == 'V') && (i = this.width) >= 8) {
                    sharpenChroma(targetFor, i);
                }
            }
        }
        fillRows(i2);
    }

    private int[] targetFor(char c, int i, int i2) {
        if (c == 'R') {
            return this.tempR;
        }
        if (c == 'G') {
            return this.tempG;
        }
        if (c == 'B') {
            return this.tempB;
        }
        if (c == 'U') {
            return this.tempU;
        }
        if (c == 'V') {
            return this.tempV;
        }
        if (this.geom.pd) {
            return this.geom.rowDelta[i][i2] == 0 ? this.tempY : this.tempY2;
        }
        if (this.geom.alternating) {
            return (this.lineIndex & 1) == 0 ? this.tempY : this.tempY2;
        }
        return this.tempY;
    }

    private void sampleScan(int i, int i2, int[] iArr) {
        double d;
        double d2;
        int i3 = i;
        int i4 = i2;
        int i5 = this.width;
        int ringFirst = ringFirst();
        int i6 = this.sampleShift;
        int i7 = 0;
        while (i7 < i5) {
            int i8 = i3 + i6;
            double d3 = i7;
            double d4 = i4;
            int i9 = i7;
            int i10 = 0;
            double d5 = i5;
            int round = ((int) Math.round(((0.15d + d3) * d4) / d5)) + i8;
            int i11 = i5;
            int round2 = ((int) Math.round(((d3 + 0.85d) * d4) / d5)) + i8;
            if (round < i3) {
                round = i3;
            }
            int i12 = i3 + i2;
            if (round2 > i12) {
                round2 = i12;
            }
            if (round2 <= round) {
                round2 = round + 1;
            }
            if (round < ringFirst) {
                round = ringFirst;
            }
            int i13 = this.writePos;
            if (round2 > i13) {
                round2 = i13;
            }
            int i14 = round2 - round;
            if (i14 <= 0) {
                iArr[i9] = 0;
            } else {
                double d6 = 0.0d;
                double d7 = 0.0d;
                double d8 = 0.0d;
                while (i10 < i14) {
                    float f = this.ampRing[(round + i10) & this.ringMask];
                    int i15 = round2;
                    double d9 = (f * f) + 1.0E-9d;
                    d8 += d9 * this.freqRing[r1];
                    d7 += d9;
                    i10++;
                    round2 = i15;
                    i14 = i14;
                }
                int i16 = round2;
                double d10 = d7 > 1.0E-12d ? d8 / d7 : 0.0d;
                int round3 = (i8 - this.rawAdvance) + ((int) Math.round(((d3 + 0.05d) * d4) / d5));
                int round4 = (i8 - this.rawAdvance) + ((int) Math.round(((d3 + 0.95d) * d4) / d5));
                if (round3 < ringFirst) {
                    round3 = ringFirst;
                }
                int i17 = this.writePos;
                if (round4 > i17) {
                    round4 = i17;
                }
                int i18 = round4 - round3;
                if (i18 < 6 || this.snrDb <= 38.0d) {
                    d = 2800.0d;
                    d2 = 700.0d;
                } else {
                    double d11 = 0.0d;
                    double d12 = 0.0d;
                    for (int i19 = round3 + 2; i19 < round4; i19++) {
                        double d13 = this.rawRing[(i19 - 1) & this.ringMask];
                        d12 += (r15[i19 & r6] + r15[(i19 - 2) & r6]) * d13;
                        d11 += 2.0d * d13 * d13;
                    }
                    d = 2800.0d;
                    d2 = 700.0d;
                    if (d11 > 1.0E-12d) {
                        double d14 = d12 / d11;
                        double d15 = 0.0d;
                        for (int i20 = 0; i20 < i18; i20++) {
                            double d16 = this.rawRing[(round3 + i20) & this.ringMask];
                            d15 += d16 * d16;
                        }
                        double d17 = d15 / i18;
                        double d18 = 1.0d;
                        double pow = d17 - (d17 / (Math.pow(10.0d, this.snrDb / 10.0d) + 1.0d));
                        if (pow > 0.1d * d17) {
                            double d19 = (d14 * d17) / pow;
                            if (d19 <= 1.0d) {
                                d18 = -1.0d;
                                if (d19 >= -1.0d) {
                                    d18 = d19;
                                }
                            }
                            double acos = (Math.acos(d18) * this.rate) / 6.283185307179586d;
                            if (acos > 700.0d && acos < 2800.0d) {
                                this.rawUsed = CSMOOTH_WIDE;
                                d10 = acos;
                            }
                        }
                    }
                }
                if (!this.rawUsed && this.snrDb > 15.0d) {
                    int i21 = this.hilbShift;
                    int i22 = round + i21;
                    int i23 = i16 + i21;
                    if (i22 < ringFirst) {
                        i22 = ringFirst;
                    }
                    int i24 = this.writePos;
                    if (i23 > i24) {
                        i23 = i24;
                    }
                    int i25 = i23 - i22;
                    if (i25 >= 2) {
                        for (int i26 = 0; i26 < i25; i26++) {
                            d6 += this.hilbRing[(i22 + i26) & this.ringMask];
                        }
                        double d20 = d6 / i25;
                        if (d20 > d2 && d20 < d) {
                            d10 = d20;
                        }
                    }
                }
                this.rawUsed = false;
                int round5 = (int) Math.round((((d10 - this.freqOffset) - BLACK_FREQ) * 255.0d) / 800.0d);
                iArr[i9] = round5 < 0 ? 0 : round5 > 255 ? 255 : round5;
            }
            i7 = i9 + 1;
            i3 = i;
            i4 = i2;
            i5 = i11;
        }
    }

    private void sharpenChroma(int[] iArr, int i) {
        if (i < 5) {
            return;
        }
        double d = this.snrDb;
        int i2 = d >= 14.0d ? d < 24.0d ? 4 : d < 34.0d ? 3 : 2 : 5;
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            int[] iArr2 = new int[i];
            iArr2[i3] = iArr[i3];
            int i5 = i - 1;
            iArr2[i5] = iArr[i5];
            int i6 = 1;
            int i7 = 1;
            while (i7 < i5) {
                int i8 = iArr[i7 - 1];
                int i9 = iArr[i7];
                int i10 = i7 + 1;
                int i11 = iArr[i10];
                int abs = Math.abs(i8 - i9);
                int abs2 = Math.abs(i11 - i9);
                if (abs <= 26 && abs2 <= 26) {
                    int i12 = iArr[i7 > i6 ? i7 - 2 : 0];
                    int i13 = i7 + 2;
                    if (i13 >= i) {
                        i13 = i5;
                    }
                    int i14 = iArr[i13];
                    if (Math.abs(i12 - i9) <= 26 && Math.abs(i14 - i9) <= 26) {
                        iArr2[i7] = (((((i12 + (i8 * 2)) + (i9 * 3)) + (i11 * 2)) + i14) + 4) / 9;
                    } else {
                        iArr2[i7] = (((i8 + (i9 * 2)) + i11) + 2) >> 2;
                    }
                } else {
                    iArr2[i7] = i9;
                }
                i7 = i10;
                i3 = 0;
                i6 = 1;
            }
            System.arraycopy(iArr2, i3, iArr, i3, i);
        }
    }

    private int findSyncPulse(int i, int i2) {
        int syncSamples = this.geom.syncSamples();
        int max = Math.max(3, (int) Math.round(syncSamples * 0.55d));
        if (findToneRunEnergy(i, 1, max, Math.max(max + 1, (int) Math.round(this.rate * 0.06d)), i2, CSMOOTH_WIDE) < 0) {
            return -1;
        }
        double porchFrequency = this.geom.porchFrequency();
        int porchSamples = this.geom.porchSamples();
        int findFreqTransition = findFreqTransition(Math.max(2, this.toneLen / 2) + this.toneStart, ((int) (this.rate * 0.03d)) + this.toneEnd, SYNC_FREQ, porchFrequency);
        if (findFreqTransition < 0) {
            return -2;
        }
        if (porchSamples > 0) {
            int i3 = porchSamples / 2;
            if (Math.max(3, i3) + findFreqTransition + 2 > this.writePos) {
                return -2;
            }
            if (Math.abs(weightedMeanFreq(Math.max(2, porchSamples / 4) + findFreqTransition, Math.max(3, i3)) - porchFrequency) > 320.0d) {
                return -1;
            }
        }
        double weightedMeanFreq = weightedMeanFreq(this.toneStart + Math.max(2, this.toneLen / 4), Math.max(3, this.toneLen / 2));
        if (Math.abs(weightedMeanFreq - SYNC_FREQ) < 260.0d) {
            updateOffset(weightedMeanFreq);
        }
        return findFreqTransition - syncSamples;
    }

    private void fillRows(int i) {
        if (this.geom.rgb) {
            if (i >= 0 && i < this.height) {
                int i2 = i * this.width;
                for (int i3 = 0; i3 < this.width; i3++) {
                    this.image[i2 + i3] = Yuv.rgbToArgb(this.tempR[i3], this.tempG[i3], this.tempB[i3]);
                }
            }
            addLines(1);
            return;
        }
        if (this.geom.pd) {
            fillYuvRows(i, i + 1, this.tempY, this.tempY2);
            addLines(2);
            return;
        }
        if (this.geom.alternating) {
            if ((this.lineIndex & 1) == 1) {
                if (this.hasPrevChroma) {
                    for (int i4 = 0; i4 < this.width; i4++) {
                        int[] iArr = this.tempU;
                        iArr[i4] = ((iArr[i4] + this.prevU[i4]) + 1) >> 1;
                        int[] iArr2 = this.tempV;
                        iArr2[i4] = ((iArr2[i4] + this.prevV[i4]) + 1) >> 1;
                    }
                }
                System.arraycopy(this.tempU, 0, this.prevU, 0, this.width);
                System.arraycopy(this.tempV, 0, this.prevV, 0, this.width);
                this.hasPrevChroma = CSMOOTH_WIDE;
                fillYuvRows(i - 1, i, this.tempY, this.tempY2);
                addLines(2);
                return;
            }
            return;
        }
        int[] iArr3 = this.tempY;
        fillYuvRows(i, i, iArr3, iArr3);
        addLines(1);
    }

    private void fillYuvRows(int i, int i2, int[] iArr, int[] iArr2) {
        if (i >= 0 && i < this.height) {
            int i3 = this.width * i;
            for (int i4 = 0; i4 < this.width; i4++) {
                this.image[i3 + i4] = Yuv.toArgb(iArr[i4], this.tempU[i4], this.tempV[i4]);
            }
        }
        if (i2 < 0 || i2 >= this.height || i2 == i) {
            return;
        }
        int i5 = i2 * this.width;
        for (int i6 = 0; i6 < this.width; i6++) {
            this.image[i5 + i6] = Yuv.toArgb(iArr2[i6], this.tempU[i6], this.tempV[i6]);
        }
    }

    private void addLines(int i) {
        int i2;
        int i3 = this.linesDone + i;
        this.linesDone = i3;
        int i4 = this.height;
        if (i3 > i4) {
            this.linesDone = i4;
        }
        if (this.listener == null || this.mode == null || (i2 = this.linesDone) >= i4 || i2 < this.lastProgressLines + this.progressEvery) {
            return;
        }
        this.lastProgressLines = i2;
        int[] iArr = this.image;
        int[] iArr2 = new int[iArr.length];
        System.arraycopy(iArr, 0, iArr2, 0, iArr.length);
        this.listener.onProgress(new Frame(this.width, this.height, iArr2), this.linesDone, this.mode);
    }

    private void finishImage() {
        this.complete = CSMOOTH_WIDE;
        this.state = 6;
        Listener listener = this.listener;
        if (listener == null || this.mode == null) {
            return;
        }
        listener.onComplete(new Frame(this.width, this.height, this.image), this.mode);
    }

    private void beginDecode(SstvMode sstvMode) {
        this.mode = sstvMode;
        int i = this.sampleRate;
        this.geom = new PlanGeom(sstvMode, i, i / this.rate);
        this.sampleShift = Math.abs(((sstvMode.plans[0].length <= 1 || sstvMode.plans[0][1].kind == 3) ? BLACK_FREQ : sstvMode.plans[0][1].frequency) - LEADER_FREQ) < 1.0d ? -Math.max(0, (int) Math.round(this.sampleRate * 4.5E-5d)) : 0;
        this.hasPrevChroma = false;
        this.nextLineExact = this.lineStart + this.geom.lineSamplesExact;
        if (this.image == null || this.width != sstvMode.width || this.height != sstvMode.height) {
            this.width = sstvMode.width;
            int i2 = sstvMode.height;
            this.height = i2;
            this.image = new int[this.width * i2];
            int i3 = 0;
            while (true) {
                int[] iArr = this.image;
                if (i3 >= iArr.length) {
                    break;
                }
                iArr[i3] = -16777216;
                i3++;
            }
            int i4 = this.width;
            this.tempR = new int[i4];
            this.tempG = new int[i4];
            this.tempB = new int[i4];
            this.tempY = new int[i4];
            this.tempY2 = new int[i4];
            this.tempU = new int[i4];
            this.tempV = new int[i4];
            this.prevU = new int[i4];
            this.prevV = new int[i4];
            this.hasPrevChroma = false;
            this.linesDone = 0;
            this.lineIndex = 0;
            this.pendingNext = -1;
            this.progressEvery = Math.max(8, this.height / 16);
            this.lastProgressLines = 0;
        } else {
            int min = Math.min(this.linesDone, this.geom.transmitLines - 1);
            this.lineIndex = min;
            if (min < 0) {
                this.lineIndex = 0;
            }
        }
        if (this.modeNotified) {
            return;
        }
        this.modeNotified = CSMOOTH_WIDE;
        Listener listener = this.listener;
        if (listener != null) {
            listener.onModeDetected(sstvMode);
        }
    }

    private void failVis(int i) {
        this.state = 0;
        this.scanPos = Math.max(0, i);
    }

    private void updateOffset(double d) {
        double d2 = d - SYNC_FREQ;
        if (Math.abs(d2) > 320.0d) {
            return;
        }
        this.freqOffset = clampD(this.freqOffset + (d2 * 0.15d), -320.0d, 320.0d);
    }
}
