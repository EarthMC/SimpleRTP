package dev.warriorrr.simplertp.model;

import dev.warriorrr.simplertp.collection.WeightedCollection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Region {
    private final String name;
    private final List<Area> areas;
    private final WeightedCollection<Area> weightedAreas = new WeightedCollection<>();

    public Region(String name, List<Area> areas) {
        this.name = name;
        this.areas = Collections.unmodifiableList(areas);

        for (final Area area : areas) {
            weightedAreas.add(area.size(), area);
        }
    }

    public Area getRandomArea() {
        return this.weightedAreas.next();
    }

    public String name() {
        return this.name;
    }

    public List<Area> areas() {
        return this.areas;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Region region)) return false;
        return Objects.equals(name, region.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
