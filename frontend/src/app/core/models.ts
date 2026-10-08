export type Role = 'admin' | 'user';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export interface User { id: string; name: string; email: string; role: Role; }
export interface Session { accessToken: string; expiresAt: string; user: User; }
export interface Task { id: string; title: string; description: string; status: TaskStatus; priority: Priority; assignee: User | null; reporter: User; dueDate: string | null; createdAt: string; updatedAt: string; version: number; }
export interface TaskInput { title: string; description: string; priority: Priority; assigneeId: string | null; dueDate: string | null; }
export interface TaskPage { content: Task[]; totalElements: number; totalPages: number; number: number; }
export interface Column { id: TaskStatus; label: string; }
export const PRIORITIES: Record<Priority, string> = { HIGH: 'Высокий', MEDIUM: 'Средний', LOW: 'Низкий' };
export const initials = (name: string) => name.split(' ').map(part => part[0]).slice(0, 2).join('');
