import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// Market API
export const marketApi = {
  // Get quote for a symbol
  getQuote: (symbol: string) => 
    apiClient.get(`/v1/market/quotes/${symbol}`),
  
  // Get kline data
  getKline: (symbol: string, period: string, limit = 100) =>
    apiClient.get('/v1/market/kline', { params: { symbol, period, limit } }),
  
  // Get all symbols
  getSymbols: () =>
    apiClient.get('/v1/market/symbols'),
  
  // Get single symbol info
  getSymbol: (symbol: string) =>
    apiClient.get(`/v1/market/symbols/${symbol}`),
};

// Trade API
export const tradeApi = {
  // Place order
  placeOrder: (orderData: {
    accountId: string;
    symbol: string;
    side: 'BUY' | 'SELL';
    type: 'LIMIT' | 'MARKET';
    price: number;
    quantity: number;
  }) => apiClient.post('/v1/orders', orderData),
  
  // Amend order
  amendOrder: (orderId: string, data: { price?: number; quantity?: number }) =>
    apiClient.put(`/v1/orders/${orderId}`, data),
  
  // Cancel order
  cancelOrder: (orderId: string) =>
    apiClient.delete(`/v1/orders/${orderId}`),
  
  // Get positions
  getPositions: (accountId: string) =>
    apiClient.get(`/v1/accounts/${accountId}/positions`),
  
  // Get assets
  getAssets: (accountId: string) =>
    apiClient.get(`/v1/accounts/${accountId}/assets`),
  
  // Get orders
  getOrders: (accountId: string) =>
    apiClient.get(`/v1/accounts/${accountId}/orders`),
  
  // Get trades
  getTrades: (accountId: string) =>
    apiClient.get(`/v1/accounts/${accountId}/trades`),
};

export default apiClient;