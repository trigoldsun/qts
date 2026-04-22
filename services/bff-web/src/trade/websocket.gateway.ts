import { WebSocketGateway, WebSocketServer, SubscribeMessage, OnGatewayConnection, OnGatewayDisconnect, MessageBody, ConnectedSocket } from '@nestjs/websockets';
import { Logger } from '@nestjs/common';
import { Server, Socket } from 'socket.io';

@WebSocketGateway({ path: '/v1/market/stream' })
export class WebsocketGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(WebsocketGateway.name);
  private connectedClients: Map<string, Socket> = new Map();

  handleConnection(client: Socket) {
    const clientId = client.id;
    this.connectedClients.set(clientId, client);
    this.logger.log(`Client connected: ${clientId}, total: ${this.connectedClients.size}`);
  }

  handleDisconnect(client: Socket) {
    const clientId = client.id;
    this.connectedClients.delete(clientId);
    this.logger.log(`Client disconnected: ${clientId}, total: ${this.connectedClients.size}`);
  }

  /**
   * Subscribe to trade updates for a specific account
   */
  @SubscribeMessage('subscribe_trades')
  handleSubscribeTrades(@MessageBody() data: { accountId: string }, @ConnectedSocket() client: Socket) {
    const { accountId } = data;
    this.logger.log(`Client ${client.id} subscribing to trades for account ${accountId}`);
    
    // Join a room for this account's trades
    client.join(`trades:${accountId}`);
    
    return { event: 'subscribed', data: { accountId, channel: 'trades' } };
  }

  /**
   * Unsubscribe from trade updates
   */
  @SubscribeMessage('unsubscribe_trades')
  handleUnsubscribeTrades(@MessageBody() data: { accountId: string }, @ConnectedSocket() client: Socket) {
    const { accountId } = data;
    this.logger.log(`Client ${client.id} unsubscribing from trades for account ${accountId}`);
    
    client.leave(`trades:${accountId}`);
    
    return { event: 'unsubscribed', data: { accountId, channel: 'trades' } };
  }

  /**
   * Broadcast trade execution to all subscribers of an account
   * This is called by the trade service when BIZ-TRADE sends execution reports
   */
  broadcastTrade(accountId: string, tradeData: any) {
    const room = `trades:${accountId}`;
    this.logger.log(`Broadcasting trade to room ${room}`);
    
    // Send to all clients in the account's room
    this.server.to(room).emit('trade', {
      type: 'TRADE',
      data: tradeData,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Broadcast order update to all subscribers of an account
   */
  broadcastOrderUpdate(accountId: string, orderData: any) {
    const room = `orders:${accountId}`;
    this.logger.log(`Broadcasting order update to room ${room}`);
    
    this.server.to(room).emit('order_update', {
      type: 'ORDER_UPDATE',
      data: orderData,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Broadcast position update to all subscribers of an account
   */
  broadcastPositionUpdate(accountId: string, positionData: any) {
    const room = `positions:${accountId}`;
    this.logger.log(`Broadcasting position update to room ${room}`);
    
    this.server.to(room).emit('position_update', {
      type: 'POSITION_UPDATE',
      data: positionData,
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
