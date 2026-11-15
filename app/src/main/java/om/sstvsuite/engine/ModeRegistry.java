package om.sstvsuite.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class ModeRegistry {
    private static final List<SstvMode> ALL = new ArrayList();

    static {
        List<SstvMode> createAll = Modes.createAll();
        for (int i = 0; i < createAll.size(); i++) {
            register(createAll.get(i));
        }
    }

    private ModeRegistry() {
    }

    static void register(SstvMode sstvMode) {
        ALL.add(sstvMode);
    }

    public static List<SstvMode> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static SstvMode byId(String str) {
        if (str == null) {
            return null;
        }
        String alias = alias(str);
        int i = 0;
        while (true) {
            List<SstvMode> list = ALL;
            if (i >= list.size()) {
                return null;
            }
            SstvMode sstvMode = list.get(i);
            if (sstvMode.id.equals(alias)) {
                return sstvMode;
            }
            i++;
        }
    }

    private static String alias(String str) {
        if (str.equals("wraase120") || str.equals("wraase_sc2_120")) {
            return "sc2_120";
        }
        if (str.equals("wraase180") || str.equals("wraase_sc2_180")) {
            return "sc2_180";
        }
        if (str.equals("scottie_dx")) {
            return "scottiedx";
        }
        if (str.equals("robot_36") || str.equals("robot36color")) {
            return "robot36";
        }
        return (str.equals("robot_72") || str.equals("robot72color")) ? "robot72" : str;
    }

    public static SstvMode byVis(int i) {
        int i2 = 0;
        while (true) {
            List<SstvMode> list = ALL;
            if (i2 >= list.size()) {
                return null;
            }
            SstvMode sstvMode = list.get(i2);
            if (sstvMode.vis == i) {
                return sstvMode;
            }
            i2++;
        }
    }
}
