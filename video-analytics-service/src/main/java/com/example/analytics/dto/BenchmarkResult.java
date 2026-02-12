package com.example.analytics.dto;

public class BenchmarkResult {
    private String format;
    private int messageSize;
    private long deserializationTimeNs;
    private long serializationTimeNs;

    public BenchmarkResult() {
    }

    public BenchmarkResult(String format, int messageSize, 
                          long deserializationTimeNs, long serializationTimeNs) {
        this.format = format;
        this.messageSize = messageSize;
        this.deserializationTimeNs = deserializationTimeNs;
        this.serializationTimeNs = serializationTimeNs;
    }

    // Getters and Setters
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public long getDeserializationTimeNs() {
        return deserializationTimeNs;
    }

    public void setDeserializationTimeNs(long deserializationTimeNs) {
        this.deserializationTimeNs = deserializationTimeNs;
    }

    public long getSerializationTimeNs() {
        return serializationTimeNs;
    }

    public void setSerializationTimeNs(long serializationTimeNs) {
        this.serializationTimeNs = serializationTimeNs;
    }
}
