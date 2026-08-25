import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { CorreoConfiguracion, CorreoConfiguracionRequest, Empresa } from '../../models/empresa.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-configuracion-correo-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './configuracion-correo-page.component.html',
  styleUrl: './configuracion-correo-page.component.scss'
})
export class ConfiguracionCorreoPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly empresasService = inject(EmpresasService);
  private readonly fb = inject(UntypedFormBuilder);
  private readonly alerts = inject(AppAlertService);

  protected empresas: Empresa[] = [];
  protected empresaSeleccionada: Empresa | null = null;
  protected configuracion: CorreoConfiguracion | null = null;
  protected loadingEmpresas = false;
  protected loadingConfiguracion = false;
  protected saving = false;
  protected errorEmpresas = '';
  protected errorConfiguracion = '';

  protected readonly form = this.fb.group({
    remitente: this.fb.control('', [Validators.required, Validators.email]),
    nombreRemitente: this.fb.control('', [Validators.required]),
    enviarXml: this.fb.control(true, [Validators.required]),
    enviarRide: this.fb.control(true, [Validators.required]),
    plantillaAsunto: this.fb.control('')
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
      remitente: this.configuracion.remitente || '',
      nombreRemitente: this.configuracion.nombreRemitente || '',
      enviarXml: this.configuracion.enviarXml,
      enviarRide: this.configuracion.enviarRide,
      plantillaAsunto: this.configuracion.plantillaAsunto || ''
    });
  }

  protected guardar(): void {
    if (!this.empresaSeleccionada || this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CorreoConfiguracionRequest = {
      remitente: this.form.controls['remitente'].value ?? '',
      nombreRemitente: this.form.controls['nombreRemitente'].value ?? '',
      enviarXml: Boolean(this.form.controls['enviarXml'].value),
      enviarRide: Boolean(this.form.controls['enviarRide'].value),
      plantillaAsunto: this.form.controls['plantillaAsunto'].value ?? ''
    };

    this.saving = true;

    this.empresasService
      .actualizarConfiguracionCorreo(this.empresaSeleccionada.id, payload)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo guardar', 'El backend no pudo actualizar la configuracion de correo.');
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
        this.alerts.success('Configuracion actualizada', 'La configuracion de correo fue guardada correctamente.');
      });
  }

  private cargarEmpresas(): void {
    this.loadingEmpresas = true;
    this.errorEmpresas = '';

    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.errorEmpresas = 'No fue posible cargar las empresas para configurar correo.';
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
      .obtenerConfiguracionCorreo(empresaId)
      .pipe(
        catchError(() => {
          this.errorConfiguracion = 'No fue posible cargar la configuracion actual de la empresa seleccionada.';
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
