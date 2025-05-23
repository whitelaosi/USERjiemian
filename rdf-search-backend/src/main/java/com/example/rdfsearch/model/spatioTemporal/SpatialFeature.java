
package com.example.rdfsearch.model.spatioTemporal;

/**
 * 空间特征
 */
public class SpatialFeature {
    private double x;
    private double y;
    private double z;
    private String spatialReference; // 空间参考系
    private String geometryType; // 几何类型
    private Object geometry; // 几何对象

    public SpatialFeature() {
    }

    public SpatialFeature(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getters and Setters
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(String spatialReference) {
        this.spatialReference = spatialReference;
    }

    public String getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(String geometryType) {
        this.geometryType = geometryType;
    }

    public Object getGeometry() {
        return geometry;
    }

    public void setGeometry(Object geometry) {
        this.geometry = geometry;
    }
}