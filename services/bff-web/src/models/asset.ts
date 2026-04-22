export interface Asset {
  assetId: string;
  accountId: string;
  currency: string;
  available: number;
  locked: number;
  total: number;
  updatedAt: Date;
}

export interface GetAssetsRequest {
  accountId: string;
}
