<template>
  <div class="theme-toggle" :title="`当前: ${themeNames[currentTheme]}`">
    <button 
      class="toggle-btn"
      @click="toggleTheme"
      aria-label="切换主题"
    >
      <span class="toggle-icon">{{ currentIcon }}</span>
    </button>
    
    <!-- 主题选择下拉 -->
    <div v-if="showPicker" class="theme-picker">
      <button
        v-for="theme in themes"
        :key="theme"
        class="theme-option"
        :class="{ active: currentTheme === theme }"
        @click="selectTheme(theme)"
      >
        <span class="option-icon">{{ getThemeIcon(theme) }}</span>
        <span class="option-name">{{ themeNames[theme] }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useTheme, type Theme } from '@/composables/useTheme'

const { currentTheme, themes, themeNames, setTheme, toggleTheme } = useTheme()

const showPicker = ref(false)

const currentIcon = computed(() => getThemeIcon(currentTheme.value))
const getThemeIcon = (theme: Theme) => ({
  'dark': '🌙',
  'light': '☀️',
  'chinese-traditional': '🏮'
}[theme])

const selectTheme = (theme: Theme) => {
  setTheme(theme)
  showPicker.value = false
}

// 点击外部关闭
const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.theme-toggle')) {
    showPicker.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.theme-toggle {
  position: relative;
}

.toggle-btn {
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover {
  background: var(--bg-elevated);
  border-color: var(--color-accent);
}

.toggle-icon {
  font-size: 18px;
}

.theme-picker {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 8px;
  min-width: 160px;
  box-shadow: var(--shadow-lg);
  z-index: 100;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-primary);
  transition: background 0.2s;
}

.theme-option:hover {
  background: var(--bg-elevated);
}

.theme-option.active {
  background: rgba(74, 159, 255, 0.2);
  color: var(--color-accent);
}

.option-icon { font-size: 16px; }
.option-name { font-size: 14px; }
</style>
