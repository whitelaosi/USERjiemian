package com.example.rdfsearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RdfInstance {
    private String uri;
    private Float length;
    private Integer microseismicEventCount;
    private Float energyRelease;
    private Float cumulativeApparentVolume;
    private String dailyEventRate;
    private Float dailyEnergyRelease;
    private Float dailyApparentVolumeRate;
    private Integer serialNumber;
    private String mileage;
    private String startMileage;
    private String endMileage;
    private String description;
    private String riskType;

    // 用于简化输出的属性
    @JsonProperty("name")
    private String simplifiedName;

    public RdfInstance() {
    }

    public RdfInstance(String uri, Float length) {
        this.uri = uri;
        this.length = length;
        this.simplifiedName = simplifyUri(uri);
    }

    // 从URI中提取简化名称
    private String simplifyUri(String uri) {
        // 如果URI包含#，取其后部分，否则取最后一个/后的部分
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        } else if (uri.contains("/")) {
            return uri.substring(uri.lastIndexOf("/") + 1);
        }
        return uri;
    }

    // Getters and Setters
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
        this.simplifiedName = simplifyUri(uri);
    }

    public Float getLength() {
        return length;
    }

    public void setLength(Float length) {
        this.length = length;
    }

    public Integer getMicroseismicEventCount() {
        return microseismicEventCount;
    }

    public void setMicroseismicEventCount(Integer microseismicEventCount) {
        this.microseismicEventCount = microseismicEventCount;
    }

    public Float getEnergyRelease() {
        return energyRelease;
    }

    public void setEnergyRelease(Float energyRelease) {
        this.energyRelease = energyRelease;
    }

    public Float getCumulativeApparentVolume() {
        return cumulativeApparentVolume;
    }

    public void setCumulativeApparentVolume(Float cumulativeApparentVolume) {
        this.cumulativeApparentVolume = cumulativeApparentVolume;
    }

    public String getDailyEventRate() {
        return dailyEventRate;
    }

    public void setDailyEventRate(String dailyEventRate) {
        this.dailyEventRate = dailyEventRate;
    }

    public Float getDailyEnergyRelease() {
        return dailyEnergyRelease;
    }

    public void setDailyEnergyRelease(Float dailyEnergyRelease) {
        this.dailyEnergyRelease = dailyEnergyRelease;
    }

    public Float getDailyApparentVolumeRate() {
        return dailyApparentVolumeRate;
    }

    public void setDailyApparentVolumeRate(Float dailyApparentVolumeRate) {
        this.dailyApparentVolumeRate = dailyApparentVolumeRate;
    }

    public Integer getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Integer serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public String getStartMileage() {
        return startMileage;
    }

    public void setStartMileage(String startMileage) {
        this.startMileage = startMileage;
    }

    public String getEndMileage() {
        return endMileage;
    }

    public void setEndMileage(String endMileage) {
        this.endMileage = endMileage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRiskType() {
        return riskType;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    public String getSimplifiedName() {
        return simplifiedName;
    }
}