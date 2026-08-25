import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardSnapshot } from '../../models/dashboard.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-control-errores-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './control-errores-page.component.html',
  styleUrl: './control-errores-page.component.scss'
})
export class ControlErroresPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly dashboardService = inject(DashboardService);
  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(10);
  private readonly busquedaSignal = signal('');

  protected snapshot: DashboardSnapshot | null = null;
  protected loading = false;
  protected error = '';
  protected readonly sizeOptions = [5, 10, 20];
  protected readonly filteredErrores = computed(() => {
    const term = this.busquedaSignal().trim().toLowerCase();
    const items = this.snapshot?.erroresPorEtapa ?? [];
    if (!term) {
      return items;
    }

    return items.filter((item) =>
      [item.etapa, String(item.cantidad)].some((value) => value.toLowerCase().includes(term))
    );
  });
  protected readonly pagedErrores = computed(() => {
    const items = this.filteredErrores();
    const start = this.pageSignal() * this.sizeSignal();
    return items.slice(start, start + this.sizeSignal());
  });
  protected readonly errorStagesWithData = computed(() => (this.snapshot?.erroresPorEtapa ?? []).filter((item) => item.cantidad > 0).length);
  protected readonly mostCriticalStage = computed(() => {
    const items = this.snapshot?.erroresPorEtapa ?? [];
    if (!items.length) {
      return null;
    }

    return [...items].sort((a, b) => b.cantidad - a.cantidad)[0] ?? null;
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    this.cargar();
  }

  protected calculateWidth(value: number, max: number): number {
    return (value / max) * 100;
  }

  protected formatLabel(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ');
  }

  protected getMaxErroresPorEtapa(): number {
    return Math.max(...(this.snapshot?.erroresPorEtapa.map((item) => item.cantidad) ?? []), 1);
  }

  protected get totalItems(): number {
    return this.filteredErrores().length;
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

  private cargar(): void {
    this.loading = true;
    this.error = '';

    this.dashboardService
      .obtenerSnapshot(this.companyContext.empresaActiva()?.id)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar las metricas reales de errores desde el backend.';
          this.snapshot = null;
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((snapshot) => {
        this.snapshot = snapshot;
        this.pageSignal.set(0);
      });
  }
}
