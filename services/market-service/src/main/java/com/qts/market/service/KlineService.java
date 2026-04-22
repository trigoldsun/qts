package com.qts.market.service;

import com.qts.market.dto.KlineQueryRequest;
import com.qts.market.dto.KlineResponse;
import com.qts.market.model.Kline;
import com.qts.market.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * K线服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KlineService {

    private final KlineRepository klineRepository;

    /**
     * 查询历史K线数据
     */
    @Cacheable(value = "kline", key = "#request.symbol + '_' + #request.period + '_' + #request.count + '_' + #request.adjustment")
    public KlineResponse queryKline(KlineQueryRequest request) {
        log.info("Querying kline for symbol={}, period={}, count={}",
                request.getSymbol(), request.getPeriod(), request.getCount());

        List<Kline> klines;

        if (request.getStartTime() != null || request.getEndTime() != null) {
            // 按时间范围查询
            klines = klineRepository.findBySymbolAndPeriodAndTimeRange(
                    request.getSymbol(),
                    request.getPeriod(),
                    request.getStartTime(),
                    request.getEndTime());
        } else {
            // 按条数查询
            PageRequest pageRequest = PageRequest.of(0, request.getCount());
            klines = klineRepository.findBySymbolAndPeriodOrderByTimestampDesc(
                    request.getSymbol(),
                    request.getPeriod(),
                    pageRequest);
        }

        // 反转列表，按时间正序返回
        List<KlineResponse.KlineData> klineDataList = klines.stream()
                .map(this::toKlineData)
                .collect(Collectors.toList());

        return KlineResponse.builder()
                .symbol(request.getSymbol())
                .period(request.getPeriod())
                .klines(klineDataList)
                .adjustment(request.getAdjustment())
                .build();
    }

    private KlineResponse.KlineData toKlineData(Kline kline) {
        return KlineResponse.KlineData.builder()
                .open(kline.getOpenPrice())
                .high(kline.getHighPrice())
                .low(kline.getLowPrice())
                .close(kline.getClosePrice())
                .volume(kline.getVolume())
                .amount(kline.getAmount())
                .timestamp(kline.getTimestamp())
                .build();
    }
}
