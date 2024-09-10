package com.Views;

import java.util.HashMap;
import java.util.Map;

public class PaintUnit {
    public enum CoordinateType {
        DIS11, DIS12, DIS2, BRK
    }

    private final Map<CoordinateType, Integer> xCoordinates = new HashMap<>();
    private final Map<CoordinateType, Integer> yCoordinates = new HashMap<>();

    public PaintUnit() {
        // Initialize all coordinates to -999 by default
        for (CoordinateType type : CoordinateType.values()) {
            xCoordinates.put(type, -999);
            yCoordinates.put(type, -999);
        }
    }

    public int getX(CoordinateType type) {
        return xCoordinates.getOrDefault(type, -999);
    }

    public void setX(CoordinateType type, int x) {
        xCoordinates.put(type, x);
    }

    public int getY(CoordinateType type) {
        return yCoordinates.getOrDefault(type, -999);
    }

    public void setY(CoordinateType type, int y) {
        yCoordinates.put(type, y);
    }
}
