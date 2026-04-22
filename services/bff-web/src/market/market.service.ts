import { Injectable, Logger } from '@nestjs/common';
import { GrpcClients } from '../trade/grpc-clients';
import { KlineQueryRequest, KlineResponse } from '../models/kline';

@Injectable()
export class MarketService {
  private readonly logger = new Logger(MarketService.name);

  constructor(private readonly grpcClients: GrpcClients) {}

  /**
   * Query historical kline data via agg-market gRPC service
   */
  async queryKline(request: KlineQueryRequest): Promise<KlineResponse> {
    this.logger.log(
      `Querying kline for symbol=${request.symbol}, period=${request.period}, count=${request.count}`
    );

    const marketClient = this.grpcClients.getClient('agg-market');

    // Simulate gRPC call to agg-market:50053
    // In production: this.marketClient.getKline(request)
    const mockKlines = this.generateMockKlines(request);

    return {
      symbol: request.symbol,
      period: request.period,
      klines: mockKlines,
      adjustment: request.adjustment,
    };
  }

  /**
   * Generate mock kline data for testing
   */
  private generateMockKlines(request: KlineQueryRequest) {
    const klines = [];
    const count = request.count ?? 100;
    const now = new Date();

    for (let i = count - 1; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * this.getIntervalMs(request.period));
      const basePrice = 50000 + Math.random() * 1000;

      klines.push({
        open: parseFloat((basePrice + Math.random() * 100).toFixed(2)),
        high: parseFloat((basePrice + 200 + Math.random() * 100).toFixed(2)),
        low: parseFloat((basePrice - 100 - Math.random() * 100).toFixed(2)),
        close: parseFloat((basePrice + Math.random() * 200 - 100).toFixed(2)),
        volume: Math.floor(1000 + Math.random() * 10000),
        amount: parseFloat((basePrice * (1000 + Math.random() * 10000)).toFixed(2)),
        timestamp: timestamp,
      });
    }

    return klines;
  }

  /**
   * Get interval in milliseconds based on period
   */
  private getIntervalMs(period: string): number {
    switch (period) {
      case '1m':
        return 60 * 1000;
      case '5m':
        return 5 * 60 * 1000;
      case '1h':
        return 60 * 60 * 1000;
      case '1d':
        return 24 * 60 * 60 * 1000;
      default:
        return 60 * 1000;
    }
  }
}
