import { CommonModule } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { EmpresasService } from '../../core/services/empresas.service';
import {
  Empresa,
  EmpresaConfiguracion,
  EmpresaConfiguracionRequest,
  EmpresaRequest
} from '../../models/empresa.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-empresas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, HasPermissionDirective],
  templateUrl: './empresas-page.component.html',
  styleUrl: './empresas-page.component.scss'
})
export class EmpresasPageComponent {
  readonly embedded = input(false);
  private readonly empresasService = inject(EmpresasService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected empresas: Empresa[] = [];
  protected loading = false;
  protected saving = false;
  protected error = '';
  protected formError = '';
  protected configError = '';
  protected editingId: string | null = null;
  protected savingStateId: string | null = null;
  protected selectedEmpresa: Empresa | null = null;
  protected configuracion: EmpresaConfiguracion | null = null;
  protected loadingConfiguracion = false;
  protected savingConfig = false;
  protected certificadoBase64: string | null = null;
  protected archivoCertificadoNombre = '';
  protected page = 0;
  protected size = 10;
  protected totalPages = 0;
  protected totalItems = 0;
  protected readonly sizeOptions = [10, 20, 50];

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
    this.cargarEmpresas();
  }

  protected irANuevaEmpresa(): void {
    this.router.navigate(['/catalogos/empresas/nuevo']);
  }

  protected irAEditarEmpresa(empresa: Empresa): void {
    this.router.navigate([`/catalogos/empresas/${empresa.id}/editar`], {
      state: { empresa }
    });
  }

  protected guardar(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
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
          this.empresas = this.empresas.map((item) => (item.id === response.id ? response : item));
        } else {
          this.empresas = [response, ...this.empresas];
        }
        this.resetForm();
      });
  }

  protected editar(empresa: Empresa): void {
    this.editingId = empresa.id;
    this.formError = '';
    this.form.patchValue({
      ruc: empresa.ruc,
      razonSocial: empresa.razonSocial,
      nombreComercial: empresa.nombreComercial || '',
      direccionMatriz: empresa.direccionMatriz || '',
      contribuyenteEspecial: empresa.contribuyenteEspecial || '',
      obligadoContabilidad: empresa.obligadoContabilidad
    });
    this.abrirConfiguracion(empresa);
  }

  protected abrirConfiguracion(empresa: Empresa): void {
    this.selectedEmpresa = empresa;
    this.archivoCertificadoNombre = '';
    this.certificadoBase64 = null;
    this.configError = '';
    this.configForm.patchValue({
      ambienteSri: empresa.ambienteSri ?? 1,
      correoNotificaciones: empresa.correoNotificaciones || '',
      correoRespuesta: '',
      certificadoNombre: '',
      certificadoClave: '',
      limpiarCertificado: false
    });
    this.cargarConfiguracion(empresa.id);
  }

  protected cambiarEstado(empresa: Empresa): void {
    this.savingStateId = empresa.id;

    this.empresasService
      .actualizarEstado(empresa.id, !empresa.activo)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible actualizar el estado de la empresa.';
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

        this.empresas = this.empresas.map((item) => (item.id === response.id ? response : item));
      });
  }

  protected resetForm(): void {
    this.editingId = null;
    this.formError = '';
    this.form.reset({
      ruc: '',
      razonSocial: '',
      nombreComercial: '',
      direccionMatriz: '',
      contribuyenteEspecial: '',
      obligadoContabilidad: true
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
    };
    reader.readAsDataURL(file);
  }

  protected guardarConfiguracion(): void {
    if (!this.selectedEmpresa || this.savingConfig) {
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
      .actualizarConfiguracion(this.selectedEmpresa.id, payload)
      .pipe(
        catchError(() => {
          this.configError = 'No fue posible guardar la configuracion SRI/correo.';
          return of(null);
        }),
        finalize(() => {
          this.savingConfig = false;
        })
      )
      .subscribe((response) => {
        if (!response || !this.selectedEmpresa) {
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

        this.empresas = this.empresas.map((item) =>
          item.id === this.selectedEmpresa?.id
            ? {
                ...item,
                ambienteSri: response.ambienteSri,
                correoNotificaciones: response.correoNotificaciones,
                certificadoConfigurado: response.certificadoConfigurado
              }
            : item
        );

        this.selectedEmpresa = this.empresas.find((item) => item.id === this.selectedEmpresa?.id) ?? this.selectedEmpresa;
      });
  }

  protected recargarConfiguracion(): void {
    if (!this.selectedEmpresa) {
      return;
    }
    this.cargarConfiguracion(this.selectedEmpresa.id);
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

  protected onPageChange(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }

    this.page = page;
    this.cargarEmpresas();
  }

  protected onSizeChange(event: Event): void {
    const nextSize = Number((event.target as HTMLSelectElement).value);
    if (Number.isNaN(nextSize) || nextSize === this.size) {
      return;
    }

    this.size = nextSize;
    this.page = 0;
    this.cargarEmpresas();
  }

  protected visiblePages(): number[] {
    if (this.totalPages <= 1) {
      return [0];
    }

    const start = Math.max(0, this.page - 1);
    const end = Math.min(this.totalPages - 1, start + 2);
    const adjustedStart = Math.max(0, end - 2);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }

  protected getRangeStart(): number {
    return this.totalItems === 0 ? 0 : this.page * this.size + 1;
  }

  protected getRangeEnd(): number {
    return Math.min((this.page + 1) * this.size, this.totalItems);
  }

  private cargarEmpresas(): void {
    this.loading = true;
    this.error = '';

    this.empresasService
      .listarPaginado(this.page, this.size)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar las empresas desde el backend.';
          this.empresas = [];
          this.totalItems = 0;
          this.totalPages = 0;
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

        this.empresas = response.items;
        this.page = response.page;
        this.size = response.size;
        this.totalPages = response.totalPages;
        this.totalItems = response.totalItems;
        if (this.selectedEmpresa) {
          this.selectedEmpresa = this.empresas.find((item) => item.id === this.selectedEmpresa?.id) ?? this.selectedEmpresa;
        } else if (this.empresas.length) {
          this.abrirConfiguracion(this.empresas[0]);
        }
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
