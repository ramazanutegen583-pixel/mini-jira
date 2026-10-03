import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { TaskApi } from '../../core/task-api';
import { Task, User, Priority } from '../../core/models';
import { Alerts } from '../../shared/alert.component';
import { ModalComponent } from '../../shared/modal.component';

@Component({ selector: 'app-task-form', imports: [ReactiveFormsModule, ModalComponent], template: `
  <app-modal [title]="task() ? 'Редактировать задачу' : 'Новая задача'" [busy]="pending()" (closed)="closed.emit()">
    <form [formGroup]="form" (ngSubmit)="submit()" class="task-form">
      <label>Название задачи <span class="required">*</span><input formControlName="title" maxlength="160" placeholder="Что нужно сделать?" cdkFocusInitial></label>
      <label>Описание<textarea formControlName="description" rows="4" maxlength="4000" placeholder="Добавьте контекст, детали и ожидаемый результат…"></textarea></label>
      <div class="form-grid"><label>Приоритет<select aria-label="Приоритет" formControlName="priority"><option value="LOW">Низкий</option><option value="MEDIUM">Средний</option><option value="HIGH">Высокий</option></select></label>
        <label>Исполнитель<select aria-label="Исполнитель" formControlName="assigneeId"><option value="">Не назначен</option>@for (user of users(); track user.id) { <option [value]="user.id">{{ user.name }}</option> }</select></label></div>
      <label>Срок выполнения<input type="date" formControlName="dueDate" min="1900-01-01" max="9999-12-31"></label>
      @if (form.touched && form.invalid) { <p class="field-error">Укажите название задачи (до 160 символов).</p> }
      @if (conflict()) { <p class="field-error" role="alert">Задача изменена другим участником. Ваш текст сохранён в форме. Скопируйте его, закройте окно, обновите доску и откройте задачу заново.</p> }
      <footer class="modal-footer"><button type="button" class="button secondary" [disabled]="pending()" (click)="closed.emit()">Отмена</button>
        <button class="button primary" [disabled]="pending() || conflict()">{{ pending() ? 'Сохраняем…' : task() ? 'Сохранить изменения' : 'Создать задачу' }}</button></footer>
    </form>
  </app-modal>` })
export class TaskFormComponent implements OnInit {
  readonly task = input<Task | null>(null); readonly users = input.required<User[]>();
  readonly closed = output(); readonly saved = output<Task>();
  readonly pending = signal(false); readonly conflict = signal(false);
  private readonly api = inject(TaskApi); private readonly alerts = inject(Alerts);
  readonly form = inject(FormBuilder).nonNullable.group({ title: ['', [Validators.required, Validators.maxLength(160), Validators.pattern(/\S/)]],
    description: ['', Validators.maxLength(4000)], priority: ['MEDIUM' as Priority], assigneeId: [''], dueDate: [''] });
  ngOnInit() { const t = this.task(); if (t) this.form.patchValue({ ...t, assigneeId: t.assignee?.id ?? '', dueDate: t.dueDate ?? '' }); }
  submit() {
    this.form.markAllAsTouched(); if (this.form.invalid || this.pending() || this.conflict()) return;
    this.pending.set(true); const value = this.form.getRawValue();
    const payload = { ...value, title: value.title.trim(), assigneeId: value.assigneeId || null, dueDate: value.dueDate || null };
    const task = this.task(); const request = task ? this.api.update(task, payload) : this.api.create(payload);
    request.pipe(finalize(() => this.pending.set(false))).subscribe({ next: result => { this.alerts.show(task ? 'Изменения сохранены' : 'Задача создана'); this.saved.emit(result); },
      error: error => { if (error.status === 409) this.conflict.set(true); this.alerts.error(error); } });
  }
}
