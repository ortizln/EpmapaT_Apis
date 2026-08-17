import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { AccessControlService } from '../../core/auth/access-control.service';
import { AppAlertService } from '../../core/services/app-alert.service';
import { PermisoSistema, RolSistema } from '../../models/auth.model';
import { AppModalComponent } from '../../shared/components/app-modal/app-modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-roles-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, AppModalComponent, HasPermissionDirective],
  templateUrl: './roles-page.component.html',
  styleUrl: './roles-page.component.scss'
})
export class RolesPageComponent {
  private readonly accessControl = inject(AccessControlService);
  private readonly alerts = inject(AppAlertService);
  private readonly roleFilterSignal = signal('TODOS');
  private readonly selectedRoleSignal = signal<RolSistema | null>(null);

  protected readonly roles = this.accessControl.rolesCatalogo;
  protected readonly permisosAgrupados = this.accessControl.permisosAgrupados;
  protected readonly filteredRoles = computed(() => {
    const roleFilter = this.roleFilterSignal();
    const roles = this.roles();
    return roleFilter === 'TODOS' ? roles : roles.filter((role) => role.codigo === roleFilter);
  });
  protected editing = false;
  protected editNombre = '';
  protected editDescripcion = '';
  protected readonly selectedPermisosSignal = signal<string[]>([]);

  constructor() {
    this.accessControl.cargarCatalogos();
  }

  protected onRoleFilterChange(value: string): void {
    this.roleFilterSignal.set(value);
  }

  protected get roleFilter(): string {
    return this.roleFilterSignal();
  }

  protected hasPermission(role: RolSistema, permissionCode: string): boolean {
    return role.permisos.includes(permissionCode);
  }

  protected abrirEdicion(role: RolSistema): void {
    this.selectedRoleSignal.set(role);
    this.editNombre = role.nombre;
    this.editDescripcion = role.descripcion;
    this.selectedPermisosSignal.set([...role.permisos]);
  }

  protected cerrarEdicion(): void {
    this.selectedRoleSignal.set(null);
    this.editNombre = '';
    this.editDescripcion = '';
    this.selectedPermisosSignal.set([]);
    this.editing = false;
  }

  protected togglePermiso(permiso: PermisoSistema, checked: boolean): void {
    this.selectedPermisosSignal.update((permisos) => {
      if (checked) {
        return [...new Set([...permisos, permiso.codigo])];
      }

      return permisos.filter((value) => value !== permiso.codigo);
    });
  }

  protected permisoSeleccionado(codigo: string): boolean {
    return this.selectedPermisosSignal().includes(codigo);
  }

  protected guardarRol(): void {
    const selectedRole = this.selectedRoleSignal();
    if (!selectedRole) {
      return;
    }

    if (!this.editNombre.trim() || !this.editDescripcion.trim() || !this.selectedPermisosSignal().length) {
      this.alerts.warning('Datos incompletos', 'Ingresa nombre, descripcion y al menos un permiso.');
      return;
    }

    this.editing = true;
    this.accessControl
      .actualizarRol({
        ...selectedRole,
        nombre: this.editNombre.trim(),
        descripcion: this.editDescripcion.trim(),
        permisos: this.selectedPermisosSignal()
      })
      .pipe(
        catchError(() => {
          this.alerts.error('Rol no actualizado', 'No se pudo guardar la configuracion del rol.');
          return of(null);
        }),
        finalize(() => {
          this.editing = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.alerts.success('Rol actualizado', `El rol ${response.nombre} fue actualizado correctamente.`);
        this.cerrarEdicion();
      });
  }

  protected get selectedRole(): RolSistema | null {
    return this.selectedRoleSignal();
  }
}
