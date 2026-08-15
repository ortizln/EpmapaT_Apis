import { CommonModule } from '@angular/common';
import { Component, effect, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { EstablecimientosService } from '../../core/services/establecimientos.service';
import { PuntosEmisionService } from '../../core/services/puntos-emision.service';
import { Empresa } from '../../models/empresa.model';
import { Establecimiento } from '../../models/establecimiento.model';
import { PuntoEmision, PuntoEmisionRequest } from '../../models/punto-emision.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-puntos-emision-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './puntos-emision-page.component.html',
  styleUrl: './puntos-emision-page.component.scss'
})
export class PuntosEmisionPageComponent {
  readonly embedded = input(false);
  private readonly empresasService = inject(EmpresasService);
  private readonly establecimientosService = inject(EstablecimientosService);
  private readonly puntosEmisionService = inject(PuntosEmisionService);
  private readonly companyContext = inject(CompanyContextService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected empresas: Empresa[] = [];
  protected establecimientos: Establecimiento[] = [];
  protected puntos: PuntoEmision[] = [];
  protected loadingEmpresas = false;
  protected loadingEstablecimientos = false;
  protected loading = false;
  protected saving = false;
  protected error = '';
  protected formError = '';
  protected editingId: string | null = null;
  protected savingStateId: string | null = null;

  protected readonly form = this.fb.group({
    empresaId: ['', [Validators.required]],
    establecimientoId: ['', [Validators.required]],
    codigo: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    nombre: ['']
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      const empresaActivaId = this.companyContext.empresaActiva()?.id ?? '';
      if (!empresaActivaId || !this.empresas.some((empresa) => empresa.id === empresaActivaId)) {
        return;
      }

      if (this.form.controls['empresaId'].value === empresaActivaId) {
        return;
      }

      this.form.patchValue({ empresaId: empresaActivaId });
      this.onEmpresaChange(false);
    });
    this.cargarEmpresas();
  }

  protected irANuevoPunto(): void {
    const empresaId = this.form.controls['empresaId'].value ?? '';
    const establecimientoId = this.form.controls['establecimientoId'].value ?? '';
    this.router.navigate(['/catalogos/puntos-emision/nuevo'], {
      queryParams: {
        ...(empresaId ? { empresaId } : {}),
        ...(establecimientoId ? { establecimientoId } : {})
      }
    });
  }

  protected irAEditarPunto(punto: PuntoEmision): void {
    this.router.navigate([`/catalogos/puntos-emision/${punto.id}/editar`], {
      state: { punto }
    });
  }

  protected onEmpresaChange(syncContext = true): void {
    const empresaId = this.form.controls['empresaId'].value ?? '';
    this.establecimientos = [];
    this.puntos = [];
    this.form.patchValue({ establecimientoId: '' });
    this.editingId = null;

    if (!empresaId) {
      return;
    }

    if (syncContext) {
      this.companyContext.seleccionarEmpresa(empresaId);
    }

    this.cargarEstablecimientos(empresaId);
  }

  protected onEstablecimientoChange(): void {
    const establecimientoId = this.form.controls['establecimientoId'].value ?? '';
    this.puntos = [];
    this.editingId = null;
    this.formError = '';

    if (!establecimientoId) {
      return;
    }

    this.cargarPuntos(establecimientoId);
  }

  protected guardar(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const establecimientoId = this.form.controls['establecimientoId'].value ?? '';
    if (!establecimientoId) {
      this.formError = 'Debes seleccionar un establecimiento antes de guardar.';
      return;
    }

    this.saving = true;
    this.formError = '';

    const payload: PuntoEmisionRequest = {
      codigo: this.form.controls['codigo'].value ?? '',
      nombre: this.form.controls['nombre'].value ?? ''
    };

    const request$ = this.editingId
      ? this.puntosEmisionService.actualizar(this.editingId, payload)
      : this.puntosEmisionService.crear(establecimientoId, payload);

    request$
      .pipe(
        catchError(() => {
          this.formError = 'No fue posible guardar el punto de emision.';
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        if (this.editingId) {
          this.puntos = this.puntos.map((item) => (item.id === response.id ? response : item));
        } else {
          this.puntos = [...this.puntos, response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        }

        const empresaId = this.form.controls['empresaId'].value ?? '';
        const establecimientoIdActual = this.form.controls['establecimientoId'].value ?? '';
        this.resetForm(false);
        this.form.patchValue({ empresaId, establecimientoId: establecimientoIdActual });
      });
  }

  protected editar(punto: PuntoEmision): void {
    this.editingId = punto.id;
    this.formError = '';
    this.form.patchValue({
      empresaId: punto.empresaId,
      establecimientoId: punto.establecimientoId,
      codigo: punto.codigo,
      nombre: punto.nombre || ''
    });
  }

  protected cambiarEstado(punto: PuntoEmision): void {
    this.savingStateId = punto.id;
    this.error = '';

    this.puntosEmisionService
      .actualizarEstado(punto.id, !punto.activo)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible actualizar el estado del punto de emision.';
          return of(null);
        }),
        finalize(() => {
          this.savingStateId = null;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.puntos = this.puntos.map((item) => (item.id === response.id ? response : item));
      });
  }

  protected resetForm(clearSelection = true): void {
    const empresaId = clearSelection ? '' : this.form.controls['empresaId'].value ?? '';
    const establecimientoId = clearSelection ? '' : this.form.controls['establecimientoId'].value ?? '';
    this.editingId = null;
    this.formError = '';
    this.form.reset({
      empresaId,
      establecimientoId,
      codigo: '',
      nombre: ''
    });
  }

  private cargarEmpresas(): void {
    this.loadingEmpresas = true;
    this.error = '';

    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar las empresas disponibles.';
          return of([]);
        }),
        finalize(() => {
          this.loadingEmpresas = false;
        })
      )
      .subscribe((response) => {
        this.empresas = [...response].sort((a, b) => a.razonSocial.localeCompare(b.razonSocial));
        if (this.empresas.length) {
          const activeEmpresaId = this.companyContext.empresaActiva()?.id ?? '';
          const empresaId = this.empresas.find((empresa) => empresa.id === activeEmpresaId)?.id ?? this.empresas[0].id;
          this.form.patchValue({ empresaId });
          this.onEmpresaChange(false);
        }
      });
  }

  private cargarEstablecimientos(empresaId: string): void {
    this.loadingEstablecimientos = true;
    this.error = '';

    this.establecimientosService
      .listarPorEmpresa(empresaId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los establecimientos disponibles.';
          this.establecimientos = [];
          return of([]);
        }),
        finalize(() => {
          this.loadingEstablecimientos = false;
        })
      )
      .subscribe((response) => {
        this.establecimientos = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        if (this.establecimientos.length) {
          const establecimientoId = this.establecimientos[0].id;
          this.form.patchValue({ establecimientoId });
          this.cargarPuntos(establecimientoId);
        } else {
          this.puntos = [];
        }
      });
  }

  private cargarPuntos(establecimientoId: string): void {
    this.loading = true;
    this.error = '';

    this.puntosEmisionService
      .listarPorEstablecimiento(establecimientoId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los puntos de emision del establecimiento.';
          this.puntos = [];
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        this.puntos = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
      });
  }
}
