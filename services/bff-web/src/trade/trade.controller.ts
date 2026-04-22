import { Controller, Get, Post, Put, Delete, Body, Param, Query, Res, HttpStatus, HttpException } from '@nestjs/common';
import { Response } from 'express';
import { TradeService } from './trade.service';
import { CreateOrderRequest, AmendOrderRequest, OrderSide, OrderType } from '../models/order';

@Controller('v1')
export class TradeController {
  constructor(private readonly tradeService: TradeService) {}

  /**
   * POST /v1/orders - Place a new order
   */
  @Post('orders')
  async createOrder(@Body() body: CreateOrderRequest, @Res() res: Response) {
    try {
      // Validate required fields
      if (!body.accountId || !body.symbol || !body.side || !body.type || body.price === undefined || body.quantity === undefined) {
        throw new HttpException('Missing required fields: accountId, symbol, side, type, price, quantity', HttpStatus.BAD_REQUEST);
      }

      // Validate price and quantity
      if (body.price < 0) {
        throw new HttpException('Price must be non-negative', HttpStatus.BAD_REQUEST);
      }
      if (body.quantity <= 0) {
        throw new HttpException('Quantity must be positive', HttpStatus.BAD_REQUEST);
      }

      const order = await this.tradeService.placeOrder({
        accountId: body.accountId,
        symbol: body.symbol,
        side: body.side as OrderSide,
        type: body.type as OrderType,
        price: body.price,
        quantity: body.quantity,
      });

      return res.status(HttpStatus.CREATED).json({
        success: true,
        data: order,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to create order', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * PUT /v1/orders/:order_id - Amend an existing order
   */
  @Put('orders/:order_id')
  async amendOrder(@Param('order_id') orderId: string, @Body() body: Partial<AmendOrderRequest>, @Res() res: Response) {
    try {
      if (!orderId) {
        throw new HttpException('Order ID is required', HttpStatus.BAD_REQUEST);
      }

      const order = await this.tradeService.amendOrder({
        orderId,
        price: body.price,
        quantity: body.quantity,
      });

      return res.status(HttpStatus.OK).json({
        success: true,
        data: order,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to amend order', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * DELETE /v1/orders/:order_id - Cancel an order
   */
  @Delete('orders/:order_id')
  async cancelOrder(@Param('order_id') orderId: string, @Res() res: Response) {
    try {
      if (!orderId) {
        throw new HttpException('Order ID is required', HttpStatus.BAD_REQUEST);
      }

      const order = await this.tradeService.cancelOrder(orderId);

      return res.status(HttpStatus.OK).json({
        success: true,
        data: order,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to cancel order', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/accounts/:account_id/positions - Get positions for an account
   */
  @Get('accounts/:account_id/positions')
  async getPositions(@Param('account_id') accountId: string, @Res() res: Response) {
    try {
      if (!accountId) {
        throw new HttpException('Account ID is required', HttpStatus.BAD_REQUEST);
      }

      const positions = await this.tradeService.getPositions(accountId);

      return res.status(HttpStatus.OK).json({
        success: true,
        data: positions,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get positions', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/accounts/:account_id/assets - Get assets for an account
   */
  @Get('accounts/:account_id/assets')
  async getAssets(@Param('account_id') accountId: string, @Res() res: Response) {
    try {
      if (!accountId) {
        throw new HttpException('Account ID is required', HttpStatus.BAD_REQUEST);
      }

      const assets = await this.tradeService.getAssets(accountId);

      return res.status(HttpStatus.OK).json({
        success: true,
        data: assets,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get assets', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/accounts/:account_id/orders - Get orders for an account
   */
  @Get('accounts/:account_id/orders')
  async getOrders(@Param('account_id') accountId: string, @Res() res: Response) {
    try {
      if (!accountId) {
        throw new HttpException('Account ID is required', HttpStatus.BAD_REQUEST);
      }

      const orders = await this.tradeService.getOrders(accountId);

      return res.status(HttpStatus.OK).json({
        success: true,
        data: orders,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get orders', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/accounts/:account_id/trades - Get trades for an account
   */
  @Get('accounts/:account_id/trades')
  async getTrades(@Param('account_id') accountId: string, @Res() res: Response) {
    try {
      if (!accountId) {
        throw new HttpException('Account ID is required', HttpStatus.BAD_REQUEST);
      }

      const trades = await this.tradeService.getTrades(accountId);

      return res.status(HttpStatus.OK).json({
        success: true,
        data: trades,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get trades', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
