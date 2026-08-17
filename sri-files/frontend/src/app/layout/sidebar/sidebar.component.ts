import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AccessControlService } from '../../core/auth/access-control.service';
import { AuthStore } from '../../core/auth/auth.store';
import { AppUiService } from '../../core/services/app-ui.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  protected readonly accessControl = inject(AccessControlService);
  protected readonly authStore = inject(AuthStore);
  protected readonly ui = inject(AppUiService);
}
