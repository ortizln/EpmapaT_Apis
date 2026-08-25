import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { DocumentoContratoService } from '../../core/services/documento-contrato.service';
import { EmpresasService } from '../../core/services/empresas.service';
import { PlantillasRideService } from '../../core/services/plantillas-ride.service';
import { RecursosEmpresaService } from '../../core/services/recursos-empresa.service';
import { DocumentoResumen, TipoDocumento } from '../../models/documento.model';
import { Empresa } from '../../models/empresa.model';
import { PlantillaRide, RideContratoDocumento } from '../../models/plantilla-ride.model';
import { RecursoEmpresa, RecursoEmpresaTipo } from '../../models/recurso-empresa.model';
import { AppModalComponent } from '../../shared/components/app-modal/app-modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-plantillas-ride-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, HasPermissionDirective, AppModalComponent],
  templateUrl: './plantillas-ride-page.component.html',
  styleUrl: './plantillas-ride-page.component.scss'
})
export class PlantillasRidePageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly empresasService = inject(EmpresasService);
  private readonly documentoContratoService = inject(DocumentoContratoService);
  private readonly plantillasRideService = inject(PlantillasRideService);
  private readonly recursosEmpresaService = inject(RecursosEmpresaService);
  private readonly alerts = inject(AppAlertService);
  private readonly fb = inject(UntypedFormBuilder);

  protected readonly tiposDocumento: TipoDocumento[] = [
    'FACTURA',
    'RETENCION',
    'GUIA_REMISION',
    'NOTA_CREDITO',
    'NOTA_DEBITO',
    'LIQUIDACION_COMPRA'
  ];
  protected readonly tiposRecurso: RecursoEmpresaTipo[] = ['LOGO_PRINCIPAL', 'LOGO_SECUNDARIO', 'MARCA_AGUA'];

  protected empresas: Empresa[] = [];
  protected empresaSeleccionada: Empresa | null = null;
  protected plantillas: PlantillaRide[] = [];
  protected documentosDisponibles: DocumentoResumen[] = [];
  protected recursos: RecursoEmpresa[] = [];
  protected plantillaSeleccionada: PlantillaRide | null = null;
  protected contrato: RideContratoDocumento | null = null;
  protected loadingEmpresas = false;
  protected loadingPlantillas = false;
  protected loadingDocumentos = false;
  protected loadingRecursos = false;
  protected loadingContrato = false;
  protected saving = false;
  protected savingRecurso = false;
  protected previewing = false;
  protected errorEmpresas = '';
  protected errorPlantillas = '';
  protected errorDocumentos = '';
  protected errorRecursos = '';
  protected errorContrato = '';
  protected selectedFile: File | null = null;
  protected selectedFileName = '';
  protected recursoFile: File | null = null;
  protected recursoFileName = '';
  protected contratoModalOpen = false;
  protected readonly contratoSearch = signal('');

  protected readonly form = this.fb.group({
    uuid: this.fb.control(null),
    tipoDocumento: this.fb.control('FACTURA', [Validators.required]),
    nombre: this.fb.control('', [Validators.required]),
    version: this.fb.control('1.0.0', [Validators.required]),
    predeterminada: this.fb.control(true),
    activa: this.fb.control(true),
    documentoUuid: this.fb.control('', [Validators.required])
  });

  protected readonly recursoForm = this.fb.group({
    tipo: this.fb.control('LOGO_PRINCIPAL', [Validators.required]),
    nombre: this.fb.control('', [Validators.required])
  });
  protected readonly contratoParametrosFiltrados = computed(() => {
    const contrato = this.contrato;
    const term = this.contratoSearch().trim().toLowerCase();
    const items = contrato?.parametros ?? [];
    if (!term) {
      return items;
    }

    return items.filter((item) =>
      [item.nombre, item.valorEjemplo].some((value) => String(value ?? '').toLowerCase().includes(term))
    );
  });
  protected readonly contratoCamposFiltrados = computed(() => {
    const contrato = this.contrato;
    const term = this.contratoSearch().trim().toLowerCase();
    const items = contrato?.detail?.campos ?? [];
    if (!term) {
      return items;
    }

    return items.filter((item) =>
      [item.nombre, item.valorEjemplo].some((value) => String(value ?? '').toLowerCase().includes(term))
    );
  });
  protected readonly contratoRecursosFiltrados = computed(() => {
    const contrato = this.contrato;
    const term = this.contratoSearch().trim().toLowerCase();
    const items = contrato?.recursos ?? [];
    if (!term) {
      return items;
    }

    return items.filter((item) =>
      [item.nombre, item.valorEjemplo].some((value) => String(value ?? '').toLowerCase().includes(term))
    );
  });
  protected readonly contratoResultadosCount = computed(
    () =>
      this.contratoParametrosFiltrados().length +
      this.contratoCamposFiltrados().length +
      this.contratoRecursosFiltrados().length
  );

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

    effect(() => {
      const empresaId = this.empresaSeleccionada?.id;
      const tipoDocumento = this.form.controls['tipoDocumento'].value as TipoDocumento;
      if (!empresaId || !tipoDocumento) {
        return;
      }
      this.cargarDocumentosDisponibles(empresaId, tipoDocumento);
    });

    this.cargarEmpresas();
  }

  protected seleccionarEmpresa(empresa: Empresa): void {
    this.companyContext.seleccionarEmpresa(empresa.id);
    this.empresaSeleccionada = empresa;
    this.plantillaSeleccionada = null;
    this.contrato = null;
    this.contratoModalOpen = false;
    this.resetForm();
    this.cargarPlantillas(empresa.id);
    this.cargarRecursos(empresa.id);
  }

  protected seleccionarPlantilla(plantilla: PlantillaRide): void {
    this.plantillaSeleccionada = plantilla;
    this.form.patchValue({
      uuid: plantilla.uuid,
      tipoDocumento: plantilla.tipoDocumento,
      nombre: plantilla.nombre,
      version: plantilla.version,
      predeterminada: plantilla.predeterminada,
      activa: plantilla.activa
    });
  }

  protected nuevaPlantilla(): void {
    this.plantillaSeleccionada = null;
    this.resetForm();
  }

  protected onTipoDocumentoChange(): void {
    const empresaId = this.empresaSeleccionada?.id;
    const tipoDocumento = this.form.controls['tipoDocumento'].value as TipoDocumento;
    if (!empresaId || !tipoDocumento) {
      return;
    }
    this.form.patchValue({ documentoUuid: '' });
    this.contrato = null;
    this.contratoModalOpen = false;
    this.cargarDocumentosDisponibles(empresaId, tipoDocumento);
  }

  protected onDocumentoSeleccionado(value: string): void {
    this.form.patchValue({ documentoUuid: value });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.selectedFile = file;
    this.selectedFileName = file?.name ?? '';
  }

  protected onRecursoFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.recursoFile = file;
    this.recursoFileName = file?.name ?? '';
  }

  protected guardar(): void {
    if (!this.empresaSeleccionada || this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      tipoDocumento: this.form.controls['tipoDocumento'].value as TipoDocumento,
      nombre: String(this.form.controls['nombre'].value ?? ''),
      version: String(this.form.controls['version'].value ?? ''),
      predeterminada: Boolean(this.form.controls['predeterminada'].value),
      activa: Boolean(this.form.controls['activa'].value)
    };

    this.saving = true;
    const request$ = this.form.controls['uuid'].value
      ? this.plantillasRideService.actualizar(String(this.form.controls['uuid'].value), payload, this.selectedFile)
      : this.plantillasRideService.crear(this.empresaSeleccionada.id, payload, this.selectedFile);

    request$
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo guardar', 'La plantilla RIDE no pudo guardarse en el backend.');
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((plantilla) => {
        if (!plantilla || !this.empresaSeleccionada) {
          return;
        }
        this.alerts.success('Plantilla guardada', 'La plantilla JRXML fue registrada correctamente.');
        this.selectedFile = null;
        this.selectedFileName = '';
        this.cargarPlantillas(this.empresaSeleccionada.id, plantilla.uuid);
      });
  }

  protected cambiarEstado(plantilla: PlantillaRide): void {
    this.plantillasRideService
      .actualizarEstado(plantilla.uuid, !plantilla.activa)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo actualizar', 'No fue posible cambiar el estado de la plantilla.');
          return of(null);
        })
      )
      .subscribe((actualizada) => {
        if (!actualizada || !this.empresaSeleccionada) {
          return;
        }
        this.alerts.success('Estado actualizado', 'La plantilla cambio su estado correctamente.');
        this.cargarPlantillas(this.empresaSeleccionada.id, actualizada.uuid);
      });
  }

  protected verificar(plantilla: PlantillaRide): void {
    this.plantillasRideService
      .verificar(plantilla.uuid)
      .pipe(
        catchError(() => {
          this.alerts.error('Verificacion fallida', 'No fue posible verificar la plantilla seleccionada.');
          return of(null);
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }
        if (response.valida) {
          this.alerts.success('Plantilla valida', response.mensaje);
        } else {
          this.alerts.warning('Plantilla invalida', response.mensaje);
        }
      });
  }

  protected descargarBase(): void {
    const tipoDocumento = this.form.controls['tipoDocumento'].value as TipoDocumento;
    this.plantillasRideService.descargarBase(tipoDocumento).subscribe((blob) => {
      this.downloadBlob(blob, `base_${tipoDocumento.toLowerCase()}.jrxml`);
    });
  }

  protected guardarRecurso(): void {
    if (!this.empresaSeleccionada || !this.recursoFile || this.recursoForm.invalid || this.savingRecurso) {
      this.recursoForm.markAllAsTouched();
      return;
    }

    this.savingRecurso = true;
    this.recursosEmpresaService
      .crear(
        this.empresaSeleccionada.id,
        this.recursoForm.controls['tipo'].value as RecursoEmpresaTipo,
        String(this.recursoForm.controls['nombre'].value ?? ''),
        this.recursoFile
      )
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo cargar recurso', 'No fue posible registrar el logo o marca de agua.');
          return of(null);
        }),
        finalize(() => {
          this.savingRecurso = false;
        })
      )
      .subscribe((recurso) => {
        if (!recurso || !this.empresaSeleccionada) {
          return;
        }
        this.alerts.success('Recurso cargado', 'El recurso grafico fue registrado correctamente.');
        this.recursoFile = null;
        this.recursoFileName = '';
        this.recursoForm.reset({
          tipo: 'LOGO_PRINCIPAL',
          nombre: ''
        });
        this.cargarRecursos(this.empresaSeleccionada.id);
      });
  }

  protected cargarContratoDocumento(): void {
    const documentoUuid = String(this.form.controls['documentoUuid'].value ?? '').trim();
    if (!documentoUuid) {
      this.form.controls['documentoUuid'].markAsTouched();
      return;
    }

    this.loadingContrato = true;
    this.errorContrato = '';
    this.contrato = null;
    this.contratoSearch.set('');

    this.plantillasRideService
      .contratoDocumento(documentoUuid)
      .pipe(
        catchError(() => {
          this.errorContrato = 'No fue posible obtener el contrato RIDE del documento seleccionado.';
          return of(null);
        }),
        finalize(() => {
          this.loadingContrato = false;
        })
      )
      .subscribe((contrato) => {
        this.contrato = contrato;
        this.contratoModalOpen = Boolean(contrato);
      });
  }

  protected updateContratoSearch(value: string): void {
    this.contratoSearch.set(value);
  }

  protected clearContratoSearch(): void {
    this.contratoSearch.set('');
  }

  protected preview(): void {
    const documentoUuid = String(this.form.controls['documentoUuid'].value ?? '').trim();
    if (!this.plantillaSeleccionada || !documentoUuid || this.previewing) {
      return;
    }
    this.previewing = true;
    this.plantillasRideService
      .preview(this.plantillaSeleccionada.uuid, documentoUuid)
      .pipe(
        catchError(() => {
          this.alerts.error('Preview fallido', 'No fue posible generar la vista previa del RIDE.');
          return of(null);
        }),
        finalize(() => {
          this.previewing = false;
        })
      )
      .subscribe((blob) => {
        if (!blob) {
          return;
        }
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener,noreferrer');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      });
  }

  protected descargarPreview(): void {
    const documentoUuid = String(this.form.controls['documentoUuid'].value ?? '').trim();
    if (!this.plantillaSeleccionada || !documentoUuid || this.previewing) {
      return;
    }
    this.previewing = true;
    this.plantillasRideService
      .preview(this.plantillaSeleccionada.uuid, documentoUuid)
      .pipe(
        catchError(() => {
          this.alerts.error('Descarga fallida', 'No fue posible descargar la vista previa del RIDE.');
          return of(null);
        }),
        finalize(() => {
          this.previewing = false;
        })
      )
      .subscribe((blob) => {
        if (!blob) {
          return;
        }
        this.downloadBlob(blob, `ride_preview_${this.plantillaSeleccionada?.tipoDocumento.toLowerCase()}.pdf`);
      });
  }

  protected cerrarContratoModal(): void {
    this.contratoModalOpen = false;
  }

  protected cambiarEstadoRecurso(recurso: RecursoEmpresa): void {
    this.recursosEmpresaService
      .actualizarEstado(recurso.uuid, !recurso.activo)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo actualizar recurso', 'No fue posible cambiar el estado del recurso grafico.');
          return of(null);
        })
      )
      .subscribe((actualizado) => {
        if (!actualizado || !this.empresaSeleccionada) {
          return;
        }
        this.alerts.success('Recurso actualizado', 'El estado del recurso fue actualizado.');
        this.cargarRecursos(this.empresaSeleccionada.id);
      });
  }

  private cargarEmpresas(): void {
    this.loadingEmpresas = true;
    this.errorEmpresas = '';

    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.errorEmpresas = 'No fue posible cargar las empresas para administrar plantillas RIDE.';
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
          this.cargarPlantillas(primeraEmpresa.id);
        }
      });
  }

  private cargarPlantillas(empresaId: string, selectedUuid?: string): void {
    this.loadingPlantillas = true;
    this.errorPlantillas = '';
    this.plantillas = [];

    this.plantillasRideService
      .listar(empresaId)
      .pipe(
        catchError(() => {
          this.errorPlantillas = 'No fue posible cargar las plantillas RIDE de la empresa.';
          return of([]);
        }),
        finalize(() => {
          this.loadingPlantillas = false;
        })
      )
      .subscribe((plantillas) => {
        this.plantillas = plantillas;
        const selected = plantillas.find((item) => item.uuid === selectedUuid) ?? plantillas[0] ?? null;
        if (selected) {
          this.seleccionarPlantilla(selected);
        } else {
          this.nuevaPlantilla();
        }
      });
  }

  private cargarDocumentosDisponibles(empresaId: string, tipoDocumento: TipoDocumento): void {
    this.loadingDocumentos = true;
    this.errorDocumentos = '';
    this.documentosDisponibles = [];

    this.documentoContratoService
      .listarDocumentos({
        empresaUuid: empresaId,
        tipoDocumento,
        page: 0,
        size: 50
      })
      .pipe(
        catchError(() => {
          this.errorDocumentos = 'No fue posible cargar documentos para usar como ejemplo de plantilla.';
          return of(null);
        }),
        finalize(() => {
          this.loadingDocumentos = false;
        })
      )
      .subscribe((response) => {
        const items = response?.items ?? [];
        this.documentosDisponibles = items.filter((item) =>
          ['AUTORIZADO', 'RIDE_GENERADO', 'CORREO_PENDIENTE', 'CORREO_ENVIADO', 'FINALIZADO'].includes(item.estado)
        );
      });
  }

  private cargarRecursos(empresaId: string): void {
    this.loadingRecursos = true;
    this.errorRecursos = '';
    this.recursos = [];

    this.recursosEmpresaService
      .listar(empresaId)
      .pipe(
        catchError(() => {
          this.errorRecursos = 'No fue posible cargar los recursos graficos de la empresa.';
          return of([]);
        }),
        finalize(() => {
          this.loadingRecursos = false;
        })
      )
      .subscribe((recursos) => {
        this.recursos = recursos;
      });
  }

  private resetForm(): void {
    this.form.reset({
      uuid: null,
      tipoDocumento: 'FACTURA',
      nombre: '',
      version: '1.0.0',
      predeterminada: true,
      activa: true,
      documentoUuid: this.form.controls['documentoUuid'].value ?? ''
    });
    this.selectedFile = null;
    this.selectedFileName = '';
  }

  private downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
