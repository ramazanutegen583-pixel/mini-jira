import { Routes } from '@angular/router';
import { authGuard } from './core/auth';
export const routes: Routes = [
  { path: 'auth', loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES) },
  { path: 'board', canActivate: [authGuard], loadChildren: () => import('./features/board/board.routes').then(m => m.BOARD_ROUTES) },
  { path: '', pathMatch: 'full', redirectTo: 'board' },
  { path: '**', redirectTo: 'board' }
];
