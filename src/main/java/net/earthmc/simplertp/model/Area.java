package net.earthmc.simplertp.model;

public record Area(int minX, int maxX, int minZ, int maxZ) {
    public int size() {
        return (maxX - minX) * (maxZ - minZ);
    }
}
