package com.qts.biz.risk.monitor.collector;

import com.qts.biz.risk.monitor.dto.SystemHealthMetricsDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;

/**
 * System Health Metrics Collector
 * Collects JVM, thread, connection pool, and CPU metrics
 */
@Component
public class SystemHealthMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    
    private final JvmMemoryMetrics jvmMemoryMetrics;
    private final JvmGcMetrics jvmGcMetrics;
    private final JvmThreadMetrics jvmThreadMetrics;
    private final ProcessorMetrics processorMetrics;
    private final ThreadMXBean threadMXBean;

    public SystemHealthMetricsCollector(
            MeterRegistry meterRegistry,
            @Autowired(required = false) DataSource dataSource) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        
        this.jvmMemoryMetrics = new JvmMemoryMetrics();
        this.jvmGcMetrics = new JvmGcMetrics();
        this.jvmThreadMetrics = new JvmThreadMetrics();
        this.processorMetrics = new ProcessorMetrics();
        
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Bind metrics to registry
        jvmMemoryMetrics.bindTo(meterRegistry);
        jvmGcMetrics.bindTo(meterRegistry);
        jvmThreadMetrics.bindTo(meterRegistry);
        processorMetrics.bindTo(meterRegistry);
    }

    /**
     * Get current system health metrics
     */
    public SystemHealthMetricsDTO getMetrics() {
        SystemHealthMetricsDTO.SystemHealthMetricsDTOBuilder builder = SystemHealthMetricsDTO.builder();
        
        // JVM Memory metrics
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        double heapUsedMB = heapUsage.getUsed() / (1024.0 * 1024.0);
        double heapMaxMB = heapUsage.getMax() / (1024.0 * 1024.0);
        double heapUsagePercent = heapUsage.getMax() > 0 
            ? (heapUsage.getUsed() * 100.0) / heapUsage.getMax() 
            : 0;
        
        builder.jvmHeapUsedMB(heapUsedMB)
            .jvmHeapMaxMB(heapMaxMB)
            .jvmHeapUsagePercent(heapUsagePercent);
        
        // GC metrics
        builder.jvmGcCount(jvmGcMetrics.getGcCount())
            .jvmGcType(detectGcType());
        
        // Thread metrics
        builder.threadCount(threadMXBean.getThreadCount())
            .peakThreadCount(threadMXBean.getPeakThreadCount())
            .daemonThreadCount(threadMXBean.getDaemonThreadCount());
        
        // HikariCP metrics
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean hikariPoolMXBean = hikariDataSource.getHikariPoolMXBean();
            if (hikariPoolMXBean != null) {
                builder.hikariActiveConnections(hikariPoolMXBean.getActiveConnections())
                    .hikariIdleConnections(hikariPoolMXBean.getIdleConnections())
                    .hikariMaxConnections(hikariPoolMXBean.getTotalConnections())
                    .hikariWaitMillis((long) hikariDataSource.getHikariConfigMXBean().getConnectionTimeout());
            }
        }
        
        // CPU metrics
        double systemCpuUsage = processorMetrics.measure().stream()
            .filter(m -> m.getId().getName().contains("system"))
            .findFirst()
            .map(m -> m.getValue())
            .orElse(0.0);
        
        builder.systemCpuUsage(systemCpuUsage)
            .processCpuUsage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        
        return builder.build();
    }
    
    private String detectGcType() {
        // Simple GC type detection based on memory metrics
        return jvmGcMetrics.getGcCount() > 0 ? "G1GC" : "Unknown";
    }
}
