import { CommonModule } from '@angular/common';
import { Component, effect, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { DocumentoContratoService } from '../../core/services/documento-contrato.service';
import { DocumentoListadoResponse, DocumentoResumen, TipoDocumento } from '../../models/documento.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { DocumentosRecientesTableComponent } from './documentos-recientes-table.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-documentos-tipo-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, DocumentosRecientesTableComponent, HasPermissionDirective],
  templateUrl: './documentos-tipo-page.component.html'
})
export class DocumentosTipoPageComponent {
  private readonly companyContext = inject(CompanyContextService);
  private readonly documentoService = inject(DocumentoContratoService);
  private readonly router = inject(Router);
  private debounceBusquedaTimer: ReturnType<typeof setTimeout> | null = null;

  readonly tipoDocumento = input.required<TipoDocumento>();
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly newRoute = input<string | null>(null);
  readonly newLabel = input('Nuevo documento');

  protected documentos: DocumentoResumen[] = [];
  protected filtroEstado = '';
  protected filtroBusqueda = '';
  protected page = 0;
  protected size = 10;
  protected totalPages = 0;
  protected totalItems = 0;
  protected loading = false;
  protected error = '';

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      this.companyContext.empresaActiva()?.id;
      this.page = 0;
      this.cargarBandeja();
    });
  }

  protected readonly tiposDocumentoVisibles = () => [this.tipoDocumento()];

  protected noop(): void {}

  protected onEstadoChange(value: string): void {
    this.filtroEstado = value;
    this.page = 0;
    this.cargarBandeja();
  }

  protected onBusquedaChange(value: string): void {
    this.filtroBusqueda = value;
    this.page = 0;

    if (this.debounceBusquedaTimer) {
      clearTimeout(this.debounceBusquedaTimer);
    }

    this.debounceBusquedaTimer = setTimeout(() => {
      this.cargarBandeja();
    }, 350);
  }

  protected onPageChange(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }

    this.page = page;
    this.cargarBandeja();
  }

  protected onSizeChange(size: number): void {
    if (size === this.size) {
      return;
    }

    this.size = size;
    this.page = 0;
    this.cargarBandeja();
  }

  protected irADetalle(uuid: string): void {
    this.router.navigate(['/documentos', uuid]);
  }

  protected irANuevoRegistro(): void {
    if (!this.newRoute()) {
      return;
    }

    this.router.navigate([this.newRoute()]);
  }

  private cargarBandeja(): void {
    this.loading = true;
    this.error = '';

    this.documentoService
      .listarDocumentos({
        empresaUuid: this.companyContext.empresaActiva()?.id || undefined,
        tipoDocumento: this.tipoDocumento(),
        estado: this.filtroEstado || undefined,
        busqueda: this.filtroBusqueda || undefined,
        page: this.page,
        size: this.size
      })
      .pipe(
        catchError(() => {
          this.error = `No fue posible cargar la bandeja de ${this.title().toLowerCase()} desde el backend.`;
          this.documentos = [];
          this.totalItems = 0;
          this.totalPages = 0;
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response: DocumentoListadoResponse | null) => {
        if (!response) {
          return;
        }

        this.documentos = response.items.map((item) => ({
          ...item,
          razonSocial: item.razonSocial || 'N/D',
          fechaEmision: item.fechaEmision || 'N/D'
        }));
        this.page = response.page;
        this.totalPages = response.totalPages;
        this.totalItems = response.totalItems;
      });
  }
}
