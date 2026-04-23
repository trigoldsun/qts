package com.qts.biz.risk.precheck;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;
import com.qts.biz.risk.precheck.dto.PreCheckResponse;
import com.qts.biz.risk.precheck.service.AssetService;
import com.qts.biz.risk.precheck.service.MarketDataService;
import com.qts.biz.risk.precheck.service.PositionService;
import com.qts.biz.risk.precheck.validator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PreCheck Service - Risk pre-trade validation
 * 
 * Validates orders against risk rules before execution:
 * - Position limit (single stock <= 20% of total assets)
 * - Fund check (available funds >= order amount * margin rate)
 * - Trading session (9:15-9:25, 9:30-11:30, 13:00-14:57)
 * - Price limit (±10% of previous close)
 * 
 * P99 latency requirement: <= 5ms
 */
@Service
public class PreCheckService {

    private static final Logger logger = LoggerFactory.getLogger(PreCheckService.class);

    private final List<PreCheckValidator> validators;
    private final PositionService positionService;
    private final AssetService assetService;
    private final MarketDataService marketDataService;

    @Autowired
    public PreCheckService(PositionService positionService,
                          AssetService assetService,
                          MarketDataService marketDataService) {
        this.validators = new ArrayList<>();
        this.positionService = positionService;
        this.assetService = assetService;
        this.marketDataService = marketDataService;
        // Initialize validators in order
        this.validators.add(new TradingSessionValidator());
        this.validators.add(new FundValidator());
        this.validators.add(new PositionLimitValidator());
        this.validators.add(new PriceLimitValidator());
    }

    /**
     * Perform pre-check validation
     * @param request Pre-check request with order details
     * @return PreCheckResponse with validation result
     */
    public PreCheckResponse check(PreCheckRequest request) {
        long startTime = System.nanoTime();
        
        logger.info("Starting pre-check for accountId={}, symbol={}, side={}", 
                    request.getAccountId(), request.getSymbol(), request.getSide());

        PreCheckResponse response = new PreCheckResponse();
        PreCheckResponse.PreCheckData data = new PreCheckResponse.PreCheckData();
        data.setCanTrade(true);
        response.setData(data);

        List<String> reasons = new ArrayList<>();

        // Build validation context
        ValidationContext context = buildContext(request);

        // Run all validators
        for (PreCheckValidator validator : validators) {
            try {
                PreCheckValidator.ValidationResult result = validator.validate(request, context);
                if (!result.isPassed()) {
                    data.setCanTrade(false);
                    reasons.add("[" + validator.getRuleName() + "] " + result.getReason());
                    logger.debug("Validation failed for {}: {}", validator.getRuleName(), result.getReason());
                }
            } catch (Exception e) {
                logger.error("Validator {} threw exception: {}", validator.getRuleName(), e.getMessage());
                data.setCanTrade(false);
                reasons.add("[" + validator.getRuleName() + "] validation error: " + e.getMessage());
            }
        }

        data.setReasons(reasons);

        if (data.isCanTrade()) {
            response.setCode(0);
            response.setMessage("success");
            logger.info("Pre-check passed for accountId={}, symbol={}", request.getAccountId(), request.getSymbol());
        } else {
            response.setCode(1);
            response.setMessage("precheck failed");
            logger.warn("Pre-check failed for accountId={}, symbol={}, reasons={}", 
                        request.getAccountId(), request.getSymbol(), reasons);
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("Pre-check completed in {}ms", elapsed);

        return response;
    }

    /**
     * Build validation context from external data sources
     * Uses injected services to fetch real-time data
     */
    private ValidationContext buildContext(PreCheckRequest request) {
        Long accountId = request.getAccountId();
        String symbol = request.getSymbol();

        // Fetch position data from PositionService
        Map<String, BigDecimal> positionMap = new HashMap<>();
        positionMap.put(symbol, positionService.getPosition(accountId, symbol));

        // Fetch asset data from AssetService
        BigDecimal totalAssets = assetService.getTotalAssets(accountId);
        BigDecimal availableCash = assetService.getAvailableCash(accountId);

        // Fetch price data from MarketDataService
        Map<String, BigDecimal> priceMap = new HashMap<>();
        BigDecimal previousClose = marketDataService.getPreviousClose(symbol);
        if (previousClose != null) {
            priceMap.put(symbol, previousClose);
        }

        return new ValidationContext.Builder()
                .accountId(accountId)
                .positionMap(positionMap)
                .priceMap(priceMap)
                .totalAssets(totalAssets)
                .availableCash(availableCash)
                .currentTime(LocalDateTime.now())
                .build();
    }

    /**
     * Perform pre-check with pre-fetched context data (for optimized performance)
     * Use this method when context data is already available to avoid redundant calls
     */
    public PreCheckResponse check(PreCheckRequest request, ValidationContext context) {
        long startTime = System.nanoTime();

        logger.info("Starting pre-check with context for accountId={}, symbol={}, side={}", 
                    request.getAccountId(), request.getSymbol(), request.getSide());

        PreCheckResponse response = new PreCheckResponse();
        PreCheckResponse.PreCheckData data = new PreCheckResponse.PreCheckData();
        data.setCanTrade(true);
        response.setData(data);

        List<String> reasons = new ArrayList<>();

        // Run all validators
        for (PreCheckValidator validator : validators) {
            try {
                PreCheckValidator.ValidationResult result = validator.validate(request, context);
                if (!result.isPassed()) {
                    data.setCanTrade(false);
                    reasons.add("[" + validator.getRuleName() + "] " + result.getReason());
                }
            } catch (Exception e) {
                logger.error("Validator {} threw exception: {}", validator.getRuleName(), e.getMessage());
                data.setCanTrade(false);
                reasons.add("[" + validator.getRuleName() + "] validation error: " + e.getMessage());
            }
        }

        data.setReasons(reasons);

        if (data.isCanTrade()) {
            response.setCode(0);
            response.setMessage("success");
        } else {
            response.setCode(1);
            response.setMessage("precheck failed");
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("Pre-check completed in {}ms", elapsed);

        return response;
    }
}