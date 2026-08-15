import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthStore } from '../../core/auth/auth.store';
import { AuthService } from '../../core/auth/auth.service';
import { AppUiService } from '../../core/services/app-ui.service';
import { CompanyContextService } from '../../core/services/company-context.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  protected readonly authStore = inject(AuthStore);
  protected readonly ui = inject(AppUiService);
  protected readonly companyContext = inject(CompanyContextService);
  private readonly authService = inject(AuthService);

  constructor() {
    this.companyContext.cargarEmpresas();
  }

  logout(): void {
    this.authService.logout();
  }

  seleccionarEmpresa(event: Event): void {
    const target = event.target as HTMLSelectElement | null;
    const id = target?.value?.trim();
    if (!id) {
      return;
    }

    this.companyContext.seleccionarEmpresa(id);
  }
}
