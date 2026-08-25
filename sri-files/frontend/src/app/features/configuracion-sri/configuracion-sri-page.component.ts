import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { Empresa, SriConfiguracion, SriConfiguracionRequest } from '../../models/empresa.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-configuracion-sri-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './configuracion-sri-page.component.html',
  styleUrl: './configuracion-sri-page.component.scss'
})
export class ConfiguracionSriPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly empresasService = inject(EmpresasService);
  private readonly fb = inject(UntypedFormBuilder);
  private readonly alerts = inject(AppAlertService);

  protected empresas: Empresa[] = [];
  protected empresaSeleccionada: Empresa | null = null;
  protected configuracion: SriConfiguracion | null = null;
  protected loadingEmpresas = false;
  protected loadingConfiguracion = false;
  protected saving = false;
  protected errorEmpresas = '';
  protected errorConfiguracion = '';

  protected readonly form = this.fb.group({
    ambiente: this.fb.control(1, [Validators.required]),
    timeoutConexionMs: this.fb.control(10000, [Validators.required, Validators.min(1000)]),
    timeoutRespuestaMs: this.fb.control(15000, [Validators.required, Validators.min(1000)]),
    maxReintentos: this.fb.control(3, [Validators.required, Validators.min(0)]),
    activo: this.fb.control(true, [Validators.required])
  });

  constructor() {
    effect(() => {
      const empresaActivaId = this.companyContext.empresaActiva()?.id;
      if (!empresaActivaId || !this.empresas.length) {
        return;
      }

      const empresa = this.empresas.find((item) => item.id === empresaActivaId);
      if (empresa && this.empresaSeleccionada?.id !== empresa.id) {
        this.seleccionarEmpresa(empresa);
      }
    });

    this.cargarEmpresas();
  }

  protected seleccionarEmpresa(empresa: Empresa): void {
    this.companyContext.seleccionarEmpresa(empresa.id);
    this.empresaSeleccionada = empresa;
    this.cargarConfiguracion(empresa.id);
  }

  protected restaurar(): void {
    if (!this.configuracion) {
      return;
    }

    this.form.reset({
      ambiente: this.configuracion.ambiente,
      timeoutConexionMs: this.configuracion.timeoutConexionMs,
      timeoutRespuestaMs: this.configuracion.timeoutRespuestaMs,
      maxReintentos: this.configuracion.maxReintentos,
      activo: this.configuracion.activo
    });
  }

  protected guardar(): void {
    if (!this.empresaSeleccionada || this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: SriConfiguracionRequest = {
      ambiente: Number(this.form.controls['ambiente'].value ?? 1),
      timeoutConexionMs: Number(this.form.controls['timeoutConexionMs'].value ?? 10000),
      timeoutRespuestaMs: Number(this.form.controls['timeoutRespuestaMs'].value ?? 15000),
      maxReintentos: Number(this.form.controls['maxReintentos'].value ?? 3),
      activo: Boolean(this.form.controls['activo'].value)
    };

    if (payload.ambiente === 2 && this.configuracion?.ambiente !== 2) {
      const confirmed = window.confirm('Vas a cambiar el ambiente SRI a PRODUCCION. Confirma para continuar.');
      if (!confirmed) {
        return;
      }
    }

    this.saving = true;

    this.empresasService
      .actualizarConfiguracionSri(this.empresaSeleccionada.id, payload)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo guardar', 'El backend no pudo actualizar la configuracion SRI.');
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((configuracion) => {
        if (!configuracion) {
          return;
        }

        this.configuracion = configuracion;
        this.restaurar();
        this.alerts.success('Configuracion actualizada', 'La configuracion SRI fue guardada correctamente.');
      });
  }

  private cargarEmpresas(): void {
    this.loadingEmpresas = true;
    this.errorEmpresas = '';

    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.errorEmpresas = 'No fue posible cargar las empresas para configurar SRI.';
          this.empresas = [];
          this.empresaSeleccionada = null;
          return of([]);
        }),
        finalize(() => {
          this.loadingEmpresas = false;
        })
      )
      .subscribe((empresas) => {
        this.empresas = empresas;
        const primeraEmpresa = empresas.find((item) => item.id === this.companyContext.empresaActiva()?.id) ?? empresas[0] ?? null;
        this.empresaSeleccionada = primeraEmpresa;

        if (primeraEmpresa) {
          this.cargarConfiguracion(primeraEmpresa.id);
        }
      });
  }

  private cargarConfiguracion(empresaId: string): void {
    this.loadingConfiguracion = true;
    this.errorConfiguracion = '';
    this.configuracion = null;

    this.empresasService
      .obtenerConfiguracionSri(empresaId)
      .pipe(
        catchError(() => {
          this.errorConfiguracion = 'No fue posible cargar la configuracion SRI actual de la empresa seleccionada.';
          return of(null);
        }),
        finalize(() => {
          this.loadingConfiguracion = false;
        })
      )
      .subscribe((configuracion) => {
        if (!configuracion) {
          return;
        }

        this.configuracion = configuracion;
        this.restaurar();
      });
  }
}
