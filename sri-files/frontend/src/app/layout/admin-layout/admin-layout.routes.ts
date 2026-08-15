import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';
import { permissionGuard } from '../../core/guards/permission.guard';

export const adminLayoutRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('../../features/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent)
      },
      {
        path: 'documentos',
        loadComponent: () =>
          import('../../features/documentos/documentos-page.component').then((m) => m.DocumentosPageComponent)
      },
      {
        path: 'documentos/nuevo',
        loadComponent: () =>
          import('../../features/documentos/documento-recepcion-page.component').then((m) => m.DocumentoRecepcionPageComponent)
      },
      {
        path: 'facturacion',
        loadComponent: () =>
          import('../../features/facturacion/facturacion-page.component').then((m) => m.FacturacionPageComponent)
      },
      {
        path: 'facturacion/nuevo',
        loadComponent: () =>
          import('../../features/facturacion/factura-form-page.component').then((m) => m.FacturaFormPageComponent)
      },
      {
        path: 'retenciones',
        loadComponent: () =>
          import('../../features/retenciones/retenciones-page.component').then((m) => m.RetencionesPageComponent)
      },
      {
        path: 'retenciones/nuevo',
        loadComponent: () =>
          import('../../features/retenciones/retencion-form-page.component').then((m) => m.RetencionFormPageComponent)
      },
      {
        path: 'guias-remision',
        loadComponent: () =>
          import('../../features/guias-remision/guias-remision-page.component').then((m) => m.GuiasRemisionPageComponent)
      },
      {
        path: 'guias-remision/nuevo',
        loadComponent: () =>
          import('../../features/guias-remision/guia-remision-form-page.component').then((m) => m.GuiaRemisionFormPageComponent)
      },
      {
        path: 'notas-credito',
        loadComponent: () =>
          import('../../features/notas-credito/notas-credito-page.component').then((m) => m.NotasCreditoPageComponent)
      },
      {
        path: 'notas-credito/nuevo',
        loadComponent: () =>
          import('../../features/notas-credito/nota-credito-form-page.component').then((m) => m.NotaCreditoFormPageComponent)
      },
      {
        path: 'notas-debito',
        loadComponent: () =>
          import('../../features/notas-debito/notas-debito-page.component').then((m) => m.NotasDebitoPageComponent)
      },
      {
        path: 'notas-debito/nuevo',
        loadComponent: () =>
          import('../../features/notas-debito/nota-debito-form-page.component').then((m) => m.NotaDebitoFormPageComponent)
      },
      {
        path: 'liquidaciones-compra',
        loadComponent: () =>
          import('../../features/liquidaciones-compra/liquidaciones-compra-page.component').then((m) => m.LiquidacionesCompraPageComponent)
      },
      {
        path: 'liquidaciones-compra/nuevo',
        loadComponent: () =>
          import('../../features/liquidaciones-compra/liquidacion-compra-form-page.component').then((m) => m.LiquidacionCompraFormPageComponent)
      },
      {
        path: 'documentos/:id',
        loadComponent: () =>
          import('../../features/documentos/documento-detalle-page.component').then((m) => m.DocumentoDetallePageComponent)
      },
      {
        path: 'catalogos',
        loadComponent: () =>
          import('../../features/catalogos-admin/catalogos-admin-page.component').then((m) => m.CatalogosAdminPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/empresas/nuevo',
        loadComponent: () =>
          import('../../features/empresas/empresa-form-page.component').then((m) => m.EmpresaFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/empresas/:id/editar',
        loadComponent: () =>
          import('../../features/empresas/empresa-form-page.component').then((m) => m.EmpresaFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/establecimientos/nuevo',
        loadComponent: () =>
          import('../../features/establecimientos/establecimiento-form-page.component').then((m) => m.EstablecimientoFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/establecimientos/:id/editar',
        loadComponent: () =>
          import('../../features/establecimientos/establecimiento-form-page.component').then((m) => m.EstablecimientoFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/puntos-emision/nuevo',
        loadComponent: () =>
          import('../../features/puntos-emision/punto-emision-form-page.component').then((m) => m.PuntoEmisionFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/puntos-emision/:id/editar',
        loadComponent: () =>
          import('../../features/puntos-emision/punto-emision-form-page.component').then((m) => m.PuntoEmisionFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'catalogos/secuenciales/editar',
        loadComponent: () =>
          import('../../features/secuenciales/secuencial-form-page.component').then((m) => m.SecuencialFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'empresas',
        loadComponent: () =>
          import('../../features/catalogos-admin/catalogos-admin-page.component').then((m) => m.CatalogosAdminPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN', tab: 'empresas' }
      },
      {
        path: 'establecimientos',
        loadComponent: () =>
          import('../../features/catalogos-admin/catalogos-admin-page.component').then((m) => m.CatalogosAdminPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN', tab: 'establecimientos' }
      },
      {
        path: 'puntos-emision',
        loadComponent: () =>
          import('../../features/catalogos-admin/catalogos-admin-page.component').then((m) => m.CatalogosAdminPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN', tab: 'puntos-emision' }
      },
      {
        path: 'secuenciales',
        loadComponent: () =>
          import('../../features/catalogos-admin/catalogos-admin-page.component').then((m) => m.CatalogosAdminPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN', tab: 'secuenciales' }
      },
      {
        path: 'usuarios',
        loadComponent: () =>
          import('../../features/usuarios/usuarios-page.component').then((m) => m.UsuariosPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'usuarios/nuevo',
        loadComponent: () =>
          import('../../features/usuarios/usuario-form-page.component').then((m) => m.UsuarioFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'usuarios/:id/editar',
        loadComponent: () =>
          import('../../features/usuarios/usuario-form-page.component').then((m) => m.UsuarioFormPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'administracion/certificados',
        loadComponent: () =>
          import('../../features/certificados/certificados-page.component').then((m) => m.CertificadosPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'administracion/configuracion-correo',
        loadComponent: () =>
          import('../../features/configuracion-correo/configuracion-correo-page.component').then((m) => m.ConfiguracionCorreoPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'control/errores',
        loadComponent: () =>
          import('../../features/control-errores/control-errores-page.component').then((m) => m.ControlErroresPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      },
      {
        path: 'control/correos',
        loadComponent: () =>
          import('../../features/control-correos/control-correos-page.component').then((m) => m.ControlCorreosPageComponent),
        canActivate: [permissionGuard],
        data: { role: 'ADMIN' }
      }
    ]
  }
];
