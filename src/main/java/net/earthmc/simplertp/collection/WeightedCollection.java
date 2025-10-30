package net.earthmc.simplertp.collection;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class WeightedCollection<E> {
    private final NavigableMap<Integer, E> map = new TreeMap<>();
    private final Random random;
    private int total = 0;

    public WeightedCollection() {
        this(new Random());
    }

    public WeightedCollection(Random random) {
        this.random = random;
    }

    public void add(int weight, E object) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, object);
    }

    public E next() {
        int value = random.nextInt(total) + 1; // Can also use floating-point weights
        return map.ceilingEntry(value).getValue();
    }
}
