import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth';
import { Alerts } from '../../shared/alert.component';
@Component({ selector: 'app-login', imports: [ReactiveFormsModule], template: `
  <main class="login-page">
    <section class="login-story"><a class="brand"><span class="brand-mark">t</span>teamflow<span class="brand-dot">.</span></a>
      <div class="login-copy"><span class="pill">МЕНЬШЕ ХАОСА. БОЛЬШЕ ФОКУСА.</span><h1>От большой идеи<br>к готовому<br><em>результату.</em></h1>
      <p>Одно пространство для задач, людей<br>и всего, что вы создаёте вместе.</p></div>
      <div class="mini-board" aria-hidden="true"><div><i></i><span>Планируем</span><b>Новая идея ↗</b><b>Всё по полочкам</b></div><div><i></i><span>Создаём</span><b>В потоке ✧</b></div><div><i></i><span>Готово</span><b>Есть результат ✓</b></div></div>
      <footer>РАБОТАЙТЕ В КОМАНДЕ. ДВИГАЙТЕСЬ ВПЕРЁД.</footer>
    </section>
    <section class="login-form-area"><div class="login-form-wrap"><span class="eyebrow">ВАШЕ РАБОЧЕЕ ПРОСТРАНСТВО</span><h2>С возвращением</h2><p class="muted">Войдите, чтобы продолжить работу с командой.</p>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>Email<input type="email" formControlName="email" autocomplete="username" placeholder="you@company.com"></label>
        <label>Пароль<input type="password" formControlName="password" autocomplete="current-password" placeholder="Введите пароль" maxlength="64"></label>
        @if (form.touched && form.invalid) { <p class="field-error">Введите корректный email и пароль.</p> }
        <button class="button primary wide" [disabled]="pending()">{{ pending() ? 'Входим…' : 'Войти в Teamflow' }} <span>→</span></button>
      </form>
      <div class="login-note"><span>◎</span><p>Доступ выдаёт администратор команды.<br>Сессия действует 30 минут.</p></div>
    </div><span class="login-bottom">Teamflow · Пространство, где идеи становятся делом</span></section>
  </main>` })
export class LoginComponent {
  private readonly auth = inject(AuthService); private readonly router = inject(Router); private readonly alerts = inject(Alerts);
  readonly pending = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit() {
    this.form.markAllAsTouched(); if (this.form.invalid || this.pending()) return;
    this.pending.set(true); const value = this.form.getRawValue();
    this.auth.login(value.email.trim(), value.password).pipe(finalize(() => this.pending.set(false))).subscribe({
      next: () => void this.router.navigateByUrl('/board'), error: error => this.alerts.error(error)
    });
  }
}
