import { Controller, Get, Post, Body, Param, Res, HttpStatus, HttpException } from '@nestjs/common';
import { Response } from 'express';
import { RiskService } from './risk.service';
import { PrecheckRequest } from '../models/risk';

@Controller('v1/risk')
export class RiskController {
  constructor(private readonly riskService: RiskService) {}

  /**
   * GET /v1/risk/status - Get risk status overview
   * Returns: {marginRatio, riskLevel, availableFunds, positionMarketValue}
   */
  @Get('status')
  async getRiskStatus(
    @Body('accountId') accountId: string,
    @Res() res: Response,
  ) {
    try {
      const status = await this.riskService.getRiskStatus(accountId || 'ACC001');

      return res.status(HttpStatus.OK).json({
        success: true,
        data: status,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get risk status', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * POST /v1/risk/precheck - Trade pre-check
   * Request: {accountId, symbol, side, price, quantity}
   * Returns: {canTrade, reasons}
   */
  @Post('precheck')
  async precheckTrade(@Body() body: PrecheckRequest, @Res() res: Response) {
    try {
      // Validate required fields
      if (!body.accountId || !body.symbol || !body.side || body.price === undefined || body.quantity === undefined) {
        throw new HttpException(
          'Missing required fields: accountId, symbol, side, price, quantity',
          HttpStatus.BAD_REQUEST,
        );
      }

      // Validate side
      if (body.side !== 'BUY' && body.side !== 'SELL') {
        throw new HttpException('Invalid side, must be BUY or SELL', HttpStatus.BAD_REQUEST);
      }

      const result = await this.riskService.precheckTrade({
        accountId: body.accountId,
        symbol: body.symbol,
        side: body.side,
        price: body.price,
        quantity: body.quantity,
      });

      return res.status(HttpStatus.OK).json({
        success: true,
        data: result,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to precheck trade', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/risk/rules - Get risk rules list
   * Returns: [{ruleId, ruleName, limit, current, unit}]
   */
  @Get('rules')
  async getRiskRules(@Body('accountId') accountId: string, @Res() res: Response) {
    try {
      const rules = await this.riskService.getRiskRules(accountId || 'ACC001');

      return res.status(HttpStatus.OK).json({
        success: true,
        data: rules,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get risk rules', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /v1/risk/limits - Get trading limits card
   */
  @Get('limits')
  async getTradingLimits(@Body('accountId') accountId: string, @Res() res: Response) {
    try {
      const limits = await this.riskService.getTradingLimits(accountId || 'ACC001');

      return res.status(HttpStatus.OK).json({
        success: true,
        data: limits,
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      throw new HttpException('Failed to get trading limits', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
