<template>
  <div 
    class="chinese-decoration" 
    :class="[type, position]"
    :style="positionStyle"
  >
    <!-- 祥云角饰 -->
    <svg 
      v-if="type === 'cloud-corner'" 
      class="cloud-ornament" 
      viewBox="0 0 100 100" 
      preserveAspectRatio="none"
    >
      <path 
        d="M50 15 C 35 15, 25 30, 25 45 C 25 55, 30 62, 38 65 C 28 68, 15 80, 15 95 C 15 95, 15 95, 16 95 C 28 95, 40 85, 50 85 C 55 85, 60 87, 64 90 C 70 92, 75 92, 80 90 C 88 87, 95 78, 95 68 C 95 58, 88 50, 78 48 C 85 40, 85 25, 75 18 C 68 12, 58 10, 50 15 Z" 
        fill="currentColor" 
      />
      <!-- 祥云卷尾 -->
      <path 
        d="M75 18 C 68 12, 58 10, 50 15 M 50 15 C 35 15, 25 30, 25 45" 
        stroke="currentColor" 
        stroke-width="2" 
        fill="none" 
        stroke-linecap="round"
      />
    </svg>
    
    <!-- 水墨渐变背景 -->
    <div v-if="type === 'ink-wash'" class="ink-wash-background">
      <div class="ink-layer ink-layer-1"></div>
      <div class="ink-layer ink-layer-2"></div>
      <div class="ink-layer ink-layer-3"></div>
    </div>
    
    <!-- 印章装饰 -->
    <div v-if="type === 'seal'" class="seal-stamp" :class="sealSize">
      <span class="seal-text">{{ sealText }}</span>
    </div>

    <!-- 竹节装饰 -->
    <svg 
      v-if="type === 'bamboo'" 
      class="bamboo-ornament" 
      viewBox="0 0 60 120" 
      preserveAspectRatio="none"
    >
      <rect x="25" y="0" width="10" height="120" rx="2" fill="currentColor" opacity="0.3"/>
      <rect x="25" y="15" width="10" height="3" rx="1" fill="currentColor" opacity="0.5"/>
      <rect x="25" y="45" width="10" height="3" rx="1" fill="currentColor" opacity="0.5"/>
      <rect x="25" y="75" width="10" height="3" rx="1" fill="currentColor" opacity="0.5"/>
      <rect x="25" y="105" width="10" height="3" rx="1" fill="currentColor" opacity="0.5"/>
      <path d="M25 20 L 10 25 M 35 20 L 50 25 M 25 50 L 8 55 M 35 50 L 52 55 M 25 80 L 10 85 M 35 80 L 50 85" 
        stroke="currentColor" stroke-width="1" opacity="0.4" fill="none"/>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type DecorationType = 'cloud-corner' | 'ink-wash' | 'seal' | 'bamboo'
type PositionType = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right'
type SealSizeType = 'sm' | 'md' | 'lg'

interface Props {
  type: DecorationType
  position?: PositionType
  sealText?: string
  sealSize?: SealSizeType
  opacity?: number
}

const props = withDefaults(defineProps<Props>(), {
  position: 'top-left',
  sealText: '量化',
  sealSize: 'md',
  opacity: 1
})

const positionStyle = computed(() => ({
  '--decoration-opacity': props.opacity
}))
</script>

<style scoped>
.chinese-decoration {
  position: absolute;
  pointer-events: none;
  opacity: var(--decoration-opacity, 1);
}

.chinese-decoration.top-left {
  top: 0;
  left: 0;
}

.chinese-decoration.top-right {
  top: 0;
  right: 0;
}

.chinese-decoration.bottom-left {
  bottom: 0;
  left: 0;
}

.chinese-decoration.bottom-right {
  bottom: 0;
  right: 0;
}

/* 祥云角饰 */
.cloud-ornament {
  width: 120px;
  height: 120px;
  color: var(--chinese-gold, #D4AF37);
  opacity: 0.15;
  animation: cloud-float 6s ease-in-out infinite;
}

.cloud-ornament.top-right,
.cloud-ornament.bottom-right {
  transform: scaleX(-1);
}

.cloud-ornament.bottom-left,
.cloud-ornament.bottom-right {
  transform: scaleY(-1);
}

.cloud-ornament.bottom-right {
  transform: scale(-1, -1);
}

@keyframes cloud-float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-5px) rotate(2deg);
  }
}

/* 水墨渐变背景 */
.ink-wash-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--chinese-ink, #1A1A1A);
}

.ink-layer {
  position: absolute;
  width: 200%;
  height: 200%;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.4;
}

.ink-layer-1 {
  top: -50%;
  left: -50%;
  background: radial-gradient(ellipse at center, rgba(45, 45, 45, 0.8) 0%, transparent 70%);
  animation: ink-drift-1 15s ease-in-out infinite;
}

.ink-layer-2 {
  top: -30%;
  left: 20%;
  background: radial-gradient(ellipse at center, rgba(26, 26, 26, 0.6) 0%, transparent 60%);
  animation: ink-drift-2 20s ease-in-out infinite;
}

.ink-layer-3 {
  top: 10%;
  left: -20%;
  background: radial-gradient(ellipse at center, rgba(70, 70, 70, 0.3) 0%, transparent 50%);
  animation: ink-drift-3 18s ease-in-out infinite;
}

@keyframes ink-drift-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10%, 10%); }
}

@keyframes ink-drift-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-15%, 5%); }
}

@keyframes ink-drift-3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(5%, -10%); }
}

/* 印章装饰 */
.seal-stamp {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid currentColor;
  background: transparent;
  position: relative;
}

.seal-stamp::before {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid currentColor;
  opacity: 0.5;
}

.seal-stamp.sm {
  width: 40px;
  height: 40px;
}

.seal-stamp.sm .seal-text {
  font-size: 12px;
}

.seal-stamp.md {
  width: 60px;
  height: 60px;
}

.seal-stamp.md .seal-text {
  font-size: 16px;
}

.seal-stamp.lg {
  width: 80px;
  height: 80px;
}

.seal-stamp.lg .seal-text {
  font-size: 20px;
}

.seal-text {
  font-family: var(--font-chinese, serif);
  color: var(--chinese-red, #C41E3A);
  font-weight: bold;
  letter-spacing: 0.1em;
  writing-mode: vertical-rl;
  text-orientation: upright;
}

.seal-stamp {
  color: var(--chinese-red, #C41E3A);
  box-shadow: 0 0 10px rgba(196, 30, 58, 0.3);
}

/* 竹节装饰 */
.bamboo-ornament {
  width: 60px;
  height: 120px;
  color: var(--chinese-green, #2E8B57);
  opacity: 0.6;
}

.bamboo-ornament.top-right {
  transform: scaleX(-1);
}

.bamboo-ornament.bottom-left {
  transform: scaleY(-1);
}

.bamboo-ornament.bottom-right {
  transform: scale(-1, -1);
}
</style>
