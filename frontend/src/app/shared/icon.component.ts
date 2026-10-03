import { Component, input } from '@angular/core';
const PATHS: Record<string, string> = {
  board: 'M3 3h7v18H3z M14 3h7v10h-7z M14 17h7v4h-7z',
  team: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M13 7a4 4 0 1 1-8 0a4 4 0 0 1 8 0',
  search: 'M21 21l-4.35-4.35 M19 11a8 8 0 1 1-16 0a8 8 0 0 1 16 0',
  plus: 'M12 5v14 M5 12h14', arrow: 'M7 17L17 7 M7 7h10v10',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
  calendar: 'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2',
  check: 'M20 6L9 17l-5-5', clock: 'M12 8v4l3 2 M22 12a10 10 0 1 1-20 0a10 10 0 0 1 20 0',
  refresh: 'M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 13 2l1 4 M4 12l1 4a8 8 0 0 0 13 2',
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z'
};
@Component({ selector: 'app-icon', template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path()" /></svg>', styles: ':host{display:inline-flex;width:18px;height:18px;flex-shrink:0}svg{width:100%;height:100%}' })
export class IconComponent { readonly name = input('board'); path() { return PATHS[this.name()] ?? PATHS['board']; } }
