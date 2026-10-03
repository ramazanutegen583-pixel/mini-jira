import { Component, HostListener, input, output } from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';
@Component({ selector: 'app-modal', imports: [A11yModule], template: `
  <div class="modal-backdrop" (click)="backdrop($event)">
    <section class="modal" role="dialog" aria-modal="true" [attr.aria-label]="title()" cdkTrapFocus [cdkTrapFocusAutoCapture]="true">
      <header class="modal-header"><div><span class="eyebrow">TEAMFLOW WORKSPACE</span><h2>{{ title() }}</h2></div>
        <button type="button" class="icon-button" [disabled]="busy()" (click)="closed.emit()" aria-label="Закрыть окно">×</button></header>
      <ng-content />
    </section>
  </div>` })
export class ModalComponent {
  readonly title = input.required<string>(); readonly busy = input(false); readonly closed = output();
  @HostListener('document:keydown.escape') escape() { if (!this.busy()) this.closed.emit(); }
  backdrop(event: MouseEvent) { if (event.target === event.currentTarget && !this.busy()) this.closed.emit(); }
}
