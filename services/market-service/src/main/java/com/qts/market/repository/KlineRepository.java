package com.qts.market.repository;

import com.qts.market.model.AdjustmentType;
import com.qts.market.model.Kline;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * K线数据仓储层
 */
@Repository
public interface KlineRepository extends JpaRepository<Kline, Long> {

    /**
     * 按标的、周期和时间范围查询K线
     */
    @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.period = :period " +
           "AND (:startTime IS NULL OR k.timestamp >= :startTime) " +
           "AND (:endTime IS NULL OR k.timestamp <= :endTime) " +
           "ORDER BY k.timestamp DESC")
    List<Kline> findBySymbolAndPeriodAndTimeRange(
            @Param("symbol") String symbol,
            @Param("period") String period,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 按标的、周期查询最近N条K线
     */
    @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.period = :period " +
           "ORDER BY k.timestamp DESC")
    List<Kline> findBySymbolAndPeriodOrderByTimestampDesc(
            @Param("symbol") String symbol,
            @Param("period") String period,
            Pageable pageable);

    /**
     * 按标的查询最新K线
     */
    @Query("SELECT k FROM Kline k WHERE k.symbol = :symbol AND k.period = :period " +
           "ORDER BY k.timestamp DESC LIMIT 1")
    Kline findLatestBySymbolAndPeriod(
            @Param("symbol") String symbol,
            @Param("period") String period);
}
