import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Role, User } from '../../core/models';
import { TaskApi } from '../../core/task-api';
import { Alerts } from '../../shared/alert.component';
import { ModalComponent } from '../../shared/modal.component';
@Component({ selector: 'app-member-form', imports: [ReactiveFormsModule, ModalComponent], template: `
  <app-modal title="Добавить участника" [busy]="pending()" (closed)="closed.emit()"><form [formGroup]="form" (ngSubmit)="submit()" class="task-form">
    <label>Имя<input formControlName="name" maxlength="80" placeholder="Имя и фамилия"></label>
    <label>Email<input type="email" formControlName="email" maxlength="160" autocomplete="off" placeholder="you@company.com"></label>
    <label>Начальный пароль<input type="password" formControlName="password" minlength="12" maxlength="64" autocomplete="new-password" placeholder="От 12 до 64 символов"></label>
    <label>Роль<select aria-label="Роль" formControlName="role"><option value="user">Участник</option><option value="admin">Администратор</option></select></label>
    <p class="muted small">Аккаунт создаётся сразу. Передайте данные для входа участнику по защищённому каналу.</p>
    @if (form.touched && form.invalid) { <p class="field-error">Проверьте имя, email и длину пароля (12–64 символа).</p> }
    <footer class="modal-footer"><button type="button" class="button secondary" [disabled]="pending()" (click)="closed.emit()">Отмена</button><button class="button primary" [disabled]="pending()">{{ pending() ? 'Создаём…' : 'Добавить участника' }}</button></footer>
  </form></app-modal>` })
export class MemberFormComponent {
  readonly closed = output(); readonly saved = output<User>(); readonly pending = signal(false);
  private readonly api = inject(TaskApi); private readonly alerts = inject(Alerts);
  readonly form = inject(FormBuilder).nonNullable.group({ name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]], password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(64)]], role: ['user' as Role] });
  submit() {
    this.form.markAllAsTouched(); if (this.form.invalid || this.pending()) return;
    this.pending.set(true); this.api.createUser(this.form.getRawValue()).pipe(finalize(() => this.pending.set(false))).subscribe({
      next: user => { this.alerts.show('Участник добавлен'); this.saved.emit(user); }, error: error => this.alerts.error(error)
    });
  }
}
