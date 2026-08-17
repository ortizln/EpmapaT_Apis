import { Component, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import {
  DocumentoDetalleResponse,
  DocumentoEstadoResponse,
  DocumentoListadoResponse,
  DocumentoResumen,
  TipoDocumento
} from '../../models/documento.model';
import { DocumentoContratoService } from '../../core/services/documento-contrato.service';
import { DocumentoSeguimientoComponent } from './documento-seguimiento.component';
import { DocumentosRecientesTableComponent } from './documentos-recientes-table.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-documentos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, DocumentoSeguimientoComponent, DocumentosRecientesTableComponent, HasPermissionDirective],
  templateUrl: './documentos-page.component.html',
  styleUrl: './documentos-page.component.scss'
})
export class DocumentosPageComponent {
  private readonly companyContext = inject(CompanyContextService);
  private readonly documentoContratoService = inject(DocumentoContratoService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private debounceBusquedaTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly tiposDocumento: TipoDocumento[] = [
    'FACTURA',
    'RETENCION',
    'GUIA_REMISION',
    'NOTA_CREDITO',
    'NOTA_DEBITO',
    'LIQUIDACION_COMPRA'
  ];
  protected readonly documentTypeTabs: Array<{ type: TipoDocumento; label: string; subtitle: string; route: string }> = [
    { type: 'FACTURA', label: 'Facturacion', subtitle: 'Facturas emitidas y recibidas', route: '/facturacion' },
    { type: 'RETENCION', label: 'Retenciones', subtitle: 'Comprobantes de retencion', route: '/retenciones' },
    { type: 'GUIA_REMISION', label: 'Guias remision', subtitle: 'Traslado y despacho', route: '/guias-remision' },
    { type: 'NOTA_CREDITO', label: 'Notas credito', subtitle: 'Ajustes y devoluciones', route: '/notas-credito' },
    { type: 'NOTA_DEBITO', label: 'Notas debito', subtitle: 'Recargos y diferencias', route: '/notas-debito' },
    { type: 'LIQUIDACION_COMPRA', label: 'Liquidaciones', subtitle: 'Compras a proveedores', route: '/liquidaciones-compra' }
  ];

  protected bandejaDocumentos: DocumentoResumen[] = [];
  protected filtroTipoDocumento = '';
  protected filtroEstado = '';
  protected filtroBusqueda = '';
  protected bandejaPage = 0;
  protected bandejaSize = 10;
  protected bandejaTotalPages = 0;
  protected bandejaTotalItems = 0;
  protected bandejaLoading = false;
  protected bandejaError = '';
  protected detalleDocumento: DocumentoDetalleResponse | null = null;
  protected estadoDocumento: DocumentoEstadoResponse | null = null;
  protected loadingSeguimiento = false;
  protected errorSeguimiento = '';

  protected readonly seguimientoForm = this.fb.group({
    uuid: this.fb.control('', [Validators.required])
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      this.companyContext.empresaActiva()?.id;
      this.bandejaPage = 0;
      this.cargarBandeja();
    });
  }

  protected irANuevoDocumento(): void {
    this.router.navigate(['/documentos/nuevo']);
  }

  protected irADetalleDocumento(uuid: string): void {
    this.router.navigate(['/documentos', uuid]);
  }

  protected consultarSeguimiento(uuid?: string): void {
    const targetUuid = uuid ?? this.seguimientoForm.controls['uuid'].value ?? '';
    if (!targetUuid.trim()) {
      this.seguimientoForm.controls['uuid'].markAsTouched();
      return;
    }

    this.loadingSeguimiento = true;
    this.errorSeguimiento = '';
    this.detalleDocumento = null;
    this.estadoDocumento = null;

    this.documentoContratoService
      .obtenerDocumento(targetUuid.trim())
      .pipe(
        catchError(() => {
          this.errorSeguimiento = 'No fue posible obtener el detalle del documento con el UUID indicado.';
          return of(null);
        })
      )
      .subscribe((detalle) => {
        this.detalleDocumento = detalle;
      });

    this.documentoContratoService
      .obtenerEstado(targetUuid.trim())
      .pipe(
        catchError(() => {
          this.errorSeguimiento = 'No fue posible consultar el estado del documento con el UUID indicado.';
          return of(null);
        }),
        finalize(() => {
          this.loadingSeguimiento = false;
        })
      )
      .subscribe((estado) => {
        this.estadoDocumento = estado;
      });
  }

  protected onFiltroTipoDocumentoChange(value: string): void {
    this.filtroTipoDocumento = value;
    this.bandejaPage = 0;
    this.cargarBandeja();
  }

  protected onFiltroEstadoChange(value: string): void {
    this.filtroEstado = value;
    this.bandejaPage = 0;
    this.cargarBandeja();
  }

  protected onFiltroBusquedaChange(value: string): void {
    this.filtroBusqueda = value;
    this.bandejaPage = 0;

    if (this.debounceBusquedaTimer) {
      clearTimeout(this.debounceBusquedaTimer);
    }

    this.debounceBusquedaTimer = setTimeout(() => {
      this.cargarBandeja();
    }, 350);
  }

  protected onBandejaPageChange(page: number): void {
    if (page < 0 || (this.bandejaTotalPages > 0 && page >= this.bandejaTotalPages)) {
      return;
    }

    this.bandejaPage = page;
    this.cargarBandeja();
  }

  protected onBandejaSizeChange(size: number): void {
    if (size === this.bandejaSize) {
      return;
    }

    this.bandejaSize = size;
    this.bandejaPage = 0;
    this.cargarBandeja();
  }

  protected abrirModulo(route: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate([route]);
  }

  private cargarBandeja(): void {
    this.bandejaLoading = true;
    this.bandejaError = '';

    this.documentoContratoService
      .listarDocumentos({
        empresaUuid: this.companyContext.empresaActiva()?.id || undefined,
        tipoDocumento: this.filtroTipoDocumento || undefined,
        estado: this.filtroEstado || undefined,
        busqueda: this.filtroBusqueda || undefined,
        page: this.bandejaPage,
        size: this.bandejaSize
      })
      .pipe(
        catchError(() => {
          this.bandejaError = 'No fue posible cargar la bandeja de documentos desde el backend.';
          this.bandejaDocumentos = [];
          this.bandejaTotalItems = 0;
          this.bandejaTotalPages = 0;
          return of(null);
        }),
        finalize(() => {
          this.bandejaLoading = false;
        })
      )
      .subscribe((response: DocumentoListadoResponse | null) => {
        if (!response) {
          return;
        }

        this.bandejaDocumentos = response.items.map((item) => ({
          ...item,
          razonSocial: item.razonSocial || 'N/D',
          fechaEmision: item.fechaEmision || 'N/D'
        }));
        this.bandejaPage = response.page;
        this.bandejaTotalPages = response.totalPages;
        this.bandejaTotalItems = response.totalItems;
      });
  }
}
