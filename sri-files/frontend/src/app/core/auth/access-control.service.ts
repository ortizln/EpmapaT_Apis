import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';
import { AuthStore } from './auth.store';
import { PermisoSistema, RolSistema } from '../../models/auth.model';
import { RolesService } from '../services/roles.service';

@Injectable({
  providedIn: 'root'
})
export class AccessControlService {
  private readonly rolesService = inject(RolesService);
  private readonly authStore = inject(AuthStore);

  private readonly permisosFallback: PermisoSistema[] = [
    { codigo: 'DASHBOARD_VER', nombre: 'Ver dashboard', descripcion: 'Permite consultar indicadores y resumenes operativos.', categoria: 'Dashboard' },
    { codigo: 'DOCUMENTO_VER', nombre: 'Ver documentos', descripcion: 'Permite consultar la bandeja documental y sus detalles.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_CREAR', nombre: 'Crear documentos', descripcion: 'Permite registrar nuevas recepciones documentales.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_AUTORIZACION_CONSULTAR', nombre: 'Consultar autorizacion', descripcion: 'Permite lanzar consultas manuales al flujo de autorizacion.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_REPROCESAR', nombre: 'Reprocesar documentos', descripcion: 'Permite programar reprocesamientos manuales del flujo documental.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_REGENERAR_RIDE', nombre: 'Regenerar RIDE', descripcion: 'Permite regenerar manualmente el RIDE del documento.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_DESCARGAR', nombre: 'Descargar archivos', descripcion: 'Permite descargar XML, RIDE y archivos asociados al documento.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_CORREO_REENVIAR', nombre: 'Reenviar correo', descripcion: 'Permite reenviar comprobantes por correo.', categoria: 'Documentos' },
    { codigo: 'DOCUMENTO_AUDITORIA_VER', nombre: 'Ver auditoria documental', descripcion: 'Permite consultar el historial reciente de cambios documentales.', categoria: 'Control' },
    { codigo: 'MONITOREO_VER', nombre: 'Ver monitoreo', descripcion: 'Permite acceder al estado operativo del backend.', categoria: 'Control' },
    { codigo: 'CONTROL_ERRORES_VER', nombre: 'Ver control de errores', descripcion: 'Permite consultar incidencias del flujo.', categoria: 'Control' },
    { codigo: 'CONTROL_CORREOS_VER', nombre: 'Ver control de correos', descripcion: 'Permite revisar la operacion de notificaciones.', categoria: 'Control' },
    { codigo: 'CATALOGO_ADMINISTRAR', nombre: 'Administrar catalogos', descripcion: 'Permite gestionar empresas, establecimientos, puntos y secuenciales.', categoria: 'Administracion' },
    { codigo: 'CERTIFICADO_ADMINISTRAR', nombre: 'Administrar certificados', descripcion: 'Permite cargar y gestionar certificados.', categoria: 'Administracion' },
    { codigo: 'CONFIGURACION_CORREO_ADMINISTRAR', nombre: 'Administrar configuracion', descripcion: 'Permite editar configuracion SRI y correo.', categoria: 'Administracion' },
    { codigo: 'EMPRESA_AUDITORIA_VER', nombre: 'Ver auditoria de empresas', descripcion: 'Permite consultar la auditoria administrativa de empresas y configuraciones sensibles.', categoria: 'Administracion' },
    { codigo: 'USUARIO_VER', nombre: 'Ver usuarios', descripcion: 'Permite consultar el listado de usuarios.', categoria: 'Seguridad' },
    { codigo: 'USUARIO_CREAR', nombre: 'Crear usuarios', descripcion: 'Permite registrar nuevos usuarios.', categoria: 'Seguridad' },
    { codigo: 'USUARIO_EDITAR', nombre: 'Editar usuarios', descripcion: 'Permite actualizar estado y credenciales de usuarios.', categoria: 'Seguridad' },
    { codigo: 'USUARIO_AUDITORIA_VER', nombre: 'Ver auditoria de usuarios', descripcion: 'Permite consultar la auditoria administrativa de usuarios.', categoria: 'Seguridad' },
    { codigo: 'ROL_VER', nombre: 'Ver roles', descripcion: 'Permite consultar la matriz de roles y permisos.', categoria: 'Seguridad' },
    { codigo: 'ROL_ADMINISTRAR', nombre: 'Administrar roles', descripcion: 'Permite definir la asignacion de permisos por rol.', categoria: 'Seguridad' },
    { codigo: 'ROL_AUDITORIA_VER', nombre: 'Ver auditoria de roles', descripcion: 'Permite consultar la auditoria administrativa de roles.', categoria: 'Seguridad' }
  ];

