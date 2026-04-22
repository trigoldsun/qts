/**
 * Trade Service Unit Tests
 * Tests for trade business logic with mocked repositories and external clients
 */

import { RiskCheckClient } from '../clients/risk-check.client';

// Mock dependencies
jest.mock('../repositories/order.repository');
jest.mock('../clients/risk-check.client');
jest.mock('../clients/account.client');

import { OrderRepository } from '../repositories/order.repository';
import { AccountClient } from '../clients/account.client';
import { TradeService } from '../services/trade.service';
import { OrderStatus } from '../models/order';

describe('TradeService', () => {
  let tradeService: TradeService;
  let mockOrderRepository: jest.Mocked<OrderRepository>;
  let mockRiskCheckClient: jest.Mocked<RiskCheckClient>;
  let mockAccountClient: jest.Mocked<AccountClient>;

  beforeEach(() => {
    // Clear all mocks
    jest.clearAllMocks();

    // Get mock instances
    mockOrderRepository = new OrderRepository() as jest.Mocked<OrderRepository>;
    mockRiskCheckClient = new RiskCheckClient() as jest.Mocked<RiskCheckClient>;
    mockAccountClient = new AccountClient() as jest.Mocked<AccountClient>;

    // Initialize service with mocked dependencies
    tradeService = new TradeService(
      mockOrderRepository,
      mockRiskCheckClient,
      mockAccountClient
    );
  });

  describe('createOrder', () => {
    it('should create order successfully when all checks pass', async () => {
      // Given
      const orderParams = {
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY' as const,
        price: 50000,
        quantity: 1.5,
      };

      const mockOrder = {
        orderId: 'ORD123',
        ...orderParams,
        status: OrderStatus.PENDING,
        createdAt: new Date(),
      };

      mockRiskCheckClient.checkRisk = jest.fn().mockResolvedValue({
        passed: true,
        accountId: orderParams.accountId,
        message: 'Risk check passed',
      });

      mockAccountClient.validateAccount = jest.fn().mockResolvedValue({
        valid: true,
        accountId: orderParams.accountId,
      });

      mockOrderRepository.create = jest.fn().mockResolvedValue(mockOrder);

      // When
      const result = await tradeService.createOrder(orderParams);

      // Then
      expect(result).toBeDefined();
      expect(result.orderId).toBe('ORD123');
      expect(result.status).toBe(OrderStatus.PENDING);
      expect(mockRiskCheckClient.checkRisk).toHaveBeenCalled();
      expect(mockAccountClient.validateAccount).toHaveBeenCalledWith(orderParams.accountId);
      expect(mockOrderRepository.create).toHaveBeenCalled();
    });

    it('should reject order when risk check fails', async () => {
      // Given
      const orderParams = {
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY' as const,
        price: 50000,
        quantity: 1.5,
      };

      mockRiskCheckClient.checkRisk = jest.fn().mockResolvedValue({
        passed: false,
        accountId: orderParams.accountId,
        message: 'Risk limit exceeded',
        rejectCode: 'RISK_001',
      });

      // When / Then
      await expect(tradeService.createOrder(orderParams)).rejects.toThrow('Risk check failed');
    });

    it('should reject order when account is invalid', async () => {
      // Given
      const orderParams = {
        accountId: 'INVALID',
        symbol: 'BTC-USDT',
        side: 'BUY' as const,
        price: 50000,
        quantity: 1.5,
      };

      mockRiskCheckClient.checkRisk = jest.fn().mockResolvedValue({
        passed: true,
        accountId: orderParams.accountId,
        message: 'Risk check passed',
      });

      mockAccountClient.validateAccount = jest.fn().mockResolvedValue({
        valid: false,
        accountId: orderParams.accountId,
        message: 'Account not found',
      });

      // When / Then
      await expect(tradeService.createOrder(orderParams)).rejects.toThrow('Invalid account');
    });

    it('should reject order with invalid parameters', async () => {
      // Given
      const invalidParams = {
        accountId: 'ACC001',
        symbol: '', // Invalid: empty symbol
        side: 'BUY' as const,
        price: -100, // Invalid: negative price
        quantity: 0, // Invalid: zero quantity
      };

      // When / Then
      await expect(tradeService.createOrder(invalidParams)).rejects.toThrow();
    });
  });

  describe('cancelOrder', () => {
    it('should cancel pending order successfully', async () => {
      // Given
      const orderId = 'ORD123';
      const mockOrder = {
        orderId,
        status: OrderStatus.PENDING,
      };

      mockOrderRepository.findById = jest.fn().mockResolvedValue(mockOrder);
      mockOrderRepository.update = jest.fn().mockResolvedValue({
        ...mockOrder,
        status: OrderStatus.CANCELLED,
      });

      // When
      const result = await tradeService.cancelOrder(orderId);

      // Then
      expect(result).toBeDefined();
      expect(result.status).toBe(OrderStatus.CANCELLED);
      expect(mockOrderRepository.update).toHaveBeenCalled();
    });

    it('should reject cancellation of filled order', async () => {
      // Given
      const orderId = 'ORD123';
      const mockOrder = {
        orderId,
        status: OrderStatus.FILLED,
      };

      mockOrderRepository.findById = jest.fn().mockResolvedValue(mockOrder);

      // When / Then
      await expect(tradeService.cancelOrder(orderId)).rejects.toThrow('Cannot cancel filled order');
    });

    it('should return null for non-existent order', async () => {
      // Given
      const orderId = 'NONEXISTENT';
      mockOrderRepository.findById = jest.fn().mockResolvedValue(null);

      // When
      const result = await tradeService.cancelOrder(orderId);

      // Then
      expect(result).toBeNull();
    });
  });

  describe('getOrder', () => {
    it('should return order by ID', async () => {
      // Given
      const orderId = 'ORD123';
      const mockOrder = {
        orderId,
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY' as const,
        price: 50000,
        quantity: 1.5,
        status: OrderStatus.FILLED,
      };

      mockOrderRepository.findById = jest.fn().mockResolvedValue(mockOrder);

      // When
      const result = await tradeService.getOrder(orderId);

      // Then
      expect(result).toEqual(mockOrder);
      expect(mockOrderRepository.findById).toHaveBeenCalledWith(orderId);
    });

    it('should return null for non-existent order', async () => {
      // Given
      const orderId = 'NONEXISTENT';
      mockOrderRepository.findById = jest.fn().mockResolvedValue(null);

      // When
      const result = await tradeService.getOrder(orderId);

      // Then
      expect(result).toBeNull();
    });
  });

  describe('getOrders', () => {
    it('should return paginated orders', async () => {
      // Given
      const filters = { accountId: 'ACC001', status: OrderStatus.FILLED };
      const pagination = { page: 1, pageSize: 10 };
      const mockOrders = [
        { orderId: 'ORD001', status: OrderStatus.FILLED },
        { orderId: 'ORD002', status: OrderStatus.FILLED },
      ];

      mockOrderRepository.findAll = jest.fn().mockResolvedValue({
        orders: mockOrders,
        total: 2,
        page: 1,
        pageSize: 10,
      });

      // When
      const result = await tradeService.getOrders(filters, pagination);

      // Then
      expect(result.orders).toHaveLength(2);
      expect(result.total).toBe(2);
      expect(mockOrderRepository.findAll).toHaveBeenCalledWith(filters, pagination);
    });
  });
});
