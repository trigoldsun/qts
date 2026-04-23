// src/composables/useResponsive.ts
import { ref, computed, onMounted, onUnmounted } from 'vue'

export type Breakpoint = 'mobile' | 'tablet' | 'desktop'

export function useResponsive() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

  const isMobile = computed(() => width.value < 768)
  const isTablet = computed(() => width.value >= 768 && width.value < 1200)
  const isDesktop = computed(() => width.value >= 1200)

  const breakpoint = computed<Breakpoint>(() => {
    if (width.value < 768) return 'mobile'
    if (width.value < 1200) return 'tablet'
    return 'desktop'
  })

  const handleResize = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
  })

  return {
    width,
    isMobile,
    isTablet,
    isDesktop,
    breakpoint,
  }
}