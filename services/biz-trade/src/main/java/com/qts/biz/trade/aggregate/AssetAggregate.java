package com.qts.biz.trade.aggregate;

import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.AssetEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Asset Aggregate Root
 * Encapsulates asset state and business rules for fund management
 */
public class AssetAggregate {

    private static final BigDecimal MARGIN_RATIO = new BigDecimal("0.10"); // 10% margin ratio
    private static final BigDecimal MAINTENANCE_RATIO = new BigDecimal("0.50"); // 50% maintenance ratio
    private static final BigDecimal DANGER_MARGIN_RATE = new BigDecimal("1.30"); // 130% maintenance margin rate
    private static final BigDecimal WARNING_MARGIN_RATE = new BigDecimal("1.50"); // 150% maintenance margin rate

    private final AssetEntity entity;

    public AssetAggregate(AssetEntity entity) {
        this.entity = entity;
    }

    /**
     * Freeze cash for pending order
     * @param amount Amount to freeze
     * @throws IllegalArgumentException if amount is negative or exceeds available cash
     */
    public void freeze(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Freeze amount must be non-negative");
        }
        if (amount.compareTo(entity.getAvailableCash()) > 0) {
            throw new IllegalArgumentException("Insufficient available cash for freeze");
        }
        entity.setAvailableCash(entity.getAvailableCash().subtract(amount));
        entity.setFrozenCash(entity.getFrozenCash().add(amount));
    }

    /**
     * Unfreeze cash when order is cancelled
     * @param amount Amount to unfreeze
     * @throws IllegalArgumentException if amount is negative or exceeds frozen cash
     */
    public void unfreeze(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unfreeze amount must be non-negative");
        }
        if (amount.compareTo(entity.getFrozenCash()) > 0) {
            throw new IllegalArgumentException("Insufficient frozen cash for unfreeze");
        }
        entity.setFrozenCash(entity.getFrozenCash().subtract(amount));
        entity.setAvailableCash(entity.getAvailableCash().add(amount));
    }

    /**
     * Apply trade execution to update asset
     * BUY: Deduct cash, increase market value
     * SELL: Increase cash, decrease market value
     * @param trade Trade execution event
     */
    public void applyTrade(TradeDTO trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }

        BigDecimal amount = trade.getAmount();
        boolean isBuy = "BUY".equalsIgnoreCase(trade.getSide());

        if (isBuy) {
            // For BUY: deduct cash and unfreeze, increase market value
            entity.setAvailableCash(entity.getAvailableCash().subtract(amount));
            entity.setMarketValue(entity.getMarketValue().add(amount));
        } else {
            // For SELL: increase cash and unfreeze, decrease market value
            entity.setAvailableCash(entity.getAvailableCash().add(amount));
            entity.setMarketValue(entity.getMarketValue().subtract(amount));
        }

        // Recalculate totals after trade
        recalculate(entity.getMarketValue());
    }

    /**
     * Recalculate total assets, margin and risk level based on market value
     * @param marketValue Current market value of positions
     */
    public void recalculate(BigDecimal marketValue) {
        // Update market value
        entity.setMarketValue(marketValue != null ? marketValue : BigDecimal.ZERO);

        // Total assets = available cash + frozen cash + market value
        BigDecimal totalAssets = entity.getAvailableCash()
                .add(entity.getFrozenCash())
                .add(entity.getMarketValue());
        entity.setTotalAssets(totalAssets);

        // Margin = market value × margin ratio
        entity.setMargin(entity.getMarketValue().multiply(MARGIN_RATIO));

        // Maintenance margin = margin × maintenance ratio
        entity.setMaintenanceMargin(entity.getMargin().multiply(MAINTENANCE_RATIO));

        // Calculate risk level
        entity.setRiskLevel(calculateRiskLevel());
    }

    /**
     * Calculate risk level based on account state
     * @return Risk level string (NORMAL, WARNING, DANGER)
     */
    public String calculateRiskLevel() {
        // Available cash < 0 → DANGER
        if (entity.getAvailableCash().compareTo(BigDecimal.ZERO) < 0) {
            return "DANGER";
        }

        // Calculate maintenance margin rate = total assets / maintenance margin
        if (entity.getMaintenanceMargin().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginRate = entity.getTotalAssets()
                    .divide(entity.getMaintenanceMargin(), 4, RoundingMode.HALF_UP);

            // Maintenance margin rate < 130% → DANGER
            if (marginRate.compareTo(DANGER_MARGIN_RATE) < 0) {
                return "DANGER";
            }

            // Maintenance margin rate < 150% → WARNING
            if (marginRate.compareTo(WARNING_MARGIN_RATE) < 0) {
                return "WARNING";
            }
        }

        return "NORMAL";
    }

    /**
     * Update today's profit loss
     * @param todayProfitLoss Today's profit/loss amount
     */
    public void updateTodayProfitLoss(BigDecimal todayProfitLoss) {
        entity.setTodayProfitLoss(todayProfitLoss != null ? todayProfitLoss : BigDecimal.ZERO);
    }

    /**
     * Update total profit loss
     * @param totalProfitLoss Total profit/loss amount
     */
    public void updateTotalProfitLoss(BigDecimal totalProfitLoss) {
        entity.setTotalProfitLoss(totalProfitLoss != null ? totalProfitLoss : BigDecimal.ZERO);
    }

    public AssetEntity getEntity() {
        return entity;
    }
}
