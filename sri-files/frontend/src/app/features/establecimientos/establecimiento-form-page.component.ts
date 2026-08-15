import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { EstablecimientosService } from '../../core/services/establecimientos.service';
import { Empresa } from '../../models/empresa.model';
import { Establecimiento, EstablecimientoRequest } from '../../models/establecimiento.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-establecimiento-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './establecimiento-form-page.component.html',
  styleUrl: './establecimiento-form-page.component.scss'
})
export class EstablecimientoFormPageComponent {
  private readonly empresasService = inject(EmpresasService);
  private readonly establecimientosService = inject(EstablecimientosService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alerts = inject(AppAlertService);
  private readonly companyContext = inject(CompanyContextService);

  protected empresas: Empresa[] = [];
  protected saving = false;
  protected formError = '';
  protected editingId: string | null = null;

  protected readonly form = this.fb.group({
    empresaId: ['', [Validators.required]],
    codigo: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    nombre: [''],
    direccion: ['']
  });

  constructor() {
    this.editingId = this.route.snapshot.paramMap.get('id');
    this.companyContext.cargarEmpresas();
    effect(() => {
      const empresaActivaId = this.companyContext.empresaActiva()?.id ?? '';
      const queryEmpresaId = this.route.snapshot.queryParamMap.get('empresaId') ?? '';
      const stateEstablecimiento = history.state['establecimiento'] as Establecimiento | undefined;

      if (!empresaActivaId || queryEmpresaId || stateEstablecimiento) {
        return;
      }

      if (!this.empresas.some((empresa) => empresa.id === empresaActivaId)) {
        return;
      }

      if (this.form.controls['empresaId'].value === empresaActivaId) {
        return;
      }

      this.form.patchValue({ empresaId: empresaActivaId });
    });
    this.cargarEmpresas();
  }

  protected volver(): void {
    this.router.navigate(['/catalogos'], { queryParams: { tab: 'establecimientos' } });
  }

  protected guardar(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Completa la informacion requerida del establecimiento.');
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
          this.alerts.error('Operacion fallida', 'No se pudo guardar el establecimiento.');
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

        this.alerts.success('Establecimiento guardado', `El establecimiento ${response.codigo} fue almacenado correctamente.`);
        this.volver();
      });
  }

  private cargarEmpresas(): void {
    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.alerts.error('Empresas no disponibles', 'No fue posible cargar las empresas para este formulario.');
          return of([]);
        })
      )
      .subscribe((response) => {
        this.empresas = [...response].sort((a, b) => a.razonSocial.localeCompare(b.razonSocial));

        const stateEstablecimiento = history.state['establecimiento'] as Establecimiento | undefined;
        const queryEmpresaId = this.route.snapshot.queryParamMap.get('empresaId') ?? '';

        if (stateEstablecimiento) {
          this.form.patchValue({
            empresaId: stateEstablecimiento.empresaId,
            codigo: stateEstablecimiento.codigo,
            nombre: stateEstablecimiento.nombre || '',
            direccion: stateEstablecimiento.direccion || ''
          });
          return;
        }

        if (queryEmpresaId) {
          this.form.patchValue({ empresaId: queryEmpresaId });
          return;
        }

        const activeEmpresaId = this.companyContext.empresaActiva()?.id ?? '';
        const empresaId = this.empresas.find((empresa) => empresa.id === activeEmpresaId)?.id ?? this.empresas[0]?.id ?? '';
        if (empresaId) {
          this.form.patchValue({ empresaId });
        }
      });
  }
}
