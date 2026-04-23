// 告警音效工具
// 使用Web Audio API实现，无需外部音频文件

type AlertLevel = 'info' | 'warning' | 'critical';

export class AlertSound {
  private audioContext: AudioContext | null = null;

  private getAudioContext(): AudioContext {
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    return this.audioContext;
  }

  // 生成不同级别告警音效
  play(level: AlertLevel): void {
    const ctx = this.getAudioContext();
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    const config = {
      info: { freq: 880, duration: 0.15, type: 'sine' as OscillatorType },
      warning: { freq: 660, duration: 0.3, type: 'triangle' as OscillatorType },
      critical: { freq: 440, duration: 0.5, type: 'sawtooth' as OscillatorType },
    };

    const { freq, duration, type } = config[level];
    
    oscillator.frequency.value = freq;
    oscillator.type = type;
    gainNode.gain.setValueAtTime(0.3, ctx.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + duration);

    oscillator.start(ctx.currentTime);
    oscillator.stop(ctx.currentTime + duration);
  }

  // CRITICAL级别告警：连续急促音效
  playCriticalAlarm(): void {
    this.play('critical');
    setTimeout(() => this.play('critical'), 200);
    setTimeout(() => this.play('critical'), 400);
  }
}

export const alertSound = new AlertSound();
