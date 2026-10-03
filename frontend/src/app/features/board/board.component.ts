import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth';
import { TaskApi } from '../../core/task-api';
import { Column, initials, PRIORITIES, Priority, Task, TaskStatus, User } from '../../core/models';
import { Alerts } from '../../shared/alert.component';
import { IconComponent } from '../../shared/icon.component';
import { ModalComponent } from '../../shared/modal.component';
import { TaskFormComponent } from './task-form.component';
import { MemberFormComponent } from './member-form.component';

@Component({ selector: 'app-board', imports: [DatePipe, FormsModule, DragDropModule, IconComponent, ModalComponent, TaskFormComponent, MemberFormComponent],
  templateUrl: './board.component.html' })
export class BoardComponent implements OnInit {
  readonly auth = inject(AuthService); private readonly api = inject(TaskApi); private readonly alerts = inject(Alerts);
  readonly tasks = signal<Task[]>([]); readonly users = signal<User[]>([]); readonly columns = signal<Column[]>([]);
  readonly loading = signal(true); readonly failed = signal(false); readonly pending = signal(false);
  readonly view = signal<'board' | 'team'>('board'); readonly search = signal(''); readonly priority = signal(''); readonly assignee = signal('');
  readonly formOpen = signal(false); readonly editing = signal<Task | null>(null); readonly memberOpen = signal(false); readonly deleting = signal<Task | null>(null);
  readonly priorities = PRIORITIES; readonly initials = initials;
  readonly visible = computed(() => this.tasks().filter(task => {
    const q = this.search().trim().toLocaleLowerCase('ru');
    return (!q || `${task.title} ${task.description} ${task.id}`.toLocaleLowerCase('ru').includes(q))
      && (!this.priority() || task.priority === this.priority())
      && (!this.assignee() || (this.assignee() === 'unassigned' ? !task.assignee : task.assignee?.id === this.assignee()));
  }));
  readonly stats = computed(() => ({ total: this.tasks().length, active: this.tasks().filter(t => t.status === 'IN_PROGRESS').length,
    done: this.tasks().filter(t => t.status === 'DONE').length, high: this.tasks().filter(t => t.priority === 'HIGH' && t.status !== 'DONE').length }));
  readonly progress = computed(() => this.stats().total ? Math.round(this.stats().done / this.stats().total * 100) : 0);
  ngOnInit() { this.load(); }
  load() {
    if (this.pending()) return;
    this.loading.set(true); this.failed.set(false);
    forkJoin({ tasks: this.api.tasks(), users: this.api.users(), columns: this.api.statuses() }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => { this.tasks.set(data.tasks); this.users.set(data.users); this.columns.set(data.columns); },
      error: error => { this.failed.set(true); this.alerts.error(error); }
    });
  }
  columnTasks(status: TaskStatus) { return this.visible().filter(task => task.status === status); }
  count(status: TaskStatus) { return this.tasks().filter(task => task.status === status).length; }
  tasksForUser(id: string) { return this.tasks().filter(task => task.assignee?.id === id).length; }
  shortId(task: Task) { return 'TF-' + task.id.slice(0, 5).toUpperCase(); }
  overdue(task: Task) { const today = new Date(); const date = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`; return task.status !== 'DONE' && !!task.dueDate && task.dueDate < date; }
  create() { this.editing.set(null); this.formOpen.set(true); }
  edit(task: Task) { if (!this.pending()) { this.editing.set(task); this.formOpen.set(true); } }
  saved(task: Task) { this.tasks.update(tasks => tasks.some(t => t.id === task.id) ? tasks.map(t => t.id === task.id ? task : t) : [task, ...tasks]); this.formOpen.set(false); }
  memberSaved(user: User) { this.users.update(users => [...users, user]); this.memberOpen.set(false); }
  drop(event: CdkDragDrop<TaskStatus>) { const task: Task = event.item.data; this.move(task, event.container.data); }
  move(task: Task, status: TaskStatus) {
    if (task.status === status || this.pending() || this.loading()) return;
    this.pending.set(true);
    this.api.move(task, status).pipe(finalize(() => this.pending.set(false))).subscribe({
      next: updated => { this.tasks.update(tasks => tasks.map(t => t.id === task.id ? updated : t)); this.alerts.show('Статус задачи обновлён'); },
      error: error => { this.alerts.error(error); if (error.status === 409 || error.status === 404) { this.pending.set(false); this.load(); } }
    });
  }
  changeStatus(task: Task, event: Event) { const element = event.target as HTMLSelectElement; const status = element.value as TaskStatus; element.value = task.status; this.move(task, status); }
  deleteTask() {
    const task = this.deleting(); if (!task || this.pending()) return;
    this.pending.set(true); this.api.delete(task).pipe(finalize(() => this.pending.set(false))).subscribe({
      next: () => { this.tasks.update(tasks => tasks.filter(t => t.id !== task.id)); this.deleting.set(null); this.alerts.show('Задача удалена'); },
      error: error => { this.alerts.error(error); this.deleting.set(null); if (error.status === 409 || error.status === 404) { this.pending.set(false); this.load(); } }
    });
  }
  resetFilters() { this.search.set(''); this.priority.set(''); this.assignee.set(''); }
}
