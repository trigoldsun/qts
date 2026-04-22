import { WebSocketGateway, WebSocketServer, SubscribeMessage, OnGatewayConnection, OnGatewayDisconnect, MessageBody, ConnectedSocket } from '@nestjs/websockets';
import { Logger } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import { RiskAlert, AlertLevel } from '../models/risk';

@WebSocketGateway({ path: '/ws/risk/alerts' })
export class RiskWebsocketGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(RiskWebsocketGateway.name);
  private connectedClients: Map<string, Socket> = new Map();

  handleConnection(client: Socket) {
    const clientId = client.id;
    this.connectedClients.set(clientId, client);
    this.logger.log(`Risk alert client connected: ${clientId}, total: ${this.connectedClients.size}`);
  }

  handleDisconnect(client: Socket) {
    const clientId = client.id;
    this.connectedClients.delete(clientId);
    this.logger.log(`Risk alert client disconnected: ${clientId}, total: ${this.connectedClients.size}`);
  }

  /**
   * Subscribe to risk alerts for a specific account
   */
  @SubscribeMessage('subscribe_alerts')
  handleSubscribeAlerts(@MessageBody() data: { accountId: string }, @ConnectedSocket() client: Socket) {
    const { accountId } = data;
    this.logger.log(`Client ${client.id} subscribing to risk alerts for account ${accountId}`);

    // Join a room for this account's risk alerts
    client.join(`risk:${accountId}`);

    return { event: 'subscribed', data: { accountId, channel: 'risk_alerts' } };
  }

  /**
   * Unsubscribe from risk alerts
   */
  @SubscribeMessage('unsubscribe_alerts')
  handleUnsubscribeAlerts(@MessageBody() data: { accountId: string }, @ConnectedSocket() client: Socket) {
    const { accountId } = data;
    this.logger.log(`Client ${client.id} unsubscribing from risk alerts for account ${accountId}`);

    client.leave(`risk:${accountId}`);

    return { event: 'unsubscribed', data: { accountId, channel: 'risk_alerts' } };
  }

  /**
   * Broadcast risk alert to all subscribers of an account
   * Called by risk service when margin ratio thresholds are breached
   * marginRatio > 80% → WARNING
   * marginRatio > 95% → CRITICAL
   */
  broadcastRiskAlert(accountId: string, alert: RiskAlert) {
    const room = `risk:${accountId}`;
    this.logger.log(`Broadcasting risk alert to room ${room}: ${JSON.stringify(alert)}`);

    this.server.to(room).emit('risk_alert', {
      type: alert.type,
      level: alert.level,
      message: alert.message,
      timestamp: alert.timestamp,
    });
  }

  /**
   * Send margin ratio warning based on threshold
   */
  sendMarginRatioAlert(accountId: string, marginRatio: number) {
    let level: AlertLevel;
    let message: string;

    if (marginRatio > 95) {
      level = AlertLevel.CRITICAL;
      message = `强平警告：保证金率 ${marginRatio}% 已超过95%阈值！`;
    } else if (marginRatio > 80) {
      level = AlertLevel.WARNING;
      message = `风控预警：保证金率 ${marginRatio}% 已超过80%阈值！`;
    } else {
      level = AlertLevel.INFO;
      message = `保证金率 ${marginRatio}% 处于安全范围`;
    }

    this.broadcastRiskAlert(accountId, {
      type: 'MARGIN_RATIO_ALERT',
      level,
      message,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Get connected client count
   */
  getClientCount(): number {
    return this.connectedClients.size;
  }
}
