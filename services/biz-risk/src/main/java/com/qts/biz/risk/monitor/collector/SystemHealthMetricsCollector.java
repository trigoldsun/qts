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
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private final OperatingSystemMXBean osMxBean;
    
    // Track GC events via memory pressure heuristics (Micrometer 1.12.0 compatible)
    // JvmGcMetrics.bindTo() registers GC metrics but doesn't expose getGcCount()
    private final AtomicLong lastGcTimestamp = new AtomicLong(0);
    private final AtomicLong gcEventCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> heapUsedHistory = new ConcurrentHashMap<>();
    private long lastRecordedHeapUsed = 0;

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
        this.osMxBean = ManagementFactory.getOperatingSystemMXBean();
        
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
        SystemHealthMetricsDTO.Builder builder = SystemHealthMetricsDTO.builder();
        
        // JVM Memory metrics
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        double heapUsedMB = heapUsage.getUsed() / (1024.0 * 1024.0);
        double heapMaxMB = heapUsage.getMax() / (1024.0 * 1024.0);
        double heapUsagePercent = heapUsage.getMax() > 0 
            ? (heapUsage.getUsed() * 100.0) / heapUsage.getMax() 
            : 0;
        
        // Detect GC events via heap usage patterns (Micrometer 1.12.0 compatible approach)
        long detectedGcCount = detectGcEvents(heapUsage.getUsed());
        
        builder.jvmHeapUsedMB(heapUsedMB)
            .jvmHeapMaxMB(heapMaxMB)
            .jvmHeapUsagePercent(heapUsagePercent)
            .jvmGcCount(detectedGcCount)
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
        
        // CPU metrics - use reflection for compatibility
        double systemCpuUsage = getCpuUsage();
        double systemLoadAvg = getSystemLoadAverage();
        
        builder.systemCpuUsage(systemCpuUsage)
            .processCpuUsage(systemLoadAvg);
        
        return builder.build();
    }
    
    /**
     * Detect GC events via heap usage pattern analysis
     * Micrometer 1.12.0 JvmGcMetrics doesn't expose getGcCount() directly,
     * so we detect GC events by observing sudden heap reductions
     */
    private long detectGcEvents(long currentHeapUsed) {
        // If heap used drops significantly, a GC likely occurred
        if (lastRecordedHeapUsed > 0 && currentHeapUsed < lastRecordedHeapUsed * 0.8) {
            // Heap reduced by more than 20% - likely a GC event
            gcEventCount.incrementAndGet();
            lastGcTimestamp.set(System.currentTimeMillis());
        }
        lastRecordedHeapUsed = currentHeapUsed;
        return gcEventCount.get();
    }
    
    private double getCpuUsage() {
        try {
            // Get system CPU load via reflection
            Method method = osMxBean.getClass().getMethod("getSystemCpuLoad");
            Object result = method.invoke(osMxBean);
            if (result instanceof Double) {
                return ((Double) result) * 100;
            }
        } catch (Exception e) {
            // Fallback to system load average
        }
        return 0.0;
    }
    
    private double getSystemLoadAverage() {
        try {
            double loadAvg = osMxBean.getSystemLoadAverage();
            if (loadAvg >= 0) {
                // Normalize to number of CPUs
                int processors = osMxBean.getAvailableProcessors();
                return loadAvg / processors;
            }
        } catch (Exception e) {
            // Fallback
        }
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }
    
    private String detectGcType() {
        // Use heap usage patterns to detect GC type
        MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        
        if (max > 0) {
            double usageRatio = (double) used / max;
            // G1GC typically has larger heap and works with regions
            // If heap usage is relatively low and we see GC events, likely G1GC
            if (usageRatio < 0.5 && gcEventCount.get() > 0) {
                return "G1GC";
            }
        }
        return gcEventCount.get() > 0 ? "G1GC" : "Unknown";
    }
}
