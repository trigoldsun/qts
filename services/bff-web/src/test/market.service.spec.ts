/**
 * Market Service Unit Tests
 * Tests for market (kline) business logic with mocked gRPC clients
 */

import { MarketService } from '../market.service';
import { GrpcClients } from '../trade/grpc-clients';
import { KlinePeriod, AdjustmentType } from '../models/kline';

// Mock dependencies
jest.mock('../trade/grpc-clients');

describe('MarketService', () => {
  let marketService: MarketService;
  let mockGrpcClients: jest.Mocked<GrpcClients>;

  beforeEach(() => {
    // Clear all mocks
    jest.clearAllMocks();

    // Get mock instances
    mockGrpcClients = new GrpcClients(null as any) as jest.Mocked<GrpcClients>;
    mockGrpcClients.getClient = jest.fn().mockReturnValue({
      name: 'agg-market',
      port: 50053,
      connected: true,
    });

    // Initialize service with mocked dependencies
    marketService = new MarketService(mockGrpcClients);
  });

  describe('queryKline', () => {
    it('should query kline data successfully', async () => {
      // Given
      const request = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_MINUTE,
        count: 10,
        adjustment: AdjustmentType.NONE,
      };

      // When
      const result = await marketService.queryKline(request);

      // Then
      expect(result).toBeDefined();
      expect(result.symbol).toBe('BTC-USDT');
      expect(result.period).toBe('1m');
      expect(result.adjustment).toBe(AdjustmentType.NONE);
      expect(result.klines).toBeDefined();
      expect(result.klines.length).toBe(10);
    });

    it('should generate klines with correct structure', async () => {
      // Given
      const request = {
        symbol: 'ETH-USDT',
        period: KlinePeriod.FIVE_MINUTES,
        count: 5,
        adjustment: AdjustmentType.NONE,
      };

      // When
      const result = await marketService.queryKline(request);

      // Then
      expect(result.klines.length).toBe(5);
      
      const firstKline = result.klines[0];
      expect(firstKline).toHaveProperty('open');
      expect(firstKline).toHaveProperty('high');
      expect(firstKline).toHaveProperty('low');
      expect(firstKline).toHaveProperty('close');
      expect(firstKline).toHaveProperty('volume');
      expect(firstKline).toHaveProperty('amount');
      expect(firstKline).toHaveProperty('timestamp');
      
      expect(typeof firstKline.open).toBe('number');
      expect(typeof firstKline.high).toBe('number');
      expect(typeof firstKline.low).toBe('number');
      expect(typeof firstKline.close).toBe('number');
      expect(typeof firstKline.volume).toBe('number');
    });

    it('should use default count of 100 when not specified', async () => {
      // Given
      const request = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_HOUR,
      };

      // When
      const result = await marketService.queryKline(request);

      // Then
      expect(result.klines.length).toBe(100);
    });

    it('should call gRPC client for agg-market', async () => {
      // Given
      const request = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_DAY,
        count: 50,
        adjustment: AdjustmentType.FORWARD,
      };

      // When
      await marketService.queryKline(request);

      // Then
      expect(mockGrpcClients.getClient).toHaveBeenCalledWith('agg-market');
    });
  });
});
