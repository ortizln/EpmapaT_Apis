import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { EmpresasService } from '../../core/services/empresas.service';
import {
  Empresa,
  EmpresaConfiguracion,
  EmpresaConfiguracionRequest,
  EmpresaRequest
} from '../../models/empresa.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-empresa-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './empresa-form-page.component.html',
  styleUrl: './empresa-form-page.component.scss'
})
export class EmpresaFormPageComponent {
  private readonly empresasService = inject(EmpresasService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alerts = inject(AppAlertService);

  protected editingId: string | null = null;
  protected saving = false;
  protected formError = '';
  protected configError = '';
  protected configuracion: EmpresaConfiguracion | null = null;
  protected loadingConfiguracion = false;
  protected savingConfig = false;
  protected certificadoBase64: string | null = null;
  protected archivoCertificadoNombre = '';

  protected readonly form = this.fb.group({
    ruc: ['', [Validators.required, Validators.pattern(/^\d{13}$/)]],
    razonSocial: ['', [Validators.required]],
    nombreComercial: [''],
    direccionMatriz: [''],
    contribuyenteEspecial: [''],
    obligadoContabilidad: [true]
  });

  protected readonly configForm = this.fb.group({
    ambienteSri: [1, [Validators.required]],
    correoNotificaciones: ['', [Validators.email]],
    correoRespuesta: ['', [Validators.email]],
    certificadoNombre: [''],
    certificadoClave: [''],
    limpiarCertificado: [false]
  });

  constructor() {
    this.editingId = this.route.snapshot.paramMap.get('id');

    if (this.editingId) {
      const stateEmpresa = history.state['empresa'] as Empresa | undefined;
      if (stateEmpresa) {
        this.patchEmpresa(stateEmpresa);
        this.cargarConfiguracion(this.editingId);
      } else {
        this.cargarEmpresaDesdeListado(this.editingId);
      }
    }
  }

  protected volver(): void {
    this.router.navigate(['/catalogos'], { queryParams: { tab: 'empresas' } });
  }

  protected guardar(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Revisa los datos requeridos de la empresa.');
      return;
    }

    this.saving = true;
    this.formError = '';

    const payload = this.form.getRawValue() as EmpresaRequest;
    const request$ = this.editingId
      ? this.empresasService.actualizar(this.editingId, payload)
      : this.empresasService.crear(payload);

    request$
      .pipe(
        catchError(() => {
          this.formError = 'No fue posible guardar la empresa. Revisa los datos e intenta nuevamente.';
          this.alerts.error('Operacion fallida', 'No se pudo guardar la informacion de la empresa.');
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

        this.alerts.success('Empresa guardada', `La empresa ${response.razonSocial} fue almacenada correctamente.`);
        this.router.navigate(['/catalogos'], { queryParams: { tab: 'empresas' } });
      });
  }

  protected onCertificadoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      this.certificadoBase64 = null;
      this.archivoCertificadoNombre = '';
      return;
    }

