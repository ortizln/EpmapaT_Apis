import { Component, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AsyncPipe } from '@angular/common';

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [AsyncPipe],
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
    administracion: 'Administracion',
    certificados: 'Certificados',
    'configuracion-correo': 'Configuracion de correo',
    control: 'Control',
    errores: 'Errores',
    correos: 'Correos'
  };

  protected readonly currentSection$ = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd),
    startWith(null),
    map(() => {
      const segments = this.router.url.split('?')[0].split('/').filter(Boolean);
      const lastSegment = segments.at(-1) ?? 'dashboard';
      const normalizedSegment = /^\d+$/.test(lastSegment) ? segments.at(-2) ?? 'dashboard' : lastSegment;
      return this.routeLabels[normalizedSegment] ?? this.formatSegment(normalizedSegment);
    })
  );

  private formatSegment(segment: string): string {
    return segment
      .split('-')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
