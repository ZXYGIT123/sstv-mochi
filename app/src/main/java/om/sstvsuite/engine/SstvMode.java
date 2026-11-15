package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class SstvMode {
    public final int height;
    public final String id;
    public final double lineMs;
    public final String name;
    public final LineRun[][] plans;
    public final boolean robotColor;
    public final int vis;
    public final int width;

    public SstvMode(String str, String str2, int i, int i2, int i3, double d, LineRun[][] lineRunArr, boolean z) {
        this.id = str;
        this.name = str2;
        this.vis = i;
        this.width = i2;
        this.height = i3;
        this.lineMs = d;
        this.plans = lineRunArr;
        this.robotColor = z;
    }

    public LineRun[] planForLine(int i) {
        LineRun[][] lineRunArr = this.plans;
        return lineRunArr[(i & 1) % lineRunArr.length];
    }

    public String toString() {
        return this.name;
    }
}
