import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { catchError, finalize, forkJoin, interval, of, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MonitorService } from '../../core/services/monitor.service';
import { MonitorHealthResponse, MonitorResumen } from '../../models/monitor.model';
import { MonitorPendienteItem } from '../../models/monitor-queue.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-monitoreo-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './monitoreo-page.component.html',
  styleUrl: './monitoreo-page.component.scss'
})
export class MonitoreoPageComponent {
  private readonly monitorService = inject(MonitorService);
  private readonly destroyRef = inject(DestroyRef);

  protected health: MonitorHealthResponse | null = null;
  protected resumen: MonitorResumen | null = null;
  protected pendientes: MonitorPendienteItem[] = [];
  protected loading = false;
  protected error = '';
  protected readonly autoRefreshEnabled = signal(true);
  protected readonly refreshIntervalSeconds = 20;
  protected readonly componentesNoOk = computed(
    () => this.health?.componentes.filter((item) => !['OK', 'ACTIVO', 'UP'].includes((item.estado ?? '').toUpperCase())) ?? []
  );
  protected readonly pendientesConAlerta = computed(
    () => this.pendientes.filter((item) => (item.estado ?? '').toUpperCase().includes('ERROR') || item.intentos > 1)
  );
  protected readonly topEstadoPendiente = computed(() => {
    const counts = new Map<string, number>();
    this.pendientes.forEach((item) => {
      const key = item.estado ?? 'DESCONOCIDO';
      counts.set(key, (counts.get(key) ?? 0) + 1);
    });

    return [...counts.entries()].sort((a, b) => b[1] - a[1])[0] ?? null;
  });

  constructor() {
    this.cargar();
    this.iniciarAutoRefresh();
  }

  protected cargar(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      health: this.monitorService.obtenerHealth(),
      pendientes: this.monitorService.obtenerPendientes()
    })
      .pipe(
        catchError(() => {
          this.error = 'No fue posible consultar el estado operativo del backend.';
          this.health = null;
          this.resumen = null;
          this.pendientes = [];
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

        this.health = response.health;
        this.resumen = response.health.resumen;
        this.pendientes = response.pendientes.items;
      });
  }

  protected toggleAutoRefresh(): void {
    this.autoRefreshEnabled.update((value) => !value);
  }

  protected formatEstado(value: string | null | undefined): string {
    return (value ?? 'DESCONOCIDO').toLowerCase().replaceAll('_', ' ');
  }

  protected getEstadoClass(value: string | null | undefined): string {
    const estado = (value ?? '').toUpperCase();
    if (estado === 'OK' || estado === 'ACTIVO' || estado === 'UP') {
      return 'status-pill status-pill--success';
    }
    if (estado === 'WARN' || estado === 'WARNING' || estado === 'DEGRADADO') {
      return 'status-pill status-pill--warning';
    }
    return 'status-pill status-pill--inactive';
  }

  private iniciarAutoRefresh(): void {
    interval(this.refreshIntervalSeconds * 1000)
      .pipe(
        startWith(0),
        switchMap(() => {
          if (!this.autoRefreshEnabled()) {
            return of(null);
          }

          this.loading = true;
          this.error = '';

          return forkJoin({
            health: this.monitorService.obtenerHealth(),
            pendientes: this.monitorService.obtenerPendientes()
          }).pipe(
            catchError(() => {
              this.error = 'No fue posible consultar el estado operativo del backend.';
              this.health = null;
              this.resumen = null;
              this.pendientes = [];
              return of(null);
            }),
            finalize(() => {
              this.loading = false;
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.health = response.health;
        this.resumen = response.health.resumen;
        this.pendientes = response.pendientes.items;
      });
  }
}
