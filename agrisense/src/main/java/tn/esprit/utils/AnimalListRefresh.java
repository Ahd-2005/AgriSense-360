package tn.esprit.utils;

import java.util.ArrayList;
import java.util.List;

public final class AnimalListRefresh {
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void notifyAnimalChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
