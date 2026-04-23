import { ref, onMounted } from 'vue'

export type Theme = 'dark' | 'light' | 'chinese-traditional'

const STORAGE_KEY = 'qts-theme'

// 全局主题状态
const currentTheme = ref<Theme>('dark')

export function useTheme() {
  // 获取可用主题
  const themes: Theme[] = ['dark', 'light', 'chinese-traditional']
  
  // 主题名称映射
  const themeNames: Record<Theme, string> = {
    'dark': '🌙 暗色主题',
    'light': '☀️ 亮色主题', 
    'chinese-traditional': '🏮 中国传统风'
  }
  
  // 设置主题
  const setTheme = (theme: Theme) => {
    currentTheme.value = theme
    localStorage.setItem(STORAGE_KEY, theme)
    document.documentElement.setAttribute('data-theme', theme)
  }
  
  // 切换到下一个主题
  const toggleTheme = () => {
    const currentIndex = themes.indexOf(currentTheme.value)
    const nextIndex = (currentIndex + 1) % themes.length
    setTheme(themes[nextIndex])
  }
  
  // 初始化
  onMounted(() => {
    const saved = localStorage.getItem(STORAGE_KEY) as Theme | null
    if (saved && themes.includes(saved)) {
      setTheme(saved)
    } else {
      // 默认跟随系统偏好
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      setTheme(prefersDark ? 'dark' : 'light')
    }
  })
  
  // 监听系统主题变化
  onMounted(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = (e: MediaQueryListEvent) => {
      if (!localStorage.getItem(STORAGE_KEY)) {
        setTheme(e.matches ? 'dark' : 'light')
      }
    }
    mediaQuery.addEventListener('change', handler)
    return () => mediaQuery.removeEventListener('change', handler)
  })
  
  return {
    currentTheme,
    themes,
    themeNames,
    setTheme,
    toggleTheme,
  }
}
