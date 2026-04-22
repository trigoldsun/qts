import { Controller, Get, Query, Res, HttpStatus, HttpException } from '@nestjs/common';
import { Response } from 'express';
import { MarketService } from './market.service';
import { KlineQueryRequest, KlinePeriod, AdjustmentType } from '../models/kline';

@Controller('v1/market')
export class MarketController {
  constructor(private readonly marketService: MarketService) {}

  /**
   * GET /v1/market/kline - Query historical kline data
   */
  @Get('kline')
  async getKline(@Query() query: KlineQueryRequest, @Res() res: Response) {
    try {
      // Validate required fields
      if (!query.symbol || !query.period) {
        throw new HttpException('Missing required fields: symbol, period', HttpStatus.BAD_REQUEST);
      }

      // Validate period
      if (!Object.values(KlinePeriod).includes(query.period)) {
        throw new HttpException(
          'Invalid period. Must be one of: 1m, 5m, 1h, 1d',
          HttpStatus.BAD_REQUEST
        );
      }

      // Validate count
      const count = query.count ?? 100;
      if (count < 1 || count > 1000) {
        throw new HttpException('Count must be between 1 and 1000', HttpStatus.BAD_REQUEST);
      }

      const klineResponse = await this.marketService.queryKline({
        symbol: query.symbol,
        period: query.period,
        startTime: query.startTime,
        endTime: query.endTime,
        count: count,
        adjustment: query.adjustment ?? AdjustmentType.NONE,
      });

      return res.status(HttpStatus.OK).json({
        success: true,
        data: klineResponse,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to query kline', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
