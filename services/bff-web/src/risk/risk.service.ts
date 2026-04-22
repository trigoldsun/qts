import { Injectable, Logger } from '@nestjs/common';
import { GrpcClients, GRPC_PORTS } from '../trade/grpc-clients';
import { RiskStatus, PrecheckRequest, PrecheckResponse, RiskRule, TradingLimit } from '../models/risk';

@Injectable()
export class RiskService {
  private readonly logger = new Logger(RiskService.name);

  constructor(private readonly grpcClients: GrpcClients) {}

  /**
   * Get risk status overview from biz-risk gRPC service
   * Calls: agg-risk:50051
   */
  async getRiskStatus(accountId: string): Promise<RiskStatus> {
    this.logger.log(`Getting risk status for account ${accountId}`);

    const riskClient = this.grpcClients.getClient('agg-risk');

    // In production: call riskClient.getRiskStatus({ accountId })
    // Simulating response from biz-risk service
    const marginRatio = 65.5; // Example value
    const riskLevel = marginRatio > 95 ? 'CRITICAL' : marginRatio > 80 ? 'WARNING' : 'NORMAL';

    return {
      marginRatio,
      riskLevel: riskLevel as RiskStatus['riskLevel'],
      availableFunds: 100000 - 35000, // available funds
      positionMarketValue: 35000,
    };
  }

  /**
   * Pre-check trade feasibility with biz-risk gRPC service
   * Calls: agg-risk:50051
   */
  async precheckTrade(req: PrecheckRequest): Promise<PrecheckResponse> {
    this.logger.log(`Pre-checking trade for account ${req.accountId}, symbol ${req.symbol}`);

    const riskClient = this.grpcClients.getClient('agg-risk');

    // In production: call riskClient.precheckTrade(req)
    // Simulating response from biz-risk service
    const reasons: string[] = [];

    // Simple validation logic
    if (req.quantity <= 0) {
      reasons.push('Invalid quantity');
    }
    if (req.price <= 0) {
      reasons.push('Invalid price');
    }
    if (req.side === 'BUY' && req.price * req.quantity > 100000) {
      reasons.push('Exceeds daily buy limit');
    }

    return {
      canTrade: reasons.length === 0,
      reasons,
    };
  }

  /**
   * Get risk rules list from biz-risk gRPC service
   * Calls: agg-risk:50051
   */
  async getRiskRules(accountId: string): Promise<RiskRule[]> {
    this.logger.log(`Getting risk rules for account ${accountId}`);

    const riskClient = this.grpcClients.getClient('agg-risk');

    // In production: call riskClient.getRiskRules({ accountId })
    // Simulating response from biz-risk service
    return [
      {
        ruleId: 'RULE001',
        ruleName: '当日可买额度',
        limit: 100000,
        current: 65000,
        unit: 'USDT',
      },
      {
        ruleId: 'RULE002',
        ruleName: '持仓上限',
        limit: 500000,
        current: 350000,
        unit: 'USDT',
      },
      {
        ruleId: 'RULE003',
        ruleName: '单笔上限',
        limit: 50000,
        current: 25000,
        unit: 'USDT',
      },
      {
        ruleId: 'RULE004',
        ruleName: '保证金率',
        limit: 100,
        current: 65.5,
        unit: '%',
      },
    ];
  }

  /**
   * Get trading limits card data
   */
  async getTradingLimits(accountId: string): Promise<TradingLimit> {
    this.logger.log(`Getting trading limits for account ${accountId}`);

    const riskClient = this.grpcClients.getClient('agg-risk');

    // In production: call riskClient.getTradingLimits({ accountId })
    return {
      dailyBuyLimit: 100000,
      positionLimit: 500000,
      singleTradeLimit: 50000,
    };
  }
}
