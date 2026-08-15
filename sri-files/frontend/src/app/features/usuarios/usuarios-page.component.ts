import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AuthStore } from '../../core/auth/auth.store';
import { UsuariosService } from '../../core/services/usuarios.service';
import { UsuarioAuditoria, UsuarioSistema } from '../../models/auth.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { AppModalComponent } from '../../shared/components/app-modal/app-modal.component';

@Component({
  selector: 'app-usuarios-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, AppModalComponent],
  templateUrl: './usuarios-page.component.html',
  styleUrl: './usuarios-page.component.scss'
})
export class UsuariosPageComponent {
  private readonly usuariosService = inject(UsuariosService);
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);

  protected usuarios: UsuarioSistema[] = [];
  protected auditoriaUsuario: UsuarioAuditoria[] = [];
  protected usuarioAuditoriaSeleccionado: UsuarioSistema | null = null;
  protected loading = false;
  protected loadingAuditoria = false;
  protected processingUserId: string | null = null;
  protected errorListado = '';
  protected errorAuditoria = '';
  protected page = 0;
  protected size = 10;
  protected totalPages = 0;
  protected totalItems = 0;
  protected readonly sizeOptions = [10, 20, 50];

  constructor() {
    this.cargarUsuarios();
  }

  protected irANuevoUsuario(): void {
    this.router.navigate(['/usuarios/nuevo']);
  }

  protected irAEditarUsuario(usuario: UsuarioSistema): void {
    this.router.navigate([`/usuarios/${usuario.id}/editar`], {
      state: { usuario }
    });
  }

  protected cargarUsuarios(): void {
    this.loading = true;
    this.errorListado = '';

    this.usuariosService
      .listar(this.page, this.size)
      .pipe(
        catchError(() => {
          this.errorListado = 'No fue posible cargar los usuarios desde el backend.';
          this.usuarios = [];
          this.totalItems = 0;
          this.totalPages = 0;
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.usuarios = response.items;
        this.page = response.page;
        this.size = response.size;
        this.totalPages = response.totalPages;
        this.totalItems = response.totalItems;
      });
  }

  protected onPageChange(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }

    this.page = page;
    this.cargarUsuarios();
  }

  protected onSizeChange(event: Event): void {
    const nextSize = Number((event.target as HTMLSelectElement).value);
    if (Number.isNaN(nextSize) || nextSize === this.size) {
      return;
    }

    this.size = nextSize;
    this.page = 0;
    this.cargarUsuarios();
  }

  protected toggleEstado(usuario: UsuarioSistema): void {
    this.errorListado = '';
    this.processingUserId = usuario.id;

    this.usuariosService
      .actualizarEstado(usuario.id, !usuario.activo)
      .pipe(
        catchError(() => {
          this.errorListado = `No fue posible actualizar el estado de ${usuario.username}.`;
          return of(null);
        }),
        finalize(() => {
          this.processingUserId = null;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.usuarios = this.usuarios.map((item) => (item.id === response.id ? response : item));
        if (this.usuarioAuditoriaSeleccionado?.id === response.id) {
          this.cargarAuditoria(response);
        }
      });
  }

  protected abrirAuditoria(usuario: UsuarioSistema): void {
    this.usuarioAuditoriaSeleccionado = usuario;
    this.cargarAuditoria(usuario);
  }

  private cargarAuditoria(usuario: UsuarioSistema): void {
    this.errorAuditoria = '';
    this.loadingAuditoria = true;

    this.usuariosService
      .obtenerAuditoria(usuario.id)
      .pipe(
        catchError(() => {
          this.errorAuditoria = `No fue posible cargar la auditoria de ${usuario.username}.`;
          this.auditoriaUsuario = [];
          return of(null);
        }),
        finalize(() => {
          this.loadingAuditoria = false;
        })
      )
      .subscribe((response) => {
        if (response) {
          this.auditoriaUsuario = response;
        }
      });
  }

  protected cerrarAuditoria(): void {
    this.usuarioAuditoriaSeleccionado = null;
    this.auditoriaUsuario = [];
    this.errorAuditoria = '';
  }

  protected isSelfAdminDeactivateBlocked(usuario: UsuarioSistema): boolean {
    const actual = this.authStore.usuario();
    return !!actual && actual.id === usuario.id && usuario.activo && actual.roles.includes('ADMIN');
  }

  protected visiblePages(): number[] {
    if (this.totalPages <= 1) {
      return [0];
    }

    const start = Math.max(0, this.page - 1);
    const end = Math.min(this.totalPages - 1, start + 2);
    const adjustedStart = Math.max(0, end - 2);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }

  protected getRangeStart(): number {
    return this.totalItems === 0 ? 0 : this.page * this.size + 1;
  }

  protected getRangeEnd(): number {
    return Math.min((this.page + 1) * this.size, this.totalItems);
  }
}
