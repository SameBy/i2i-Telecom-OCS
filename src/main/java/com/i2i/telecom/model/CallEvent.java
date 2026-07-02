package com.i2i.telecom.model;

public class CallEvent {
    private String msisdn;
    private String destination;
    private boolean isInternational;
    private int durationInMinutes;
    private String status;
    private String rejectReason;
    private long timestamp;

    public CallEvent() {}

    public CallEvent(String msisdn, String destination, boolean isInternational, int durationInMinutes, String status, String rejectReason) {
        this.msisdn = msisdn;
        this.destination = destination;
        this.isInternational = isInternational;
        this.durationInMinutes = durationInMinutes;
        this.status = status;
        this.rejectReason = rejectReason;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public boolean isInternational() { return isInternational; }
    public void setInternational(boolean international) { isInternational = international; }

    public int getDurationInMinutes() { return durationInMinutes; }
    public void setDurationInMinutes(int durationInMinutes) { this.durationInMinutes = durationInMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}