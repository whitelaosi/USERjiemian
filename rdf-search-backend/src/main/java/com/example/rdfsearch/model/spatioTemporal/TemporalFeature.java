
package com.example.rdfsearch.model.spatioTemporal;

import java.util.Date;

/**
 * 时间特征
 */
public class TemporalFeature {
    private Date startTime;
    private Date endTime;
    private Long duration; // 持续时间（毫秒）
    private String temporalType; // 时间类型（瞬时/持续/周期）

    public TemporalFeature() {
    }

    public TemporalFeature(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.duration = endTime.getTime() - startTime.getTime();
        }
    }

    // Getters and Setters
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
        updateDuration();
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        updateDuration();
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getTemporalType() {
        return temporalType;
    }

    public void setTemporalType(String temporalType) {
        this.temporalType = temporalType;
    }

    private void updateDuration() {
        if (startTime != null && endTime != null) {
            this.duration = endTime.getTime() - startTime.getTime();
        }
    }
}