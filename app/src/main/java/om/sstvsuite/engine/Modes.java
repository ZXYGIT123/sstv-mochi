package om.sstvsuite.engine;

import java.util.ArrayList;
import java.util.List;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
final class Modes {
    private Modes() {
    }

    static List<SstvMode> createAll() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(rgbMode("martin1", "Martin 1", 44, 320, 256, new double[]{4.862d, 0.572d, 0.572d}, 146.432d, new char[]{'G', 'B', 'R'}, 0));
        arrayList.add(rgbMode("martin2", "Martin 2", 40, 320, 256, new double[]{4.862d, 0.572d, 0.572d}, 73.216d, new char[]{'G', 'B', 'R'}, 0));
        arrayList.add(rgbMode("scottie1", "Scottie 1", 60, 320, 256, new double[]{9.0d, 1.5d, 1.5d}, 138.24d, new char[]{'R', 'G', 'B'}, 1));
        arrayList.add(rgbMode("scottie2", "Scottie 2", 56, 320, 256, new double[]{9.0d, 1.5d, 1.5d}, 88.064d, new char[]{'R', 'G', 'B'}, 1));
        arrayList.add(rgbMode("scottiedx", "Scottie DX", 76, 320, 256, new double[]{9.0d, 1.5d, 1.5d}, 345.6d, new char[]{'R', 'G', 'B'}, 1));
        arrayList.add(rgbMode("sc2_180", "Wraase SC2-180", 55, 320, 256, new double[]{5.5225d, 0.5d, 0.0d}, 235.0d, new char[]{'R', 'G', 'B'}, 2));
        arrayList.add(rgbMode("sc2_120", "Wraase SC2-120", 55, 320, 256, new double[]{5.5225d, 0.5d, 0.0d}, 152.8d, new char[]{'R', 'G', 'B'}, 2));
        arrayList.add(robot36());
        arrayList.add(robot72());
        arrayList.add(pd("pd50", "PD 50", 93, 320, 256, 91.52d));
        arrayList.add(pd("pd90", "PD 90", 99, 320, 256, 170.24d));
        arrayList.add(pd("pd120", "PD 120", 95, 640, 496, 121.6d));
        arrayList.add(pd("pd160", "PD 160", 98, 512, 400, 195.584d));
        arrayList.add(pd("pd180", "PD 180", 96, 640, 496, 183.04d));
        arrayList.add(pd("pd240", "PD 240", 97, 640, 496, 244.48d));
        arrayList.add(pd("pd290", "PD 290", 94, 800, 616, 228.8d));
        arrayList.add(hfFax());
        return arrayList;
    }

    private static SstvMode rgbMode(String str, String str2, int i, int i2, int i3, double[] dArr, double d, char[] cArr, int i4) {
        PlanBuilder planBuilder = new PlanBuilder();
        planBuilder.fixed(0, dArr[0], 1200.0d);
        planBuilder.fixed(1, dArr[1], 1500.0d);
        for (int i5 = 0; i5 < cArr.length; i5++) {
            planBuilder.scan(cArr[i5], d);
            if (i4 == 0) {
                planBuilder.fixed(2, dArr[2], 1500.0d);
            } else if (i4 == 1 && i5 < cArr.length - 1) {
                planBuilder.fixed(2, dArr[2], 1500.0d);
            }
        }
        LineRun[] build = planBuilder.build();
        return new SstvMode(str, str2, i, i2, i3, planBuilder.time, new LineRun[][]{build, build}, false);
    }

    private static SstvMode robot36() {
        PlanBuilder planBuilder = new PlanBuilder();
        planBuilder.fixed(0, 9.0d, 1200.0d);
        planBuilder.fixed(1, 3.0d, 1500.0d);
        planBuilder.scan('Y', 88.0d);
        planBuilder.fixed(2, 4.5d, 1500.0d);
        planBuilder.fixed(1, 1.5d, 1900.0d);
        planBuilder.scan('V', 44.0d);
        LineRun[] build = planBuilder.build();
        PlanBuilder planBuilder2 = new PlanBuilder();
        planBuilder2.fixed(0, 9.0d, 1200.0d);
        planBuilder2.fixed(1, 3.0d, 1500.0d);
        planBuilder2.scan('Y', 88.0d);
        planBuilder2.fixed(2, 4.5d, 2300.0d);
        planBuilder2.fixed(1, 1.5d, 1900.0d);
        planBuilder2.scan('U', 44.0d);
        return new SstvMode("robot36", "Robot 36", 8, 320, 240, planBuilder.time, new LineRun[][]{build, planBuilder2.build()}, true);
    }

    private static SstvMode robot72() {
        PlanBuilder planBuilder = new PlanBuilder();
        planBuilder.fixed(0, 9.0d, 1200.0d);
        planBuilder.fixed(1, 3.0d, 1500.0d);
        planBuilder.scan('Y', 138.0d);
        planBuilder.fixed(2, 4.5d, 1500.0d);
        planBuilder.fixed(1, 1.5d, 1900.0d);
        planBuilder.scan('V', 69.0d);
        planBuilder.fixed(2, 4.5d, 2300.0d);
        planBuilder.fixed(1, 1.5d, 1900.0d);
        planBuilder.scan('U', 69.0d);
        LineRun[] build = planBuilder.build();
        return new SstvMode("robot72", "Robot 72", 12, 320, 240, planBuilder.time, new LineRun[][]{build, build}, true);
    }

    /**
     * HF Fax（WEFAX）：没有 VIS 头，每行 500 ms —— 25 ms 的 1200 Hz 行同步 + 475 ms 图像，
     * 图像 1500 Hz(黑) → 2300 Hz(白) 单调调频，640 × 1200 行（与 Robot36 的 HF Fax 解码器对齐）。
     */
    private static SstvMode hfFax() {
        PlanBuilder planBuilder = new PlanBuilder();
        planBuilder.fixed(1, 25.0d, 1200.0d);
        planBuilder.scan('Y', 475.0d);
        LineRun[] build = planBuilder.build();
        return new SstvMode("hffax", "HF Fax", -1, 640, 1200, planBuilder.time, new LineRun[][]{build}, false);
    }

    private static SstvMode pd(String str, String str2, int i, int i2, int i3, double d) {
        PlanBuilder planBuilder = new PlanBuilder();
        planBuilder.fixed(0, 20.0d, 1200.0d);
        planBuilder.fixed(1, 2.08d, 1500.0d);
        planBuilder.scan('Y', d);
        planBuilder.scan('V', d);
        planBuilder.scan('U', d);
        planBuilder.scan('Y', d);
        LineRun[] build = planBuilder.build();
        return new SstvMode(str, str2, i, i2, i3, planBuilder.time, new LineRun[][]{build, build}, true);
    }

    private static final class PlanBuilder {
        private final List<LineRun> runs;
        private double time;

        private PlanBuilder() {
            this.runs = new ArrayList();
        }

        void fixed(int i, double d, double d2) {
            this.runs.add(new LineRun(i, (char) 0, this.time, d, d2));
            this.time += d;
        }

        void scan(char c, double d) {
            this.runs.add(new LineRun(3, c, this.time, d, 0.0d));
            this.time += d;
        }

        LineRun[] build() {
            List<LineRun> list = this.runs;
            return (LineRun[]) list.toArray(new LineRun[list.size()]);
        }
    }
}
