import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import {
  DocumentoArchivoItemResponse,
  DocumentoAutorizacionConsultaResponse,
  DocumentoAutorizacionManualResponse,
  DocumentoAuditoriaEventoResponse,
  DocumentoCorreoReenvioResponse,
  DocumentoCorreoSeguimientoResponse,
  DocumentoDetalleResponse,
  DocumentoErrorItemResponse,
  DocumentoEstadoResponse,
  DocumentoEstadoTimelineItem,
  DocumentoHistorialItemResponse,
  DocumentoIntentoSriResponse
} from '../../models/documento.model';
import { AppAlertService } from '../../core/services/app-alert.service';
import { DocumentoContratoService } from '../../core/services/documento-contrato.service';
import { DocumentoAuditoriaService } from '../../core/services/documento-auditoria.service';
import { AppModalComponent } from '../../shared/components/app-modal/app-modal.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';
import { HasPermissionDirective } from '../../shared/directives/has-permission.directive';

type DocumentoDetalleTab = 'resumen' | 'jsonXml' | 'archivos' | 'sri' | 'historial' | 'errores' | 'correos' | 'auditoria';

@Component({
  selector: 'app-documento-detalle-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, AppModalComponent, StatusChipComponent, HasPermissionDirective],
  templateUrl: './documento-detalle-page.component.html',
  styleUrl: './documento-detalle-page.component.scss'
})
export class DocumentoDetallePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly documentoService = inject(DocumentoContratoService);
  private readonly documentoAuditoriaService = inject(DocumentoAuditoriaService);
  private readonly alerts = inject(AppAlertService);

  protected readonly tabs: Array<{ id: DocumentoDetalleTab; label: string }> = [
    { id: 'resumen', label: 'Resumen' },
    { id: 'jsonXml', label: 'JSON / XML' },
    { id: 'archivos', label: 'Archivos' },
    { id: 'sri', label: 'SRI' },
    { id: 'historial', label: 'Historial' },
    { id: 'errores', label: 'Errores' },
    { id: 'correos', label: 'Correos' },
    { id: 'auditoria', label: 'Auditoria' }
  ];

  protected readonly activeTab = signal<DocumentoDetalleTab>('resumen');
  protected readonly documento = signal<DocumentoDetalleResponse | null>(null);
  protected readonly estado = signal<DocumentoEstadoResponse | null>(null);
  protected readonly autorizacion = signal<DocumentoAutorizacionManualResponse | null>(null);
  protected readonly autorizacionClave = signal<DocumentoAutorizacionConsultaResponse | null>(null);
  protected readonly historialItems = signal<DocumentoHistorialItemResponse[]>([]);
  protected readonly archivosItems = signal<DocumentoArchivoItemResponse[]>([]);
  protected readonly auditoriaItems = signal<DocumentoAuditoriaEventoResponse[]>([]);
  protected readonly intentosSri = signal<DocumentoIntentoSriResponse | null>(null);
  protected readonly seguimientoCorreo = signal<DocumentoCorreoSeguimientoResponse | null>(null);
  protected readonly erroresItems = signal<DocumentoErrorItemResponse[]>([]);
  protected readonly xmlAutorizado = signal<string | null>(null);
  protected readonly xmlModalOpen = signal(false);
  protected readonly xmlSearch = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly consultandoAutorizacion = signal(false);
  protected readonly reprocesando = signal(false);
  protected readonly cargandoXmlManual = signal(false);
  protected readonly regenerandoRide = signal(false);
  protected readonly autorizacionError = signal('');
  protected readonly consultandoPorClave = signal(false);
  protected readonly autorizacionClaveError = signal('');
  protected readonly consultandoXml = signal(false);
  protected readonly xmlError = signal('');
  protected readonly loadingHistorial = signal(false);
  protected readonly loadingArchivos = signal(false);
  protected readonly loadingAuditoria = signal(false);
  protected readonly historialError = signal('');
  protected readonly archivosError = signal('');
  protected readonly auditoriaError = signal('');
  protected readonly loadingIntentosSri = signal(false);
  protected readonly intentosSriError = signal('');
  protected readonly loadingCorreo = signal(false);
  protected readonly correoError = signal('');
  protected readonly reenviandoCorreo = signal(false);
  protected readonly correoSuccess = signal('');
  protected readonly loadingErrores = signal(false);
  protected readonly erroresError = signal('');
  protected readonly xmlSinFirmarNombre = signal('');

  protected readonly estadoActual = computed(() => this.estado()?.estado || this.documento()?.estado || 'SIN ESTADO');
  protected readonly puedeReprocesar = computed(() => {
    const estado = this.estadoActual().toUpperCase();
    return [
      'ERROR_VALIDACION',
      'ERROR_XML',
      'ERROR_FIRMA',
      'DEVUELTO_SRI',
      'NO_AUTORIZADO',
      'ERROR_ENVIO_SRI',
      'ERROR_AUTORIZACION',
      'ERROR_RIDE',
      'ERROR_CORREO',
      'REQUIERE_INTERVENCION'
    ].includes(estado);
  });
  protected readonly puedeRegenerarRide = computed(() => {
    const estado = this.estadoActual().toUpperCase();
    return ['AUTORIZADO', 'RIDE_GENERADO', 'CORREO_PENDIENTE', 'CORREO_ENVIADO', 'FINALIZADO', 'ERROR_RIDE'].includes(estado);
  });
  protected readonly headerDescription = computed(() => {
    const documento = this.documento();
    if (!documento) {
      return 'Consulta operativa del documento, su estado actual y sus referencias principales.';
    }

    return `${documento.tipoDocumento} ${documento.numeroDocumento || 'sin numero'} con trazabilidad operativa y datos del receptor.`;
  });

  protected readonly timeline = computed<DocumentoEstadoTimelineItem[]>(() => {
    const documento = this.documento();
    const estado = this.estado();

    if (!documento) {
      return [];
    }

    return [
      {
        titulo: 'Recepcion',
        descripcion: 'Registro base del comprobante dentro de la plataforma.',
        valor: documento.fechaRecepcion || 'Pendiente',
        resaltado: true
      },
      {
        titulo: 'Emision',
        descripcion: 'Fecha comercial reportada por el comprobante.',
        valor: documento.fechaEmision || 'No informada'
      },
      {
        titulo: 'Estado backend',
        descripcion: 'Estado devuelto por el servicio operativo actual.',
        valor: estado?.estado || documento.estado || 'Sin estado'
      },
      {
        titulo: 'Intervencion',
        descripcion: 'Bandera operativa para trabajo manual o seguimiento.',
        valor: estado?.requiereIntervencion ? 'Requerida' : 'No requerida'
      }
    ];
  });
  protected readonly xmlFormateado = computed(() => this.prettyPrintXml(this.xmlAutorizado()));
  protected readonly xmlSearchResults = computed(() => {
    const term = this.xmlSearch().trim().toLowerCase();
    const xml = this.xmlFormateado();

    if (!term || !xml) {
      return [];
    }

    return xml
      .split('\n')
      .map((linea, index) => ({ lineNumber: index + 1, content: linea }))
      .filter((item) => item.content.toLowerCase().includes(term))
      .slice(0, 50);
  });
  protected readonly xmlSearchMatchCount = computed(() => this.xmlSearchResults().length);
  protected readonly xmlLineCount = computed(() => {
    const xml = this.xmlFormateado();
    return xml ? xml.split('\n').length : 0;
  });
  protected readonly xmlSizeLabel = computed(() => {
    const xml = this.xmlAutorizado();
    if (!xml) {
      return '0 B';
    }

    const bytes = new TextEncoder().encode(xml).length;
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    return `${(bytes / 1024).toFixed(1)} KB`;
  });

  constructor() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const uuid = params.get('id');
          if (!uuid) {
            this.error.set('No se recibio el identificador del documento.');
            this.loading.set(false);
            return of(null);
          }

          this.loading.set(true);
          this.error.set('');
          this.documento.set(null);
          this.estado.set(null);
          this.autorizacion.set(null);
          this.autorizacionError.set('');
          this.autorizacionClave.set(null);
          this.autorizacionClaveError.set('');
          this.historialItems.set([]);
          this.archivosItems.set([]);
          this.auditoriaItems.set([]);
          this.intentosSri.set(null);
          this.seguimientoCorreo.set(null);
          this.erroresItems.set([]);
          this.xmlAutorizado.set(null);
          this.xmlSearch.set('');
          this.xmlSinFirmarNombre.set('');
          this.xmlError.set('');
          this.historialError.set('');
          this.archivosError.set('');
          this.auditoriaError.set('');
          this.intentosSriError.set('');
          this.correoError.set('');
          this.correoSuccess.set('');
          this.erroresError.set('');

          return forkJoin({
            detalle: this.documentoService.obtenerDocumento(uuid).pipe(catchError(() => of(null))),
            estado: this.documentoService.obtenerEstado(uuid).pipe(catchError(() => of(null)))
          }).pipe(
            finalize(() => {
              this.loading.set(false);
            })
          );
        })
      )
      .subscribe((response) => {
        if (!response?.detalle) {
          this.error.set('No fue posible cargar el documento solicitado. Verifica el UUID o vuelve a intentarlo.');
          return;
        }

        this.documento.set(response.detalle);
        this.estado.set(response.estado);
        this.cargarHistorial(response.detalle.id);
        this.cargarArchivos(response.detalle.id);
        this.cargarAuditoria(response.detalle.id);
        this.cargarIntentosSri(response.detalle.id);
        this.cargarSeguimientoCorreo(response.detalle.id);
        this.cargarErrores(response.detalle.id);
      });
  }

  protected consultarAutorizacionManual(): void {
    const documento = this.documento();
    if (!documento?.id || this.consultandoAutorizacion()) {
      return;
    }

    this.consultandoAutorizacion.set(true);
    this.autorizacionError.set('');

    this.documentoService
      .consultarAutorizacion(documento.id)
      .pipe(
        catchError(() => {
          this.autorizacionError.set('No fue posible consultar la autorizacion manual del documento en este momento.');
          return of(null);
        }),
        finalize(() => {
          this.consultandoAutorizacion.set(false);
        })
      )
      .subscribe((resultado) => {
        if (!resultado) {
          return;
        }

        this.autorizacion.set(resultado);
      });
  }

  protected consultarAutorizacionPorClave(): void {
    const claveAcceso = this.documento()?.claveAcceso;
    if (!claveAcceso || this.consultandoPorClave()) {
      return;
    }

    this.consultandoPorClave.set(true);
    this.autorizacionClaveError.set('');

    this.documentoService
      .consultarAutorizacionPorClave(claveAcceso, false)
      .pipe(
        catchError(() => {
          this.autorizacionClaveError.set('No fue posible consultar la autorizacion por clave de acceso en este momento.');
          return of(null);
        }),
        finalize(() => {
          this.consultandoPorClave.set(false);
        })
      )
      .subscribe((resultado) => {
        if (!resultado) {
          return;
        }

        this.autorizacionClave.set(resultado);
      });
  }

  protected consultarXmlAutorizado(): void {
    const claveAcceso = this.documento()?.claveAcceso;
    if (!claveAcceso || this.consultandoXml()) {
      return;
    }

    this.consultandoXml.set(true);
    this.xmlError.set('');

    this.documentoService
      .consultarAutorizacionPorClave(claveAcceso, true)
      .pipe(
        catchError(() => {
          this.xmlError.set('No fue posible recuperar el XML autorizado en este momento.');
          return of(null);
        }),
        finalize(() => {
          this.consultandoXml.set(false);
        })
      )
      .subscribe((resultado) => {
        if (!resultado) {
          return;
        }

        this.autorizacionClave.set(resultado);
        this.xmlAutorizado.set(resultado.xmlAutorizado || null);
        if (!resultado.xmlAutorizado) {
          this.xmlError.set('La consulta respondio correctamente, pero no devolvio XML autorizado.');
        }
      });
  }

  protected copiarXmlAutorizado(): void {
    const xml = this.xmlAutorizado();
    if (!xml) {
      this.xmlError.set('No hay XML autorizado cargado para copiar.');
      return;
    }

    navigator.clipboard
      .writeText(xml)
      .then(() => {
        this.xmlError.set('');
      })
      .catch(() => {
        this.xmlError.set('No fue posible copiar el XML al portapapeles.');
      });
  }

  protected updateXmlSearch(value: string): void {
    this.xmlSearch.set(value);
  }

  protected clearXmlSearch(): void {
    this.xmlSearch.set('');
  }

  protected descargarXmlAutorizado(): void {
    const xml = this.xmlAutorizado();
    const claveAcceso = this.documento()?.claveAcceso || 'documento-autorizado';
    if (!xml) {
      this.xmlError.set('No hay XML autorizado cargado para descargar.');
      return;
    }

    const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${claveAcceso}.xml`;
    anchor.click();
    URL.revokeObjectURL(url);
    this.xmlError.set('');
  }

  protected getSRIStatusLabel(): string {
    if (this.autorizacionClave()) {
      return this.autorizacionClave()!.autorizado ? 'Autorizado por clave' : this.autorizacionClave()!.estado;
    }

    if (this.autorizacion()) {
      return this.autorizacion()!.autorizado ? 'Autorizado manualmente' : this.autorizacion()!.estado;
    }

    return 'Sin consulta ejecutada';
  }

  protected descargarArchivo(tipo: string): void {
    const documento = this.documento();
    if (!documento?.id) {
      return;
    }

    const request$ =
      tipo === 'XML_GENERADO'
        ? this.documentoService.descargarXml(documento.id)
        : tipo === 'XML_FIRMADO'
          ? this.documentoService.descargarXmlFirmado(documento.id)
          : tipo === 'XML_AUTORIZADO'
            ? this.documentoService.descargarXmlAutorizado(documento.id)
            : this.documentoService.descargarRide(documento.id);

    request$.pipe(catchError(() => of(null))).subscribe((blob) => {
      if (!blob) {
        return;
      }

      const archivo = this.archivosItems().find((item) => item.tipo === tipo);
      const extension = tipo === 'RIDE_PDF' ? 'pdf' : 'xml';
      const nombre = archivo?.nombre || `${documento.claveAcceso || documento.id}.${extension}`;
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = nombre;
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  protected reenviarCorreoDocumento(): void {
    const documento = this.documento();
    if (!documento?.id || this.reenviandoCorreo() || !this.seguimientoCorreo()?.correoConfigurado) {
      return;
    }

    this.reenviandoCorreo.set(true);
    this.correoError.set('');
    this.correoSuccess.set('');

    this.documentoService
      .reenviarCorreo(documento.id)
      .pipe(
        catchError(() => {
          this.correoError.set('No fue posible reenviar el correo del documento en este momento.');
          return of(null);
        }),
        finalize(() => {
          this.reenviandoCorreo.set(false);
        })
      )
      .subscribe((response: DocumentoCorreoReenvioResponse | null) => {
        if (!response) {
          return;
        }

        this.correoSuccess.set(response.mensaje || 'Correo reenviado correctamente.');
        this.cargarSeguimientoCorreo(documento.id);
        this.cargarHistorial(documento.id);
        this.cargarErrores(documento.id);
      });
  }

  protected reprocesarDocumento(): void {
    const documento = this.documento();
    if (!documento?.id || this.reprocesando()) {
      return;
    }

    const motivo = window.prompt('Motivo de reprocesamiento (opcional):', '') ?? '';
    this.reprocesando.set(true);

    this.documentoService
      .reprocesar(documento.id, motivo)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo reprocesar', 'El backend no pudo programar el reprocesamiento del documento.');
          return of(null);
        }),
        finalize(() => {
          this.reprocesando.set(false);
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.alerts.success('Documento programado', response.mensaje || 'El documento fue enviado a reprocesamiento.');
        this.refrescarDetalle(documento.id);
      });
  }

  protected seleccionarXmlSinFirmar(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    this.xmlSinFirmarNombre.set(file?.name ?? '');
  }

  protected cargarXmlSinFirmar(input: HTMLInputElement): void {
    const documento = this.documento();
    const file = input.files?.[0] ?? null;
    if (!documento?.id || !file || this.cargandoXmlManual()) {
      return;
    }

    const motivo = window.prompt('Motivo de la carga manual del XML (opcional):', '') ?? '';
    this.cargandoXmlManual.set(true);
    this.xmlError.set('');

    this.documentoService
      .cargarXmlSinFirmar(documento.id, file, motivo)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo procesar el XML', 'El backend no pudo firmar y enviar el XML sin firmar cargado.');
          return of(null);
        }),
        finalize(() => {
          this.cargandoXmlManual.set(false);
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        input.value = '';
        this.xmlSinFirmarNombre.set('');
        this.alerts.success('XML procesado', response.mensaje || 'El XML fue cargado y procesado correctamente.');
        this.refrescarDetalle(documento.id);
      });
  }

  protected regenerarRideDocumento(): void {
    const documento = this.documento();
    if (!documento?.id || this.regenerandoRide()) {
      return;
    }

    const motivo = window.prompt('Motivo para regenerar el RIDE (opcional):', '') ?? '';
    this.regenerandoRide.set(true);

    this.documentoService
      .regenerarRide(documento.id, motivo)
      .pipe(
        catchError(() => {
          this.alerts.error('No se pudo regenerar el RIDE', 'El backend no pudo regenerar el RIDE del documento.');
          return of(null);
        }),
        finalize(() => {
          this.regenerandoRide.set(false);
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.alerts.success('RIDE regenerado', response.mensaje || 'El RIDE fue regenerado correctamente.');
        this.refrescarDetalle(documento.id);
      });
  }

  private prettyPrintXml(xml: string | null): string {
    if (!xml) {
      return '';
    }

    try {
      const normalized = xml.replace(/>\s*</g, '><').trim();
      const parts = normalized.replace(/(>)(<)(\/*)/g, '$1\n$2$3').split('\n');
      let depth = 0;

      return parts
        .map((part) => {
          if (part.match(/^<\/.+/)) {
            depth = Math.max(depth - 1, 0);
          }

          const line = `${'  '.repeat(depth)}${part}`;

          if (part.match(/^<[^!?/][^>]*[^/]>/)) {
            depth += 1;
          }

          return line;
        })
        .join('\n');
    } catch {
      return xml;
    }
  }

  private cargarHistorial(uuid: string): void {
    this.loadingHistorial.set(true);
    this.historialError.set('');

    this.documentoService
      .obtenerHistorial(uuid)
      .pipe(
        catchError(() => {
          this.historialError.set('No fue posible cargar el historial real del documento.');
          return of([]);
        }),
        finalize(() => {
          this.loadingHistorial.set(false);
        })
      )
      .subscribe((items) => {
        this.historialItems.set(items);
      });
  }

  private refrescarDetalle(uuid: string): void {
    forkJoin({
      detalle: this.documentoService.obtenerDocumento(uuid).pipe(catchError(() => of(null))),
      estado: this.documentoService.obtenerEstado(uuid).pipe(catchError(() => of(null)))
    }).subscribe((response) => {
      if (response.detalle) {
        this.documento.set(response.detalle);
      }
      this.estado.set(response.estado);
      this.cargarHistorial(uuid);
      this.cargarArchivos(uuid);
      this.cargarAuditoria(uuid);
      this.cargarIntentosSri(uuid);
      this.cargarSeguimientoCorreo(uuid);
      this.cargarErrores(uuid);
    });
  }

  private cargarArchivos(uuid: string): void {
    this.loadingArchivos.set(true);
    this.archivosError.set('');

    this.documentoService
      .listarArchivos(uuid)
      .pipe(
        catchError(() => {
          this.archivosError.set('No fue posible cargar los archivos registrados del documento.');
          return of([]);
        }),
        finalize(() => {
          this.loadingArchivos.set(false);
        })
      )
      .subscribe((items) => {
        this.archivosItems.set(items);
      });
  }

  private cargarAuditoria(uuid: string): void {
    this.loadingAuditoria.set(true);
    this.auditoriaError.set('');

    this.documentoAuditoriaService
      .obtenerAuditoriaReciente()
      .pipe(
        catchError(() => {
          this.auditoriaError.set('No fue posible cargar la auditoria del documento.');
          return of(null);
        }),
        finalize(() => {
          this.loadingAuditoria.set(false);
        })
      )
      .subscribe((response) => {
        this.auditoriaItems.set(response?.eventos.filter((item) => item.documentoUuid === uuid) ?? []);
      });
  }

  private cargarIntentosSri(uuid: string): void {
    this.loadingIntentosSri.set(true);
    this.intentosSriError.set('');

    this.documentoService
      .obtenerIntentosSri(uuid)
      .pipe(
        catchError(() => {
          this.intentosSriError.set('No fue posible cargar los intentos SRI reales del documento.');
          return of(null);
        }),
        finalize(() => {
          this.loadingIntentosSri.set(false);
        })
      )
      .subscribe((response) => {
        this.intentosSri.set(response);
      });
  }

  private cargarSeguimientoCorreo(uuid: string): void {
    this.loadingCorreo.set(true);
    this.correoError.set('');

    this.documentoService
      .obtenerSeguimientoCorreo(uuid)
      .pipe(
        catchError(() => {
          this.correoError.set('No fue posible cargar el seguimiento real de correo del documento.');
          return of(null);
        }),
        finalize(() => {
          this.loadingCorreo.set(false);
        })
      )
      .subscribe((response) => {
        this.seguimientoCorreo.set(response);
      });
  }

  private cargarErrores(uuid: string): void {
    this.loadingErrores.set(true);
    this.erroresError.set('');

    this.documentoService
      .obtenerErrores(uuid)
      .pipe(
        catchError(() => {
          this.erroresError.set('No fue posible cargar los errores reales del documento.');
          return of([]);
        }),
        finalize(() => {
          this.loadingErrores.set(false);
        })
      )
      .subscribe((items) => {
        this.erroresItems.set(items);
      });
  }
}
