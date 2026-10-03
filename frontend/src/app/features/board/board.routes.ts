import { Routes } from '@angular/router';
export const BOARD_ROUTES: Routes = [{ path: '', loadComponent: () => import('./board.component').then(m => m.BoardComponent) }];
