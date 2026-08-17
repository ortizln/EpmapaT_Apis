import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
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
  private readonly monitorService = inject(MonitorService);
  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(10);

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
  protected readonly pagedTimeline = computed(() => {
    const items = this.correos ?? [];
    const start = this.pageSignal() * this.sizeSignal();
    return items.slice(start, start + this.sizeSignal());
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    this.cargar();
  }

  protected get totalItems(): number {
    return this.correos.length;
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

  private cargar(): void {
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
      });
  }
}