    this.archivoCertificadoNombre = file.name;
    this.configForm.patchValue({ certificadoNombre: file.name, limpiarCertificado: false });

    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const base64 = result.includes(',') ? result.split(',')[1] ?? '' : result;
      this.certificadoBase64 = base64 || null;
    };
    reader.onerror = () => {
      this.configError = 'No fue posible leer el certificado seleccionado.';
      this.certificadoBase64 = null;
      this.archivoCertificadoNombre = '';
      this.alerts.error('Archivo invalido', 'No fue posible procesar el certificado seleccionado.');
    };
    reader.readAsDataURL(file);
  }

  protected guardarConfiguracion(): void {
    if (!this.editingId || this.savingConfig) {
      return;
    }

    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    const limpiarCertificado = this.configForm.controls['limpiarCertificado'].value ?? false;
    const certificadoClave = (this.configForm.controls['certificadoClave'].value ?? '').trim();
    if (this.certificadoBase64 && !certificadoClave) {
      this.configError = 'Debes ingresar la clave del certificado para poder cargar el archivo.';
      return;
    }

    this.savingConfig = true;
    this.configError = '';

    const payload: EmpresaConfiguracionRequest = {
      ambienteSri: Number(this.configForm.controls['ambienteSri'].value ?? 1),
      correoNotificaciones: (this.configForm.controls['correoNotificaciones'].value ?? '').trim(),
      correoRespuesta: (this.configForm.controls['correoRespuesta'].value ?? '').trim(),
      certificadoNombre: (this.configForm.controls['certificadoNombre'].value ?? '').trim(),
      certificadoBase64: limpiarCertificado ? null : this.certificadoBase64,
      certificadoClave: limpiarCertificado ? null : (certificadoClave || null),
      limpiarCertificado
    };

    this.empresasService
      .actualizarConfiguracion(this.editingId, payload)
      .pipe(
        catchError(() => {
          this.configError = 'No fue posible guardar la configuracion SRI/correo.';
          this.alerts.error('Configuracion no guardada', 'No se pudo actualizar la configuracion de la empresa.');
          return of(null);
        }),
        finalize(() => {
          this.savingConfig = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.configuracion = response;
        this.certificadoBase64 = null;
        this.archivoCertificadoNombre = '';
        this.configForm.patchValue({
          ambienteSri: response.ambienteSri,
          correoNotificaciones: response.correoNotificaciones || '',
          correoRespuesta: response.correoRespuesta || '',
          certificadoNombre: response.certificadoNombre || '',
          certificadoClave: '',
          limpiarCertificado: false
        });
        this.alerts.success('Configuracion actualizada', 'La configuracion SRI/correo fue guardada correctamente.');
      });
  }

  protected formatVigencia(desde: string | null, hasta: string | null): string {
    if (!desde && !hasta) {
      return 'N/D';
    }
    const desdeFmt = desde ? new Date(desde).toLocaleDateString() : 'N/D';
    const hastaFmt = hasta ? new Date(hasta).toLocaleDateString() : 'N/D';
    return `${desdeFmt} - ${hastaFmt}`;
  }

  protected getCertificadoEstado(): {
    tone: 'danger' | 'warning' | 'success' | 'muted';
    title: string;
    detail: string;
  } {
    if (!this.configuracion?.certificadoConfigurado) {
      return {
        tone: 'muted',
        title: 'Certificado pendiente',
        detail: 'Carga un archivo .p12 o .pfx para habilitar la firma electronica de esta empresa.'
      };
    }

    const vigenciaHasta = this.configuracion.certificadoVigenciaHasta;
    if (!vigenciaHasta) {
      return {
        tone: 'warning',
        title: 'Vigencia no disponible',
        detail: 'El certificado existe, pero no fue posible determinar su fecha de expiracion.'
      };
    }

    const ahora = new Date();
    const expiracion = new Date(vigenciaHasta);
    const diffMs = expiracion.getTime() - ahora.getTime();
    const diasRestantes = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

    if (Number.isNaN(expiracion.getTime())) {
      return {
        tone: 'warning',
        title: 'Fecha invalida',
        detail: 'La fecha de expiracion reportada por el backend no pudo interpretarse correctamente.'
      };
    }

    if (diasRestantes < 0) {
      return {
        tone: 'danger',
        title: 'Certificado vencido',
        detail: `El certificado expiro el ${expiracion.toLocaleDateString()}. Debes reemplazarlo antes de emitir documentos.`
      };
    }

    if (diasRestantes <= 30) {
      return {
        tone: 'warning',
        title: 'Certificado por vencer',
        detail: `Quedan ${diasRestantes} dia(s) de vigencia. Conviene renovarlo pronto para evitar interrupciones.`
      };
    }

    return {
      tone: 'success',
      title: 'Certificado vigente',
      detail: `El certificado esta operativo y vence en ${diasRestantes} dia(s).`
    };
  }

  private cargarEmpresaDesdeListado(id: string): void {
    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.alerts.error('Empresa no disponible', 'No fue posible recuperar la empresa a editar.');
          this.volver();
          return of([]);
        })
      )
      .subscribe((response) => {
        const empresa = response.find((item) => item.id === id);
        if (!empresa) {
          this.alerts.warning('Empresa no encontrada', 'La empresa seleccionada ya no esta disponible.');
          this.volver();
          return;
        }

        this.patchEmpresa(empresa);
        this.cargarConfiguracion(id);
      });
  }

  private patchEmpresa(empresa: Empresa): void {
    this.form.patchValue({
      ruc: empresa.ruc,
      razonSocial: empresa.razonSocial,
      nombreComercial: empresa.nombreComercial || '',
      direccionMatriz: empresa.direccionMatriz || '',
      contribuyenteEspecial: empresa.contribuyenteEspecial || '',
      obligadoContabilidad: empresa.obligadoContabilidad
    });
  }

  private cargarConfiguracion(empresaId: string): void {
    this.loadingConfiguracion = true;
    this.configError = '';

    this.empresasService
      .obtenerConfiguracion(empresaId)
      .pipe(
        catchError(() => {
          this.configError = 'No fue posible cargar la configuracion de la empresa seleccionada.';
          this.configuracion = null;
          return of(null);
        }),
        finalize(() => {
          this.loadingConfiguracion = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.configuracion = response;
        this.configForm.patchValue({
          ambienteSri: response.ambienteSri,
          correoNotificaciones: response.correoNotificaciones || '',
          correoRespuesta: response.correoRespuesta || '',
          certificadoNombre: response.certificadoNombre || '',
          certificadoClave: '',
          limpiarCertificado: false
        });
      });
  }
}
