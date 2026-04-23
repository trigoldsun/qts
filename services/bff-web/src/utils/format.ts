/**
 * 格式化工具函数
 */

/**
 * 格式化数字，保留指定小数位
 */
export const formatNumber = (num: number, decimals = 4): string => {
  return num.toLocaleString('en-US', { 
    maximumFractionDigits: decimals 
  })
}

/**
 * 格式化价格，保留2位小数
 */
export const formatPrice = (price: number): string => {
  return price.toLocaleString('en-US', { 
    minimumFractionDigits: 2, 
    maximumFractionDigits: 2 
  })
}

/**
 * 格式化涨跌幅
 */
export const formatPercent = (value: number): string => {
  return `${value >= 0 ? '+' : ''}${(value * 100).toFixed(2)}%`
}

/**
 * 格式化时间
 */
export const formatTime = (date: Date): string => {
  return new Date(date).toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

/**
 * 格式化成交量
 */
export const formatVolume = (vol: number): string => {
  if (vol >= 1e8) return (vol / 1e8).toFixed(2) + '亿'
  if (vol >= 1e4) return (vol / 1e4).toFixed(2) + '万'
  return vol.toFixed(2)
}

/**
 * 格式化盈亏金额
 */
export const formatPnL = (value: number): string => {
  return `${value >= 0 ? '+' : ''}${formatNumber(value)}`
}