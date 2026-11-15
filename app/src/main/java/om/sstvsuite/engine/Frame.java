package om.sstvsuite.engine;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class Frame {
    public final int[] argb;
    public final int height;
    public final int width;

    public Frame(int i, int i2, int[] iArr) {
        if (i <= 0 || i2 <= 0) {
            throw new IllegalArgumentException("bad frame size");
        }
        if (iArr == null || iArr.length != i * i2) {
            throw new IllegalArgumentException("bad argb length");
        }
        this.width = i;
        this.height = i2;
        this.argb = iArr;
    }

    public int get(int i, int i2) {
        return this.argb[(i2 * this.width) + i];
    }

    public Frame copy() {
        return new Frame(this.width, this.height, (int[]) this.argb.clone());
    }
}
