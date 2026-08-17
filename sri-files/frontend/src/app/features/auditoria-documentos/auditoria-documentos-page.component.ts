import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Component, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { DocumentoAuditoriaService } from '../../core/services/documento-auditoria.service';
import { DocumentoAuditoriaEventoResponse, DocumentoAuditoriaResumenResponse } from '../../models/documento.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-auditoria-documentos-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './auditoria-documentos-page.component.html',
  styleUrl: './auditoria-documentos-page.component.scss'
})
export class AuditoriaDocumentosPageComponent {
  private readonly auditoriaService = inject(DocumentoAuditoriaService);
  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(10);
  private readonly filtroTipoSignal = signal('TODOS');
  private readonly filtroOrigenSignal = signal('TODOS');
  private readonly filtroEstadoSignal = signal('TODOS');
  private readonly busquedaSignal = signal('');

  protected data: DocumentoAuditoriaResumenResponse | null = null;
  protected loading = false;
  protected error = '';
  protected readonly sizeOptions = [10, 20, 50];
  protected readonly filteredItems = computed(() => {
    const filtroTipo = this.filtroTipoSignal();
    const filtroOrigen = this.filtroOrigenSignal();
    const filtroEstado = this.filtroEstadoSignal();
    const busqueda = this.busquedaSignal().trim().toLowerCase();

    return (this.data?.eventos ?? []).filter((item) => {
      const matchesTipo = filtroTipo === 'TODOS' || item.tipoDocumento === filtroTipo;
      const matchesOrigen = filtroOrigen === 'TODOS' || (item.origen ?? 'SIN_ORIGEN') === filtroOrigen;
      const estadoNuevo = item.estadoNuevo ?? 'SIN_ESTADO';
      const matchesEstado = filtroEstado === 'TODOS' || estadoNuevo === filtroEstado;
      const matchesBusqueda =
        busqueda.length === 0 ||
        [
          item.documentoUuid,
          item.tipoDocumento,
          item.numeroDocumento,
          item.externalId,
          item.estadoAnterior,
          item.estadoNuevo,
          item.descripcion,
          item.origen,
          item.createdAt
        ]
          .filter((value): value is string => !!value)
          .some((value) => value.toLowerCase().includes(busqueda));

      return matchesTipo && matchesOrigen && matchesEstado && matchesBusqueda;
    });
  });
  protected readonly pagedItems = computed(() => {
    const items = this.filteredItems();
    const start = this.pageSignal() * this.sizeSignal();
    return items.slice(start, start + this.sizeSignal());
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.loading = true;
    this.error = '';

    this.auditoriaService
      .obtenerAuditoriaReciente()
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar la auditoria reciente de documentos.';
          this.data = null;
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

        this.data = response;
        this.pageSignal.set(0);
      });
  }

  protected formatValue(value: string | null | undefined, fallback = 'No registrado'): string {
    return value && value.trim().length > 0 ? value : fallback;
  }

  protected formatLabel(value: string | null | undefined): string {
    return this.formatValue(value, 'Sin estado').toLowerCase().replaceAll('_', ' ');
  }

  protected onFiltroTipoChange(value: string): void {
    this.filtroTipoSignal.set(value);
    this.pageSignal.set(0);
  }

  protected onFiltroOrigenChange(value: string): void {
    this.filtroOrigenSignal.set(value);
    this.pageSignal.set(0);
  }

  protected onFiltroEstadoChange(value: string): void {
    this.filtroEstadoSignal.set(value);
    this.pageSignal.set(0);
  }

  protected onBusquedaChange(value: string): void {
    this.busquedaSignal.set(value);
    this.pageSignal.set(0);
  }

  protected limpiarFiltros(): void {
    this.filtroTipoSignal.set('TODOS');
    this.filtroOrigenSignal.set('TODOS');
    this.filtroEstadoSignal.set('TODOS');
    this.busquedaSignal.set('');
    this.pageSignal.set(0);
  }

  protected get filtroTipo(): string {
    return this.filtroTipoSignal();
  }

  protected get filtroOrigen(): string {
    return this.filtroOrigenSignal();
  }

  protected get filtroEstado(): string {
    return this.filtroEstadoSignal();
  }

  protected get busqueda(): string {
    return this.busquedaSignal();
  }

  protected get tiposDisponibles(): string[] {
    return this.buildUniqueOptions((this.data?.eventos ?? []).map((item) => item.tipoDocumento));
  }

  protected get origenesDisponibles(): string[] {
    return this.buildUniqueOptions((this.data?.eventos ?? []).map((item) => item.origen ?? 'SIN_ORIGEN'));
  }

  protected get estadosDisponibles(): string[] {
    return this.buildUniqueOptions((this.data?.eventos ?? []).map((item) => item.estadoNuevo ?? 'SIN_ESTADO'));
  }

  protected get totalItems(): number {
    return this.filteredItems().length;
  }

  protected get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.sizeSignal()));
  }

  protected get currentPage(): number {
    return this.pageSignal();
  }

  protected get pageSize(): number {
    return this.sizeSignal();
  }

  protected onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) {
      return;
    }

    this.pageSignal.set(page);
  }

  protected onSizeChange(event: Event): void {
    const nextSize = Number((event.target as HTMLSelectElement).value);
    if (Number.isNaN(nextSize) || nextSize === this.sizeSignal()) {
      return;
    }

    this.sizeSignal.set(nextSize);
    this.pageSignal.set(0);
  }

  protected visiblePages(): number[] {
    if (this.totalPages <= 1) {
      return [0];
    }

    const start = Math.max(0, this.currentPage - 1);
    const end = Math.min(this.totalPages - 1, start + 2);
    const adjustedStart = Math.max(0, end - 2);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }

  protected getRangeStart(): number {
    return this.totalItems === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  protected getRangeEnd(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalItems);
  }

  protected trackByEvento(_: number, item: DocumentoAuditoriaEventoResponse): number {
    return item.id;
  }

  private buildUniqueOptions(values: string[]): string[] {
    return [...new Set(values.filter((value) => value && value.trim().length > 0))].sort((a, b) => a.localeCompare(b));
  }
}
