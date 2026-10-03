import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AlertComponent } from './shared/alert.component';
@Component({ selector: 'app-root', imports: [RouterOutlet, AlertComponent], template: '<router-outlet /><app-alert />' })
export class AppComponent {}
