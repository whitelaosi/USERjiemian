
package com.example.rdfsearch.model.spatioTemporal;

/**
 * 粒度层级
 */
public enum GranularityLevel {
    FINE("细粒度", 1),
    MEDIUM("中粒度", 2),
    COARSE("粗粒度", 3);

    private String name;
    private int level;

    GranularityLevel(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public static GranularityLevel fromLevel(int level) {
        for (GranularityLevel g : values()) {
            if (g.level == level) {
                return g;
            }
        }
        return MEDIUM; // 默认返回中粒度
    }
}