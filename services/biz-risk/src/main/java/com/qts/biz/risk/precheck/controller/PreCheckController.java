package com.qts.biz.risk.precheck.controller;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;
import com.qts.biz.risk.precheck.dto.PreCheckResponse;
import com.qts.biz.risk.precheck.PreCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.time.LocalDateTime;

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

        // Use PreCheckService.check() which uses injected services to fetch real-time data
        PreCheckResponse response = preCheckService.check(request);

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("Pre-check request completed in {}ms, result: canTrade={}, code={}",
                    elapsed, response.getData().isCanTrade(), response.getCode());

        return response;
    }
}