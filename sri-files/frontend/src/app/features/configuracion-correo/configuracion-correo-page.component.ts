import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { Empresa, EmpresaConfiguracion, EmpresaConfiguracionRequest } from '../../models/empresa.model';
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
  protected configuracion: EmpresaConfiguracion | null = null;
  protected loadingEmpresas = false;
  protected loadingConfiguracion = false;
  protected saving = false;
  protected errorEmpresas = '';
  protected errorConfiguracion = '';
  protected certificadoArchivoBase64: string | null = null;
  protected certificadoArchivoNombre = '';

  protected readonly form = this.fb.group({
    ambienteSri: this.fb.control(1, [Validators.required]),
    correoNotificaciones: this.fb.control('', [Validators.required, Validators.email]),
    correoRespuesta: this.fb.control('', [Validators.required, Validators.email]),
    certificadoNombre: this.fb.control(''),
    certificadoClave: this.fb.control('')
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
      ambienteSri: this.configuracion.ambienteSri,
      correoNotificaciones: this.configuracion.correoNotificaciones || '',
      correoRespuesta: this.configuracion.correoRespuesta || '',
      certificadoNombre: this.configuracion.certificadoNombre || '',
      certificadoClave: ''
    });
    this.certificadoArchivoBase64 = null;
    this.certificadoArchivoNombre = '';
  }

  protected guardar(): void {
    if (!this.empresaSeleccionada || this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: EmpresaConfiguracionRequest = {
      ambienteSri: Number(this.form.controls['ambienteSri'].value ?? 1),
      correoNotificaciones: this.form.controls['correoNotificaciones'].value ?? '',
      correoRespuesta: this.form.controls['correoRespuesta'].value ?? '',
      certificadoNombre: this.form.controls['certificadoNombre'].value ?? '',
      certificadoBase64: this.certificadoArchivoBase64,
      certificadoClave: this.form.controls['certificadoClave'].value || null,
      limpiarCertificado: false
    };

    this.saving = true;

    this.empresasService
      .actualizarConfiguracion(this.empresaSeleccionada.id, payload)
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
        this.alerts.success('Configuracion actualizada', 'La configuracion de correo y ambiente fue guardada correctamente.');
      });
  }

  protected onCertificadoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const archivo = input?.files?.[0];

    if (!archivo) {
      this.certificadoArchivoBase64 = null;
      this.certificadoArchivoNombre = '';
      return;
    }

    this.certificadoArchivoNombre = archivo.name;
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const base64 = result.includes(',') ? result.split(',')[1] : result;
      this.certificadoArchivoBase64 = base64 || null;
    };
    reader.onerror = () => {
      this.certificadoArchivoBase64 = null;
      this.certificadoArchivoNombre = '';
      this.alerts.error('Archivo invalido', 'No se pudo leer el certificado seleccionado.');
    };
    reader.readAsDataURL(archivo);
  }

  protected limpiarCertificadoActual(): void {
    if (!this.empresaSeleccionada || this.saving) {
      return;
    }

    const payload: EmpresaConfiguracionRequest = {
      ambienteSri: Number(this.form.controls['ambienteSri'].value ?? 1),
      correoNotificaciones: this.form.controls['correoNotificaciones'].value ?? '',
      correoRespuesta: this.form.controls['correoRespuesta'].value ?? '',
      certificadoNombre: '',
      certificadoBase64: null,
      certificadoClave: null,
      limpiarCertificado: true
    };

    this.saving = true;

    this.empresasService
      .actualizarConfiguracion(this.empresaSeleccionada.id, payload)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo limpiar', 'El backend no pudo eliminar el certificado actual.');
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
        this.alerts.success('Certificado limpiado', 'El certificado actual fue eliminado de la configuracion.');
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
      .obtenerConfiguracion(empresaId)
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
