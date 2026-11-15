package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class LineRun {
    public static final int PORCH = 1;
    public static final int SCAN = 3;
    public static final int SEPARATOR = 2;
    public static final int SYNC = 0;
    public final char channel;
    public final double durationMs;
    public final double frequency;
    public final int kind;
    public final double startMs;

    public LineRun(int i, char c, double d, double d2, double d3) {
        this.kind = i;
        this.channel = c;
        this.startMs = d;
        this.durationMs = d2;
        this.frequency = d3;
    }

    public static LineRun sync(double d, double d2) {
        return new LineRun(0, (char) 0, d, d2, 1200.0d);
    }

    public static LineRun porch(double d, double d2) {
        return new LineRun(1, (char) 0, d, d2, 1500.0d);
    }

    public static LineRun separator(double d, double d2) {
        return new LineRun(2, (char) 0, d, d2, 1500.0d);
    }

    public static LineRun scan(char c, double d, double d2) {
        return new LineRun(3, c, d, d2, 0.0d);
    }

    public String toString() {
        String str;
        int i = this.kind;
        if (i == 3) {
            return "SCAN(" + this.channel + " @" + this.startMs + "ms +" + this.durationMs + "ms)";
        }
        if (i == 0) {
            str = "SYNC";
        } else {
            str = i == 1 ? "PORCH" : "SEP";
        }
        return str + "(@" + this.startMs + "ms +" + this.durationMs + "ms)";
    }
}
