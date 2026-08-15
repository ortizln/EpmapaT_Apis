import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardSnapshot } from '../../models/dashboard.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly dashboardService = inject(DashboardService);

  protected snapshot: DashboardSnapshot | null = null;
  protected loading = false;
  protected error = '';

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      this.companyContext.empresaActiva()?.id;
      this.cargarResumen();
    });
  }

  protected cargarResumen(): void {
    this.loading = true;
    this.error = '';

    this.dashboardService
      .obtenerSnapshot(this.companyContext.empresaActiva()?.id)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar el resumen operativo desde el backend.';
          this.snapshot = null;
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        this.snapshot = response;
      });
  }

  protected calculateWidth(value: number, max: number): number {
    return (value / max) * 100;
  }

  protected formatLabel(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ');
  }

  protected formatDuration(value: number): string {
    if (!value) {
      return '0 ms';
    }

    if (value < 1000) {
      return `${value} ms`;
    }

    return `${(value / 1000).toFixed(1)} s`;
  }

  protected getMaxPorTipo(): number {
    return Math.max(...(this.snapshot?.porTipo.map((item) => item.cantidad) ?? []), 1);
  }

  protected getMaxPorEstado(): number {
    return Math.max(...(this.snapshot?.porEstado.map((item) => item.cantidad) ?? []), 1);
  }

  protected getMaxPorDia(): number {
    return Math.max(...(this.snapshot?.porDia.map((item) => item.cantidad) ?? []), 1);
  }

  protected getMaxErroresPorEtapa(): number {
    return Math.max(...(this.snapshot?.erroresPorEtapa.map((item) => item.cantidad) ?? []), 1);
  }

  protected getHeroTitle(): string {
    if (!this.snapshot) {
      return 'Resumen centralizado del flujo documental';
    }

    if (this.snapshot.resumen.errores > 0 || this.snapshot.resumen.noAutorizados > 0) {
      return 'La operacion requiere atencion sobre incidencias activas';
    }

    if (this.snapshot.resumen.procesando > 0) {
      return 'El flujo se mantiene estable con documentos todavia en proceso';
    }

    return 'La plataforma muestra una operacion documental estable';
  }

  protected getHeroDescription(): string {
    if (!this.snapshot) {
      return 'Aqui consolidamos el volumen, el estado y la distribucion de documentos procesados por el backend.';
    }

    return `${this.snapshot.resumen.total} documentos acumulados, ${this.snapshot.resumen.recibidos} recibidos hoy y ${this.snapshot.resumen.autorizados} ya autorizados o finalizados.`;
  }
}
