import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { expand, EMPTY, map, reduce } from 'rxjs';
import { Column, Role, Task, TaskInput, TaskPage, TaskStatus, User } from './models';
@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);
  // Fetch every page, so the board never silently hides tasks beyond the API page size.
  tasks() {
    const page = (number: number) => this.http.get<TaskPage>(`/api/tasks?page=${number}&size=100`);
    return page(0).pipe(expand(result => result.number + 1 < result.totalPages ? page(result.number + 1) : EMPTY),
      map(result => result.content), reduce((all, tasks) => [...all, ...tasks], [] as Task[]));
  }
  users() { return this.http.get<User[]>('/api/users'); }
  statuses() { return this.http.get<Column[]>('/api/statuses'); }
  create(input: TaskInput) { return this.http.post<Task>('/api/tasks', input); }
  update(task: Task, input: TaskInput) { return this.http.put<Task>(`/api/tasks/${task.id}`, { ...input, version: task.version }); }
  move(task: Task, status: TaskStatus) { return this.http.patch<Task>(`/api/tasks/${task.id}/status`, { status, version: task.version }); }
  delete(task: Task) { return this.http.delete<void>(`/api/tasks/${task.id}?version=${task.version}`); }
  createUser(input: { name: string; email: string; password: string; role: Role }) { return this.http.post<User>('/api/users', input); }
}
