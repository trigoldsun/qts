/**
 * Market Controller Unit Tests
 * Tests for market HTTP endpoints
 */

import { HttpStatus } from '@nestjs/common';
import { MarketController } from '../market.controller';
import { MarketService } from '../market.service';
import { KlinePeriod, AdjustmentType } from '../models/kline';

// Mock dependencies
jest.mock('../market.service');

describe('MarketController', () => {
  let marketController: MarketController;
  let mockMarketService: jest.Mocked<MarketService>;

  const mockResponse = () => {
    const res: any = {};
    res.status = jest.fn().mockReturnValue(res);
    res.json = jest.fn().mockReturnValue(res);
    return res;
  };

  beforeEach(() => {
    // Clear all mocks
    jest.clearAllMocks();

    // Get mock instances
    mockMarketService = new MarketService(null as any) as jest.Mocked<MarketService>;
    mockMarketService.queryKline = jest.fn();

    // Initialize controller with mocked dependencies
    marketController = new MarketController(mockMarketService);
  });

  describe('GET /v1/market/kline', () => {
    it('should return kline data successfully', async () => {
      // Given
      const mockKlineResponse = {
        symbol: 'BTC-USDT',
        period: '1m',
        klines: [
          {
            open: 50000,
            high: 50500,
            low: 49500,
            close: 50200,
            volume: 1000,
            amount: 50000000,
            timestamp: new Date(),
          },
        ],
        adjustment: AdjustmentType.NONE,
      };

      mockMarketService.queryKline.mockResolvedValue(mockKlineResponse);

      const req = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_MINUTE,
        count: 100,
        adjustment: AdjustmentType.NONE,
      };

      const res = mockResponse();

      // When
      await marketController.getKline(req, res);

      // Then
      expect(res.status).toHaveBeenCalledWith(HttpStatus.OK);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        data: mockKlineResponse,
      });
    });

    it('should throw error when symbol is missing', async () => {
      // Given
      const req = {
        period: KlinePeriod.ONE_MINUTE,
      } as any;

      const res = mockResponse();

      // When & Then
      await expect(marketController.getKline(req, res)).rejects.toThrow(
        'Missing required fields: symbol, period'
      );
    });

    it('should throw error when period is missing', async () => {
      // Given
      const req = {
        symbol: 'BTC-USDT',
      } as any;

      const res = mockResponse();

      // When & Then
      await expect(marketController.getKline(req, res)).rejects.toThrow(
        'Missing required fields: symbol, period'
      );
    });

    it('should throw error for invalid period', async () => {
      // Given
      const req = {
        symbol: 'BTC-USDT',
        period: 'invalid' as any,
      };

      const res = mockResponse();

      // When & Then
      await expect(marketController.getKline(req, res)).rejects.toThrow(
        'Invalid period. Must be one of: 1m, 5m, 1h, 1d'
      );
    });

    it('should throw error when count exceeds 1000', async () => {
      // Given
      const req = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_MINUTE,
        count: 1001,
      };

      const res = mockResponse();

      // When & Then
      await expect(marketController.getKline(req, res)).rejects.toThrow(
        'Count must be between 1 and 1000'
      );
    });

    it('should throw error when count is less than 1', async () => {
      // Given
      const req = {
        symbol: 'BTC-USDT',
        period: KlinePeriod.ONE_MINUTE,
        count: 0,
      };

      const res = mockResponse();

      // When & Then
      await expect(marketController.getKline(req, res)).rejects.toThrow(
        'Count must be between 1 and 1000'
      );
    });
  });
});
