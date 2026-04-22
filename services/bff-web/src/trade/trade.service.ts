import { Injectable, Logger } from '@nestjs/common';
import { GrpcClients, GRPC_PORTS } from './grpc-clients';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order';
import { Position } from '../models/position';
import { Asset } from '../models/asset';
import { Trade } from '../models/trade';
import { AmendOrderRequest } from '../models/order';

@Injectable()
export class TradeService {
  private readonly logger = new Logger(TradeService.name);

  constructor(private readonly grpcClients: GrpcClients) {}

  /**
   * Place order via agg-trade gRPC service
   */
  async placeOrder(cmd: CreateOrderRequest): Promise<Order> {
    this.logger.log(`Placing order for account ${cmd.accountId}, symbol ${cmd.symbol}`);
    
    const tradeClient = this.grpcClients.getClient('agg-trade');
    
    // Simulate gRPC call to agg-trade:50051
    // In production: this.tradeClient.placeOrder(cmd)
    const order: Order = {
      orderId: `ORD${Date.now()}`,
      accountId: cmd.accountId,
      symbol: cmd.symbol,
      side: cmd.side,
      type: cmd.type,
      price: cmd.price,
      quantity: cmd.quantity,
      filledQuantity: 0,
      status: OrderStatus.SUBMITTED,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    this.logger.log(`Order placed: ${order.orderId}`);
    return order;
  }

  /**
   * Amend (modify) order via agg-trade gRPC service
   */
  async amendOrder(req: AmendOrderRequest): Promise<Order> {
    this.logger.log(`Amending order ${req.orderId}`);
    
    const tradeClient = this.grpcClients.getClient('agg-trade');
    
    // Simulate gRPC call to agg-trade for order amendment
    const order: Order = {
      orderId: req.orderId,
      accountId: 'ACC001',
      symbol: 'BTC-USDT',
      side: 'BUY',
      type: 'LIMIT',
      price: req.price ?? 50000,
      quantity: req.quantity ?? 1.5,
      filledQuantity: 0,
      status: OrderStatus.SUBMITTED,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    this.logger.log(`Order amended: ${order.orderId}`);
    return order;
  }

  /**
   * Cancel order via agg-trade gRPC service
   */
  async cancelOrder(orderId: string): Promise<Order> {
    this.logger.log(`Cancelling order ${orderId}`);
    
    const tradeClient = this.grpcClients.getClient('agg-trade');
    
    // Simulate gRPC call to agg-trade for order cancellation
    const order: Order = {
      orderId,
      accountId: 'ACC001',
      symbol: 'BTC-USDT',
      side: 'BUY',
      type: 'LIMIT',
      price: 50000,
      quantity: 1.5,
      filledQuantity: 0,
      status: OrderStatus.CANCELLED,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    this.logger.log(`Order cancelled: ${orderId}`);
    return order;
  }

  /**
   * Get positions for account via agg-trade gRPC service
   */
  async getPositions(accountId: string): Promise<Position[]> {
    this.logger.log(`Getting positions for account ${accountId}`);
    
    const tradeClient = this.grpcClients.getClient('agg-trade');
    
    // Simulate gRPC call: this.tradeClient.getPositions(accountId)
    const positions: Position[] = [
      {
        positionId: 'POS001',
        accountId,
        symbol: 'BTC-USDT',
        quantity: 1.5,
        avgPrice: 50000,
        unrealizedPnl: 1500,
        realizedPnl: 0,
        updatedAt: new Date(),
      },
      {
        positionId: 'POS002',
        accountId,
        symbol: 'ETH-USDT',
        quantity: 10,
        avgPrice: 3000,
        unrealizedPnl: 500,
        realizedPnl: 0,
        updatedAt: new Date(),
      },
    ];

    return positions;
  }

  /**
   * Get assets for account via agg-trade gRPC service
   */
  async getAssets(accountId: string): Promise<Asset[]> {
    this.logger.log(`Getting assets for account ${accountId}`);
    
    const assetClient = this.grpcClients.getClient('agg-trade');
    
    // Simulate gRPC call: this.assetClient.getAssets(accountId)
    const assets: Asset[] = [
      {
        assetId: 'AST001',
        accountId,
        currency: 'USDT',
        available: 100000,
        locked: 5000,
        total: 105000,
        updatedAt: new Date(),
      },
      {
        assetId: 'AST002',
        accountId,
        currency: 'BTC',
        available: 2.5,
        locked: 0,
        total: 2.5,
        updatedAt: new Date(),
      },
    ];

    return assets;
  }

  /**
   * Get orders for account
   */
  async getOrders(accountId: string): Promise<Order[]> {
    this.logger.log(`Getting orders for account ${accountId}`);
    
    // Simulate fetching orders
    const orders: Order[] = [
      {
        orderId: 'ORD001',
        accountId,
        symbol: 'BTC-USDT',
        side: 'BUY',
        type: 'LIMIT',
        price: 50000,
        quantity: 1.5,
        filledQuantity: 1.5,
        status: OrderStatus.FILLED,
        createdAt: new Date(),
        updatedAt: new Date(),
      },
    ];

    return orders;
  }

  /**
   * Get trades for account
   */
  async getTrades(accountId: string): Promise<Trade[]> {
    this.logger.log(`Getting trades for account ${accountId}`);
    
    // Simulate fetching trades
    const trades: Trade[] = [
      {
        tradeId: 'TRD001',
        orderId: 'ORD001',
        accountId,
        symbol: 'BTC-USDT',
        side: 'BUY',
        price: 50000,
        quantity: 1.5,
        timestamp: new Date(),
      },
    ];

    return trades;
  }

  /**
   * Get single order by ID
   */
  async getOrder(orderId: string): Promise<Order | null> {
    this.logger.log(`Getting order ${orderId}`);
    
    // Simulate fetching order
    if (!orderId.startsWith('ORD')) {
      return null;
    }

    return {
      orderId,
      accountId: 'ACC001',
      symbol: 'BTC-USDT',
      side: 'BUY',
      type: 'LIMIT',
      price: 50000,
      quantity: 1.5,
      filledQuantity: 1.5,
      status: OrderStatus.FILLED,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
  }
}
