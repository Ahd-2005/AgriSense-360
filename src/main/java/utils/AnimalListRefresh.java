package utils;

import java.util.ArrayList;
import java.util.List;


public final class AnimalListRefresh {

    private static final List<Runnable> listeners = new ArrayList<>();

    public static void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    

    public static void notifyAnimalChanged() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception e) {

            }
        }
    }
}