  private readonly rolesFallback: RolSistema[] = [
    {
      codigo: 'ADMIN',
      nombre: 'Administrador',
      descripcion: 'Control total del sistema, configuracion y seguridad.',
      permisos: this.permisosFallback.map((permiso) => permiso.codigo)
    },
    {
      codigo: 'OPERADOR',
      nombre: 'Operador',
      descripcion: 'Gestion operativa documental sin acceso a configuraciones sensibles.',
      permisos: [
        'DASHBOARD_VER',
        'DOCUMENTO_VER',
        'DOCUMENTO_CREAR',
        'DOCUMENTO_AUTORIZACION_CONSULTAR',
        'DOCUMENTO_REPROCESAR',
        'DOCUMENTO_REGENERAR_RIDE',
        'DOCUMENTO_DESCARGAR',
        'DOCUMENTO_CORREO_REENVIAR'
      ]
    }
  ];

  private readonly permisosCatalogoSignal = signal<PermisoSistema[]>(this.permisosFallback);
  private readonly rolesCatalogoSignal = signal<RolSistema[]>(this.rolesFallback);

  readonly permisosCatalogo = this.permisosCatalogoSignal.asReadonly();
  readonly rolesCatalogo = this.rolesCatalogoSignal.asReadonly();

  readonly permisosAgrupados = computed(() => {
    return this.permisosCatalogo().reduce<Record<string, PermisoSistema[]>>((accumulator, permiso) => {
      accumulator[permiso.categoria] ??= [];
      accumulator[permiso.categoria].push(permiso);
      return accumulator;
    }, {});
  });

  readonly permisosUsuario = computed(() => {
    const usuario = this.authStore.usuario();
    if (!usuario) {
      return [];
    }

    if (usuario.permisos?.length) {
      return usuario.permisos;
    }

    return [...new Set(usuario.roles.flatMap((role) => this.getPermisosPorRol(role)))];
  });

  hasRole(role: string): boolean {
    return this.authStore.usuario()?.roles.includes(role) ?? false;
  }

  hasPermission(permission: string): boolean {
    return this.permisosUsuario().includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  getPermisosPorRol(role: string): string[] {
    return this.rolesCatalogo().find((item) => item.codigo === role)?.permisos ?? [];
  }

  getRolNombre(role: string): string {
    return this.rolesCatalogo().find((item) => item.codigo === role)?.nombre ?? role;
  }

  cargarCatalogos(): void {
    this.rolesService
      .listarPermisos()
      .pipe(
        tap((permisos) => {
          if (permisos.length) {
            this.permisosCatalogoSignal.set(permisos);
          }
        }),
        catchError(() => of(this.permisosFallback))
      )
      .subscribe();

    this.rolesService
      .listarRoles()
      .pipe(
        tap((roles) => {
          if (roles.length) {
            this.rolesCatalogoSignal.set(roles);
          }
        }),
        catchError(() => of(this.rolesFallback))
      )
      .subscribe();
  }

  actualizarRol(payload: RolSistema): ReturnType<RolesService['actualizarRol']> {
    return this.rolesService.actualizarRol(payload.codigo, {
      nombre: payload.nombre,
      descripcion: payload.descripcion,
      permisos: payload.permisos
    }).pipe(
      tap((rolActualizado) => {
        this.rolesCatalogoSignal.update((roles) => roles.map((role) => (role.codigo === rolActualizado.codigo ? rolActualizado : role)));
      })
    );
  }
}
