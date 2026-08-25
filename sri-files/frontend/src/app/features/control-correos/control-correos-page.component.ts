import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { AppAlertService } from '../../core/services/app-alert.service';
import { MonitorService } from '../../core/services/monitor.service';
import { CorreoPendienteItem } from '../../models/monitor-queue.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-control-correos-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './control-correos-page.component.html',
  styleUrl: './control-correos-page.component.scss'
})
export class ControlCorreosPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly appAlertService = inject(AppAlertService);
  private readonly monitorService = inject(MonitorService);
  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(10);
  private readonly busquedaSignal = signal('');

  protected correos: CorreoPendienteItem[] = [];
  protected loading = false;
  protected error = '';
  protected readonly sizeOptions = [5, 10, 20];
  protected readonly pendientesCount = computed(() =>
    this.correos.filter((item) => item.estado === 'CORREO_PENDIENTE').length
  );
  protected readonly erroresCount = computed(() =>
    this.correos.filter((item) => item.estado === 'ERROR_CORREO').length
  );
  protected readonly intervencionCount = computed(() =>
    this.correos.filter((item) => item.requiereIntervencion).length
  );
  protected readonly filteredCorreos = computed(() => {
    const term = this.busquedaSignal().trim().toLowerCase();
    if (!term) {
      return this.correos;
    }

    return this.correos.filter((item) =>
      [
        item.numeroDocumento,
        item.razonSocial,
        item.tipoDocumento,
        item.destinatario,
        item.estado,
        item.fechaAutorizacion,
        item.fechaRecepcion
      ]
        .filter((value): value is string => !!value)
        .some((value) => value.toLowerCase().includes(term))
    );
  });
  protected readonly topEstado = computed(() => {
    const counts = new Map<string, number>();
    this.filteredCorreos().forEach((item) => {
      counts.set(item.estado, (counts.get(item.estado) ?? 0) + 1);
    });

    return [...counts.entries()].sort((a, b) => b[1] - a[1])[0] ?? null;
  });
  protected readonly pagedTimeline = computed(() => {
    const items = this.filteredCorreos();
    const start = this.pageSignal() * this.sizeSignal();
    return items.slice(start, start + this.sizeSignal());
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    this.cargar();
  }

  protected get totalItems(): number {
    return this.filteredCorreos().length;
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

  protected recargar(): void {
    this.cargar(true);
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

  protected onBusquedaChange(value: string): void {
    this.busquedaSignal.set(value);
    this.pageSignal.set(0);
  }

  protected clearBusqueda(): void {
    this.busquedaSignal.set('');
    this.pageSignal.set(0);
  }

  protected get busqueda(): string {
    return this.busquedaSignal();
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

  private cargar(showSuccessAlert = false): void {
    this.loading = true;
    this.error = '';

    this.monitorService
      .obtenerCorreosPendientes()
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar la bandeja real de correos desde el backend.';
          this.correos = [];
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        this.correos = response?.items ?? [];
        this.pageSignal.set(0);
        if (showSuccessAlert) {
          this.appAlertService.success('Bandeja actualizada.', 'Se recargo el seguimiento real de correos.');
        }
      });
  }
}
