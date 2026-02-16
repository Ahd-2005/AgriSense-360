package utils;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentListRefresh {
    private static final List<Runnable> listeners = new ArrayList<>();

    private EquipmentListRefresh() {
    }

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void notifyEquipmentChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
