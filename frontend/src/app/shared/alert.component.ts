import { Component, inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
@Injectable({ providedIn: 'root' })
export class Alerts {
  readonly message = signal<{ text: string; type: 'success' | 'error' } | null>(null);
  private timer?: ReturnType<typeof setTimeout>;
  show(text: string, type: 'success' | 'error' = 'success') {
    clearTimeout(this.timer); this.message.set({ text, type }); this.timer = setTimeout(() => this.message.set(null), 7000);
  }
  error(error: unknown) {
    const e = error as HttpErrorResponse;
    this.show(e.status === 0 ? 'Сервер недоступен. Проверьте соединение и повторите действие.' : e.error?.message || 'Не удалось выполнить действие.', 'error');
  }
}
@Component({ selector: 'app-alert', template: `
  @if (alerts.message(); as message) {
    <div class="toast" [class.error]="message.type === 'error'" role="alert">
      <span class="toast-icon">{{ message.type === 'error' ? '!' : '✓' }}</span><span>{{ message.text }}</span>
      <button type="button" (click)="alerts.message.set(null)" aria-label="Закрыть уведомление">×</button>
    </div>
  }` })
export class AlertComponent { readonly alerts = inject(Alerts); }
