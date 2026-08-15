import { CommonModule } from '@angular/common';
import { Component, effect, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { EstablecimientosService } from '../../core/services/establecimientos.service';
import { Empresa } from '../../models/empresa.model';
import { Establecimiento, EstablecimientoRequest } from '../../models/establecimiento.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-establecimientos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './establecimientos-page.component.html',
  styleUrl: './establecimientos-page.component.scss'
})
export class EstablecimientosPageComponent {
  readonly embedded = input(false);
  private readonly empresasService = inject(EmpresasService);
  private readonly establecimientosService = inject(EstablecimientosService);
  private readonly companyContext = inject(CompanyContextService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected empresas: Empresa[] = [];
  protected establecimientos: Establecimiento[] = [];
  protected loadingEmpresas = false;
  protected loading = false;
  protected saving = false;
  protected error = '';
  protected formError = '';
  protected editingId: string | null = null;
  protected savingStateId: string | null = null;

  protected readonly form = this.fb.group({
    empresaId: ['', [Validators.required]],
    codigo: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    nombre: [''],
    direccion: ['']
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

  protected irANuevoEstablecimiento(): void {
    const empresaId = this.form.controls['empresaId'].value ?? '';
    this.router.navigate(['/catalogos/establecimientos/nuevo'], {
      queryParams: empresaId ? { empresaId } : undefined
    });
  }

  protected irAEditarEstablecimiento(establecimiento: Establecimiento): void {
    this.router.navigate([`/catalogos/establecimientos/${establecimiento.id}/editar`], {
      state: { establecimiento }
    });
  }

  protected onEmpresaChange(syncContext = true): void {
    this.editingId = null;
    this.formError = '';
    const empresaId = this.form.controls['empresaId'].value ?? '';
    if (!empresaId) {
      this.establecimientos = [];
      return;
    }

    if (syncContext) {
      this.companyContext.seleccionarEmpresa(empresaId);
    }

    this.cargarEstablecimientos(empresaId);
  }

  protected guardar(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const empresaId = this.form.controls['empresaId'].value ?? '';
    if (!empresaId) {
      this.formError = 'Debes seleccionar una empresa antes de guardar.';
      return;
    }

    this.saving = true;
    this.formError = '';

    const payload: EstablecimientoRequest = {
      codigo: this.form.controls['codigo'].value ?? '',
      nombre: this.form.controls['nombre'].value ?? '',
      direccion: this.form.controls['direccion'].value ?? ''
    };

    const request$ = this.editingId
      ? this.establecimientosService.actualizar(this.editingId, payload)
      : this.establecimientosService.crear(empresaId, payload);

    request$
      .pipe(
        catchError(() => {
          this.formError = 'No fue posible guardar el establecimiento.';
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
          this.establecimientos = this.establecimientos.map((item) => (item.id === response.id ? response : item));
        } else {
          this.establecimientos = [...this.establecimientos, response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        }

        const selectedEmpresaId = this.form.controls['empresaId'].value ?? '';
        this.resetForm(false);
        this.form.patchValue({ empresaId: selectedEmpresaId });
      });
  }

  protected editar(establecimiento: Establecimiento): void {
    this.editingId = establecimiento.id;
    this.formError = '';
    this.form.patchValue({
      empresaId: establecimiento.empresaId,
      codigo: establecimiento.codigo,
      nombre: establecimiento.nombre || '',
      direccion: establecimiento.direccion || ''
    });
  }

  protected cambiarEstado(establecimiento: Establecimiento): void {
    this.savingStateId = establecimiento.id;
    this.error = '';

    this.establecimientosService
      .actualizarEstado(establecimiento.id, !establecimiento.activo)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible actualizar el estado del establecimiento.';
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

        this.establecimientos = this.establecimientos.map((item) => (item.id === response.id ? response : item));
      });
  }

  protected resetForm(clearEmpresa = true): void {
    const empresaId = clearEmpresa ? '' : this.form.controls['empresaId'].value ?? '';
    this.editingId = null;
    this.formError = '';
    this.form.reset({
      empresaId,
      codigo: '',
      nombre: '',
      direccion: ''
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
          const initialEmpresaId =
            this.empresas.find((empresa) => empresa.id === activeEmpresaId)?.id ?? this.empresas[0].id;
          this.form.patchValue({ empresaId: initialEmpresaId });
          this.onEmpresaChange(false);
        }
      });
  }

  private cargarEstablecimientos(empresaId: string): void {
    this.loading = true;
    this.error = '';

    this.establecimientosService
      .listarPorEmpresa(empresaId)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los establecimientos de la empresa seleccionada.';
          this.establecimientos = [];
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        this.establecimientos = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
      });
  }
}
