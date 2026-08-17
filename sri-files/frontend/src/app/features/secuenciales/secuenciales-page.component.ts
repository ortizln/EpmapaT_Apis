import { CommonModule } from '@angular/common';
import { Component, effect, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { EstablecimientosService } from '../../core/services/establecimientos.service';
import { PuntosEmisionService } from '../../core/services/puntos-emision.service';
import { SecuencialesService } from '../../core/services/secuenciales.service';
import { Empresa } from '../../models/empresa.model';
import { Establecimiento } from '../../models/establecimiento.model';
import { PuntoEmision } from '../../models/punto-emision.model';
import { Secuencial } from '../../models/secuencial.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-secuenciales-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './secuenciales-page.component.html',
  styleUrl: './secuenciales-page.component.scss'
})
export class SecuencialesPageComponent {
  readonly embedded = input(false);
  private readonly empresasService = inject(EmpresasService);
  private readonly establecimientosService = inject(EstablecimientosService);
  private readonly puntosEmisionService = inject(PuntosEmisionService);
  private readonly secuencialesService = inject(SecuencialesService);
  private readonly companyContext = inject(CompanyContextService);
  private readonly router = inject(Router);

  protected empresas: Empresa[] = [];
  protected establecimientos: Establecimiento[] = [];
  protected puntosEmision: PuntoEmision[] = [];
  protected secuenciales: Secuencial[] = [];
  protected empresaId = '';
  protected establecimientoId = '';
  protected puntoEmisionId = '';
  protected loading = false;
  protected error = '';
  protected savingTipo = '';
  protected draftValues: Record<string, number> = {};
  protected draftActive: Record<string, boolean> = {};

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      const empresaActivaId = this.companyContext.empresaActiva()?.id ?? '';
      if (!empresaActivaId || !this.empresas.some((empresa) => empresa.id === empresaActivaId)) {
        return;
      }

      if (this.empresaId === empresaActivaId) {
        return;
      }

      this.seleccionarEmpresa(empresaActivaId, false);
    });
    this.cargarEmpresas();
  }

  protected irAAdministrarSecuenciales(): void {
    if (!this.puntoEmisionId) {
      return;
    }

    this.router.navigate(['/catalogos/secuenciales/editar'], {
      queryParams: {
        empresaId: this.empresaId,
        establecimientoId: this.establecimientoId,
        puntoEmisionId: this.puntoEmisionId
      }
    });
  }

  protected onEmpresaChange(event: Event): void {
    this.seleccionarEmpresa((event.target as HTMLSelectElement).value);
  }

  protected onEstablecimientoChange(event: Event): void {
    this.establecimientoId = (event.target as HTMLSelectElement).value;
    this.puntoEmisionId = '';
    this.puntosEmision = [];
    this.secuenciales = [];
    this.draftValues = {};
    this.draftActive = {};

    if (!this.establecimientoId) {
      return;
    }

    this.cargarPuntosEmision(this.establecimientoId);
  }

  protected onPuntoEmisionChange(event: Event): void {
    this.puntoEmisionId = (event.target as HTMLSelectElement).value;
    this.secuenciales = [];
    this.draftValues = {};
    this.draftActive = {};

    if (!this.puntoEmisionId) {
      return;
    }

    this.cargarSecuenciales(this.puntoEmisionId);
  }

  protected onValorChange(tipoDocumento: string, event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.draftValues[tipoDocumento] = Number.isNaN(value) ? 0 : value;
  }

  protected onActivoChange(tipoDocumento: string, event: Event): void {
    this.draftActive[tipoDocumento] = (event.target as HTMLInputElement).checked;
  }

  protected guardar(secuencial: Secuencial): void {
    if (!this.puntoEmisionId) {
      return;
    }

    this.savingTipo = secuencial.tipoDocumento;
    this.error = '';

    this.secuencialesService
      .actualizar(this.puntoEmisionId, secuencial.tipoDocumento, {
        valorActual: this.draftValues[secuencial.tipoDocumento] ?? secuencial.valorActual,
        activo: this.draftActive[secuencial.tipoDocumento] ?? secuencial.activo
      })
      .pipe(
        catchError(() => {
          this.error = `No fue posible guardar el secuencial de ${this.formatLabel(secuencial.tipoDocumento)}.`;
          return of(null);
        }),
        finalize(() => {
          this.savingTipo = '';
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.secuenciales = this.secuenciales.map((item) =>
          item.tipoDocumento === response.tipoDocumento ? response : item
        );
        this.draftValues[response.tipoDocumento] = response.valorActual;
        this.draftActive[response.tipoDocumento] = response.activo;
      });
  }

  protected formatLabel(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ');
  }

  protected getDraftValor(secuencial: Secuencial): number {
    return this.draftValues[secuencial.tipoDocumento] ?? secuencial.valorActual;
  }

  protected getDraftActivo(secuencial: Secuencial): boolean {
    return this.draftActive[secuencial.tipoDocumento] ?? secuencial.activo;
  }

  private seleccionarEmpresa(empresaId: string, syncContext = true): void {
    this.empresaId = empresaId;
    this.establecimientoId = '';
    this.puntoEmisionId = '';
    this.establecimientos = [];
    this.puntosEmision = [];
    this.secuenciales = [];
    this.draftValues = {};
    this.draftActive = {};

    if (!this.empresaId) {
      return;
    }

    if (syncContext) {
      this.companyContext.seleccionarEmpresa(this.empresaId);
    }

    this.cargarEstablecimientos(this.empresaId);
  }

  private cargarEmpresas(): void {
    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar las empresas disponibles.';
          return of([]);
        })
      )
      .subscribe((response) => {
        this.empresas = [...response].sort((a, b) => a.razonSocial.localeCompare(b.razonSocial));
        if (this.empresas.length) {
          const activeEmpresaId = this.companyContext.empresaActiva()?.id ?? '';
          const empresaId = this.empresas.find((empresa) => empresa.id === activeEmpresaId)?.id ?? this.empresas[0].id;
          this.seleccionarEmpresa(empresaId, false);
        }
      });
  }

  private cargarEstablecimientos(empresaId: string): void {
    this.establecimientosService
      .listarPorEmpresa(empresaId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los establecimientos.';
          return of([]);
        })
      )
      .subscribe((response) => {
        this.establecimientos = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        if (this.establecimientos.length) {
          this.establecimientoId = this.establecimientos[0].id;
          this.cargarPuntosEmision(this.establecimientoId);
        }
      });
  }

  private cargarPuntosEmision(establecimientoId: string): void {
    this.puntosEmisionService
      .listarPorEstablecimiento(establecimientoId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los puntos de emision.';
          return of([]);
        })
      )
      .subscribe((response) => {
        this.puntosEmision = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        if (this.puntosEmision.length) {
          this.puntoEmisionId = this.puntosEmision[0].id;
          this.cargarSecuenciales(this.puntoEmisionId);
        }
      });
  }

  private cargarSecuenciales(puntoEmisionId: string): void {
    this.loading = true;
    this.error = '';

    this.secuencialesService
      .listarPorPuntoEmision(puntoEmisionId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los secuenciales del punto de emision.';
          this.secuenciales = [];
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        this.secuenciales = response;
        this.draftValues = Object.fromEntries(response.map((item) => [item.tipoDocumento, item.valorActual]));
        this.draftActive = Object.fromEntries(response.map((item) => [item.tipoDocumento, item.activo]));
      });
  }
}
