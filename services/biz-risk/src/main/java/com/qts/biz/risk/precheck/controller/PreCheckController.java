package com.qts.biz.risk.precheck.controller;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;
import com.qts.biz.risk.precheck.dto.PreCheckResponse;
import com.qts.biz.risk.precheck.validator.ValidationContext;
import com.qts.biz.risk.precheck.PreCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Risk Pre-Check API
 * POST /api/v1/risk/precheck
 */
@RestController
@RequestMapping("/api/v1/risk")
public class PreCheckController {

    private static final Logger logger = LoggerFactory.getLogger(PreCheckController.class);

    private final PreCheckService preCheckService;

    @Autowired
    public PreCheckController(PreCheckService preCheckService) {
        this.preCheckService = preCheckService;
    }

    /**
     * Pre-check endpoint for risk validation
     * 
     * @param request Pre-check request containing order details
     * @return PreCheckResponse with validation result
     */
    @PostMapping("/precheck")
    public PreCheckResponse preCheck(@RequestBody PreCheckRequest request) {
        long startTime = System.nanoTime();
        
        logger.info("Received pre-check request: accountId={}, symbol={}, side={}, price={}, quantity={}",
                    request.getAccountId(), request.getSymbol(), request.getSide(),
                    request.getPrice(), request.getQuantity());

        // Build validation context with external data
        // In production, this would inject services to get real-time data
        ValidationContext context = buildContext(request);

        PreCheckResponse response = preCheckService.check(request, context);

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("Pre-check request completed in {}ms, result: canTrade={}, code={}",
                    elapsed, response.getData().isCanTrade(), response.getCode());

        return response;
    }

    /**
     * Build validation context from external services
     * TODO: Inject actual services (PositionService, AssetService, MarketDataService)
     */
    private ValidationContext buildContext(PreCheckRequest request) {
        // Mock data - in production, fetch from services
        Map<String, BigDecimal> positionMap = new HashMap<>();
        positionMap.put(request.getSymbol(), BigDecimal.ZERO); // No existing position

        Map<String, BigDecimal> priceMap = new HashMap<>();
        // Set previous close price for price limit validation
        if (request.getPrice() != null && request.getSymbol() != null) {
            // This is a mock - in production, get from market data service
            priceMap.put(request.getSymbol(), request.getPrice()); 
        }

        // Mock asset data - in production, get from AssetService
        BigDecimal totalAssets = new BigDecimal("1000000"); // 100万 total assets
        BigDecimal availableCash = new BigDecimal("500000"); // 50万 available

        return new ValidationContext.Builder()
                .accountId(request.getAccountId())
                .positionMap(positionMap)
                .priceMap(priceMap)
                .totalAssets(totalAssets)
                .availableCash(availableCash)
                .currentTime(LocalDateTime.now())
                .build();
    }
}