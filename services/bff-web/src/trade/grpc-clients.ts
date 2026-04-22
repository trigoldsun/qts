import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { ModuleRef } from '@nestjs/core';

export const GRPC_PORTS = {
  'agg-trade': 50051,
  'agg-risk': 50052,
  'agg-market': 50053,
} as const;

export interface GrpcClientOptions {
  package: string;
  protoPath: string;
  url: string;
}

@Injectable()
export class GrpcClients implements OnModuleInit, OnModuleDestroy {
  private clients: Map<string, any> = new Map();

  onModuleInit() {
    this.connectClients();
  }

  onModuleDestroy() {
    this.closeClients();
  }

  private connectClients() {
    // In production, these would establish actual gRPC connections
    // For now, we set up the client configuration for agg-trade, agg-risk, agg-market
    
    Object.entries(GRPC_PORTS).forEach(([name, port]) => {
      console.log(`gRPC client configured: ${name} -> localhost:${port}`);
      this.clients.set(name, {
        name,
        port,
        connected: true,
      });
    });
  }

  private closeClients() {
    this.clients.forEach((client, name) => {
      console.log(`gRPC client disconnected: ${name}`);
    });
    this.clients.clear();
  }

  getClient(name: string): any {
    const client = this.clients.get(name);
    if (!client) {
      throw new Error(`gRPC client ${name} not found`);
    }
    return client;
  }

  isConnected(name: string): boolean {
    const client = this.clients.get(name);
    return client?.connected ?? false;
  }
}
