/**
 * Trade Controller Unit Tests
 * Tests for trade API endpoints with mocked service layer
 */

import { Request, Response } from 'express';

// Mock the trade service before importing controller
jest.mock('../services/trade.service', () => ({
  TradeService: {
    createOrder: jest.fn(),
    cancelOrder: jest.fn(),
    getOrder: jest.fn(),
    getOrders: jest.fn(),
  },
}));

import { TradeController } from '../controllers/trade.controller';
import { TradeService } from '../services/trade.service';

describe('TradeController', () => {
  let mockRequest: Partial<Request>;
  let mockResponse: Partial<Response>;
  let controller: TradeController;

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Setup mock request
    mockRequest = {
      body: {},
      params: {},
      query: {},
      headers: {},
    };

    // Setup mock response
    mockResponse = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis(),
      send: jest.fn().mockReturnThis(),
    };

    controller = new TradeController();
  });

  describe('POST /api/trade/orders', () => {
    it('should create order successfully', async () => {
      // Given
      const orderData = {
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY',
        price: 50000,
        quantity: 1.5,
      };
      const expectedResult = {
        success: true,
        orderId: 'ORD123',
        data: orderData,
      };

      mockRequest.body = orderData;
      (TradeService.createOrder as jest.Mock).mockResolvedValue(expectedResult);

      // When
      await controller.createOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(TradeService.createOrder).toHaveBeenCalledWith(orderData);
      expect(mockResponse.status).toHaveBeenCalledWith(201);
      expect(mockResponse.json).toHaveBeenCalledWith(expectedResult);
    });

    it('should return 400 for invalid order data', async () => {
      // Given
      const invalidOrderData = {
        accountId: 'ACC001',
        symbol: '', // Invalid: empty symbol
        side: 'BUY',
        price: -100, // Invalid: negative price
        quantity: 0, // Invalid: zero quantity
      };

      mockRequest.body = invalidOrderData;

      // When
      await controller.createOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(mockResponse.status).toHaveBeenCalledWith(400);
    });

    it('should handle service errors', async () => {
      // Given
      const orderData = {
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY',
        price: 50000,
        quantity: 1.5,
      };

      mockRequest.body = orderData;
      (TradeService.createOrder as jest.Mock).mockRejectedValue(new Error('Service unavailable'));

      // When
      await controller.createOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });

  describe('DELETE /api/trade/orders/:orderId', () => {
    it('should cancel order successfully', async () => {
      // Given
      const orderId = 'ORD123';
      const expectedResult = { success: true, orderId };

      mockRequest.params = { orderId };
      (TradeService.cancelOrder as jest.Mock).mockResolvedValue(expectedResult);

      // When
      await controller.cancelOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(TradeService.cancelOrder).toHaveBeenCalledWith(orderId);
      expect(mockResponse.status).toHaveBeenCalledWith(200);
    });

    it('should return 404 for non-existent order', async () => {
      // Given
      const orderId = 'NONEXISTENT';
      mockRequest.params = { orderId };
      (TradeService.cancelOrder as jest.Mock).mockResolvedValue(null);

      // When
      await controller.cancelOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(mockResponse.status).toHaveBeenCalledWith(404);
    });
  });

  describe('GET /api/trade/orders/:orderId', () => {
    it('should get order by ID', async () => {
      // Given
      const orderId = 'ORD123';
      const expectedOrder = {
        orderId: 'ORD123',
        accountId: 'ACC001',
        symbol: 'BTC-USDT',
        side: 'BUY',
        price: 50000,
        quantity: 1.5,
        status: 'FILLED',
      };

      mockRequest.params = { orderId };
      (TradeService.getOrder as jest.Mock).mockResolvedValue(expectedOrder);

      // When
      await controller.getOrder(mockRequest as Request, mockResponse as Response);

      // Then
      expect(TradeService.getOrder).toHaveBeenCalledWith(orderId);
      expect(mockResponse.json).toHaveBeenCalledWith(expectedOrder);
    });
  });

  describe('GET /api/trade/orders', () => {
    it('should get orders with pagination', async () => {
      // Given
      const orders = [
        { orderId: 'ORD001', symbol: 'BTC-USDT' },
        { orderId: 'ORD002', symbol: 'ETH-USDT' },
      ];
      const pagination = { page: 1, pageSize: 10, total: 2 };

      mockRequest.query = { page: '1', pageSize: '10' };
      (TradeService.getOrders as jest.Mock).mockResolvedValue({ orders, pagination });

      // When
      await controller.getOrders(mockRequest as Request, mockResponse as Response);

      // Then
      expect(TradeService.getOrders).toHaveBeenCalled();
      expect(mockResponse.json).toHaveBeenCalledWith({ orders, pagination });
    });
  });
});
