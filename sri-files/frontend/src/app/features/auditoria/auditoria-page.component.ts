import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { DocumentoAuditoriaService } from '../../core/services/documento-auditoria.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { RolesService } from '../../core/services/roles.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

interface AuditoriaResumenCard {
  titulo: string;
  descripcion: string;
  total: number;
  ruta: string;
  permiso: string;
  ultimaActividad: string;
}

@Component({
  selector: 'app-auditoria-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './auditoria-page.component.html',
  styleUrl: './auditoria-page.component.scss'
})
export class AuditoriaPageComponent {
  private readonly documentoAuditoriaService = inject(DocumentoAuditoriaService);
  private readonly usuariosService = inject(UsuariosService);
  private readonly rolesService = inject(RolesService);
  private readonly empresasService = inject(EmpresasService);

  protected cards: AuditoriaResumenCard[] = [];
  protected loading = false;
  protected error = '';

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      documentos: this.documentoAuditoriaService.obtenerAuditoriaReciente(),
      usuarios: this.usuariosService.obtenerAuditoriaReciente(0, 1),
      roles: this.rolesService.obtenerAuditoriaReciente(0, 1),
      empresas: this.empresasService.obtenerAuditoriaReciente(0, 1)
    })
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar el resumen central de auditoria.';
          this.cards = [];
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

        this.cards = [
          {
            titulo: 'Documentos',
            descripcion: 'Transiciones y eventos del flujo documental.',
            total: response.documentos.totalEventos,
            ruta: '/control/auditoria-documentos',
            permiso: 'DOCUMENTO_AUDITORIA_VER',
            ultimaActividad: response.documentos.eventos[0]?.createdAt ?? 'Sin actividad'
          },
          {
            titulo: 'Usuarios',
            descripcion: 'Altas, cambios de estado y acciones de acceso.',
            total: response.usuarios.totalItems,
            ruta: '/control/auditoria-usuarios',
            permiso: 'USUARIO_AUDITORIA_VER',
            ultimaActividad: response.usuarios.items[0]?.fecha ?? 'Sin actividad'
          },
          {
            titulo: 'Roles',
            descripcion: 'Cambios sobre matriz de roles y permisos.',
            total: response.roles.totalItems,
            ruta: '/control/auditoria-roles',
            permiso: 'ROL_AUDITORIA_VER',
            ultimaActividad: response.roles.items[0]?.fecha ?? 'Sin actividad'
          },
          {
            titulo: 'Empresas',
            descripcion: 'Cambios administrativos y configuraciones sensibles.',
            total: response.empresas.totalItems,
            ruta: '/control/auditoria-empresas',
            permiso: 'EMPRESA_AUDITORIA_VER',
            ultimaActividad: response.empresas.items[0]?.fecha ?? 'Sin actividad'
          }
        ];
      });
  }
}
