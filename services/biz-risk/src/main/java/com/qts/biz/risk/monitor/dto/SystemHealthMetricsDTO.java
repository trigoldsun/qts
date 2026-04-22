package com.qts.biz.risk.monitor.dto;

import java.time.LocalDateTime;

/**
 * System Health Metrics DTO
 * JVM, connection pool, thread pool metrics
 */
public class SystemHealthMetricsDTO {

    private LocalDateTime timestamp;
    
    // JVM metrics
    private Double jvmHeapUsedMB;
    private Double jvmHeapMaxMB;
    private Double jvmHeapUsagePercent;
    private Long jvmGcCount;
    private String jvmGcType;
    
    // Thread metrics
    private Integer threadCount;
    private Integer peakThreadCount;
    private Integer daemonThreadCount;
    
    // Connection pool metrics
    private Integer hikariActiveConnections;
    private Integer hikariIdleConnections;
    private Integer hikariMaxConnections;
    private Long hikariWaitMillis;
    
    // CPU metrics
    private Double systemCpuUsage;
    private Double processCpuUsage;

    public SystemHealthMetricsDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getJvmHeapUsedMB() {
        return jvmHeapUsedMB;
    }

    public void setJvmHeapUsedMB(Double jvmHeapUsedMB) {
        this.jvmHeapUsedMB = jvmHeapUsedMB;
    }

    public Double getJvmHeapMaxMB() {
        return jvmHeapMaxMB;
    }

    public void setJvmHeapMaxMB(Double jvmHeapMaxMB) {
        this.jvmHeapMaxMB = jvmHeapMaxMB;
    }

    public Double getJvmHeapUsagePercent() {
        return jvmHeapUsagePercent;
    }

    public void setJvmHeapUsagePercent(Double jvmHeapUsagePercent) {
        this.jvmHeapUsagePercent = jvmHeapUsagePercent;
    }

    public Long getJvmGcCount() {
        return jvmGcCount;
    }

    public void setJvmGcCount(Long jvmGcCount) {
        this.jvmGcCount = jvmGcCount;
    }

    public String getJvmGcType() {
        return jvmGcType;
    }

    public void setJvmGcType(String jvmGcType) {
        this.jvmGcType = jvmGcType;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Integer getPeakThreadCount() {
        return peakThreadCount;
    }

    public void setPeakThreadCount(Integer peakThreadCount) {
        this.peakThreadCount = peakThreadCount;
    }

    public Integer getDaemonThreadCount() {
        return daemonThreadCount;
    }

    public void setDaemonThreadCount(Integer daemonThreadCount) {
        this.daemonThreadCount = daemonThreadCount;
    }

    public Integer getHikariActiveConnections() {
        return hikariActiveConnections;
    }

    public void setHikariActiveConnections(Integer hikariActiveConnections) {
        this.hikariActiveConnections = hikariActiveConnections;
    }

    public Integer getHikariIdleConnections() {
        return hikariIdleConnections;
    }

    public void setHikariIdleConnections(Integer hikariIdleConnections) {
        this.hikariIdleConnections = hikariIdleConnections;
    }

    public Integer getHikariMaxConnections() {
        return hikariMaxConnections;
    }

    public void setHikariMaxConnections(Integer hikariMaxConnections) {
        this.hikariMaxConnections = hikariMaxConnections;
    }

    public Long getHikariWaitMillis() {
        return hikariWaitMillis;
    }

    public void setHikariWaitMillis(Long hikariWaitMillis) {
        this.hikariWaitMillis = hikariWaitMillis;
    }

    public Double getSystemCpuUsage() {
        return systemCpuUsage;
    }

    public void setSystemCpuUsage(Double systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }

    public Double getProcessCpuUsage() {
        return processCpuUsage;
    }

    public void setProcessCpuUsage(Double processCpuUsage) {
        this.processCpuUsage = processCpuUsage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SystemHealthMetricsDTO m = new SystemHealthMetricsDTO();

        public Builder jvmHeapUsedMB(Double val) { m.jvmHeapUsedMB = val; return this; }
        public Builder jvmHeapMaxMB(Double val) { m.jvmHeapMaxMB = val; return this; }
        public Builder jvmHeapUsagePercent(Double val) { m.jvmHeapUsagePercent = val; return this; }
        public Builder jvmGcCount(Long val) { m.jvmGcCount = val; return this; }
        public Builder jvmGcType(String val) { m.jvmGcType = val; return this; }
        public Builder threadCount(Integer val) { m.threadCount = val; return this; }
        public Builder peakThreadCount(Integer val) { m.peakThreadCount = val; return this; }
        public Builder daemonThreadCount(Integer val) { m.daemonThreadCount = val; return this; }
        public Builder hikariActiveConnections(Integer val) { m.hikariActiveConnections = val; return this; }
        public Builder hikariIdleConnections(Integer val) { m.hikariIdleConnections = val; return this; }
        public Builder hikariMaxConnections(Integer val) { m.hikariMaxConnections = val; return this; }
        public Builder hikariWaitMillis(Long val) { m.hikariWaitMillis = val; return this; }
        public Builder systemCpuUsage(Double val) { m.systemCpuUsage = val; return this; }
        public Builder processCpuUsage(Double val) { m.processCpuUsage = val; return this; }

        public SystemHealthMetricsDTO build() {
            return m;
        }
    }
}
