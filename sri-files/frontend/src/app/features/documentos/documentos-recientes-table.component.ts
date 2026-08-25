import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { DocumentoResumen, TipoDocumento } from '../../models/documento.model';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-documentos-recientes-table',
  standalone: true,
  imports: [CommonModule, StatusChipComponent, HasPermissionDirective],
  templateUrl: './documentos-recientes-table.component.html',
  styleUrl: './documentos-recientes-table.component.scss'
})
export class DocumentosRecientesTableComponent {
  readonly documentos = input<DocumentoResumen[]>([]);
  readonly tiposDocumento = input<TipoDocumento[]>([]);
  readonly tipoDocumento = input('');
  readonly estado = input('');
  readonly busqueda = input('');
  readonly page = input(0);
  readonly size = input(10);
  readonly totalPages = input(0);
  readonly totalItems = input(0);
  readonly loading = input(false);
  readonly error = input('');

  readonly tipoDocumentoChange = output<string>();
  readonly estadoChange = output<string>();
  readonly busquedaChange = output<string>();
  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();
  readonly verDetalle = output<string>();
  readonly exportar = output<void>();
  readonly cargarXmlRapido = output<{ id: string; file: File }>();

  protected readonly sizeOptions = [10, 20, 50];

  protected onTipoDocumentoChange(event: Event): void {
    this.tipoDocumentoChange.emit((event.target as HTMLSelectElement | null)?.value ?? '');
  }

  protected onEstadoInput(event: Event): void {
    this.estadoChange.emit((event.target as HTMLInputElement | null)?.value ?? '');
  }

  protected onBusquedaInput(event: Event): void {
    this.busquedaChange.emit((event.target as HTMLInputElement | null)?.value ?? '');
  }

  protected onSizeChange(event: Event): void {
    const nextSize = Number((event.target as HTMLSelectElement | null)?.value ?? this.size());
    this.sizeChange.emit(Number.isNaN(nextSize) ? this.size() : nextSize);
  }

  protected currentPageLabel(): number {
    return this.totalItems() === 0 ? 0 : this.page() + 1;
  }

  protected rangeStart(): number {
    return this.totalItems() === 0 ? 0 : this.page() * this.size() + 1;
  }

  protected rangeEnd(): number {
    return Math.min((this.page() + 1) * this.size(), this.totalItems());
  }

  protected visiblePages(): number[] {
    const total = this.totalPages();
    if (total <= 1) {
      return [0];
    }

    const current = this.page();
    const start = Math.max(0, current - 1);
    const end = Math.min(total - 1, start + 2);
    const adjustedStart = Math.max(0, end - 2);

    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }

  protected onExportar(): void {
    this.exportar.emit();
  }

  protected seleccionarXmlRapido(documentoId: string, event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    if (!file) {
      return;
    }

    this.cargarXmlRapido.emit({ id: documentoId, file });
    if (input) {
      input.value = '';
    }
  }
}
