import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { EstablecimientosService } from '../../core/services/establecimientos.service';
import { PuntosEmisionService } from '../../core/services/puntos-emision.service';
import { Empresa } from '../../models/empresa.model';
import { Establecimiento } from '../../models/establecimiento.model';
import { PuntoEmision, PuntoEmisionRequest } from '../../models/punto-emision.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-punto-emision-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './punto-emision-form-page.component.html',
  styleUrl: './punto-emision-form-page.component.scss'
})
export class PuntoEmisionFormPageComponent {
  private readonly empresasService = inject(EmpresasService);
  private readonly establecimientosService = inject(EstablecimientosService);
  private readonly puntosEmisionService = inject(PuntosEmisionService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alerts = inject(AppAlertService);
  private readonly companyContext = inject(CompanyContextService);

  protected empresas: Empresa[] = [];
  protected establecimientos: Establecimiento[] = [];
  protected saving = false;
  protected formError = '';
  protected editingId: string | null = null;

  protected readonly form = this.fb.group({
    empresaId: ['', [Validators.required]],
    establecimientoId: ['', [Validators.required]],
    codigo: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    nombre: ['']
  });

  constructor() {
    this.editingId = this.route.snapshot.paramMap.get('id');
    this.companyContext.cargarEmpresas();
    effect(() => {
      const empresaActivaId = this.companyContext.empresaActiva()?.id ?? '';
      const queryEmpresaId = this.route.snapshot.queryParamMap.get('empresaId') ?? '';
      const statePunto = history.state['punto'] as PuntoEmision | undefined;

      if (!empresaActivaId || queryEmpresaId || statePunto) {
        return;
      }

      if (!this.empresas.some((empresa) => empresa.id === empresaActivaId)) {
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

  protected volver(): void {
    this.router.navigate(['/catalogos'], { queryParams: { tab: 'puntos-emision' } });
  }

  protected onEmpresaChange(syncContext = true): void {
    const empresaId = this.form.controls['empresaId'].value ?? '';
    this.establecimientos = [];
    this.form.patchValue({ establecimientoId: '' });

    if (!empresaId) {
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
      this.alerts.warning('Formulario incompleto', 'Completa la informacion requerida del punto de emision.');
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
          this.alerts.error('Operacion fallida', 'No se pudo guardar el punto de emision.');
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

        this.alerts.success('Punto guardado', `El punto ${response.codigo} fue almacenado correctamente.`);
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

        const statePunto = history.state['punto'] as PuntoEmision | undefined;
        const queryEmpresaId = this.route.snapshot.queryParamMap.get('empresaId') ?? '';

        if (statePunto) {
          this.form.patchValue({
            empresaId: statePunto.empresaId,
            establecimientoId: statePunto.establecimientoId,
            codigo: statePunto.codigo,
            nombre: statePunto.nombre || ''
          });
          this.cargarEstablecimientos(statePunto.empresaId, statePunto.establecimientoId);
          return;
        }

        const activeEmpresaId = this.companyContext.empresaActiva()?.id ?? '';
        const empresaId =
          queryEmpresaId ||
          this.empresas.find((empresa) => empresa.id === activeEmpresaId)?.id ||
          this.empresas[0]?.id ||
          '';
        if (empresaId) {
          this.form.patchValue({ empresaId });
          const queryEstablecimientoId = this.route.snapshot.queryParamMap.get('establecimientoId') ?? '';
          if (queryEstablecimientoId) {
            this.cargarEstablecimientos(empresaId, queryEstablecimientoId);
          } else {
            this.onEmpresaChange(false);
          }
        }
      });
  }

  private cargarEstablecimientos(empresaId: string, selectedId = ''): void {
    this.establecimientosService
      .listarPorEmpresa(empresaId)
      .pipe(
        catchError(() => {
          this.alerts.error('Establecimientos no disponibles', 'No fue posible cargar los establecimientos.');
          return of([]);
        })
      )
      .subscribe((response) => {
        this.establecimientos = [...response].sort((a, b) => a.codigo.localeCompare(b.codigo));
        const establecimientoId = selectedId || this.establecimientos[0]?.id || '';
        this.form.patchValue({ establecimientoId });
      });
  }
}
