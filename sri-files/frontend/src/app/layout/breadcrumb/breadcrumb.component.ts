import { Component, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AsyncPipe, CommonModule } from '@angular/common';

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './breadcrumb.component.html',
  styleUrl: './breadcrumb.component.scss'
})
export class BreadcrumbComponent {
  private readonly router = inject(Router);
  private readonly routeLabels: Record<string, string> = {
    dashboard: 'Dashboard',
    documentos: 'Documentos',
    nuevo: 'Nuevo registro',
    facturacion: 'Facturacion',
    retenciones: 'Retenciones',
    'guias-remision': 'Guias de remision',
    'notas-credito': 'Notas de credito',
    'notas-debito': 'Notas de debito',
    'liquidaciones-compra': 'Liquidaciones de compra',
    catalogos: 'Catalogos operativos',
    empresas: 'Empresas',
    establecimientos: 'Establecimientos',
    'puntos-emision': 'Puntos de emision',
    secuenciales: 'Secuenciales',
    usuarios: 'Usuarios',
    seguridad: 'Seguridad',
    roles: 'Roles y permisos',
    administracion: 'Administracion',
    certificados: 'Certificados',
    'configuracion-correo': 'Configuracion de correo',
    control: 'Control',
    errores: 'Errores',
    correos: 'Correos',
    monitoreo: 'Monitoreo',
    auditoria: 'Auditoria',
    'auditoria-documentos': 'Auditoria documental',
    'configuracion-sri': 'Configuracion SRI',
    'plantillas-ride': 'Plantillas RIDE'
  };

  protected readonly breadcrumbItems$ = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd),
    startWith(null),
    map(() => {
      const segments = this.router.url.split('?')[0].split('/').filter(Boolean);
      return ['Inicio', ...segments.map((segment, index) => {
        const normalizedSegment = /^\d+$/.test(segment) ? segments[index - 1] ?? segment : segment;
        return this.routeLabels[normalizedSegment] ?? this.formatSegment(normalizedSegment);
      })];
    })
  );

  private formatSegment(segment: string): string {
    return segment
      .split('-')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
