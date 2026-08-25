import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DocumentoContrato,
  DocumentoAutorizacionConsultaResponse,
  DocumentoAutorizacionManualResponse,
  DocumentoCorreoReenvioResponse,
  DocumentoCorreoSeguimientoResponse,
  DocumentoArchivoItemResponse,
  DocumentoOperacionManualResponse,
  DocumentoResumenOperativo,
  DocumentoDetalleResponse,
  DocumentoErrorItemResponse,
  DocumentoEstadoResponse,
  DocumentoHistorialItemResponse,
  DocumentoIntentoSriResponse,
  DocumentoListadoResponse,
  DocumentoRecepcionRequest,
  DocumentoRecepcionResponse,
  TipoDocumento
} from '../../models/documento.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentoContratoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/documentos`;

  obtenerContrato(tipoDocumento: TipoDocumento): Observable<DocumentoContrato> {
    return this.http.get<DocumentoContrato>(`${this.apiUrl}/contratos/${tipoDocumento}`);
  }

  recibirDocumento(payload: DocumentoRecepcionRequest): Observable<DocumentoRecepcionResponse> {
    return this.http.post<DocumentoRecepcionResponse>(this.apiUrl, payload);
  }

  obtenerDocumento(uuid: string): Observable<DocumentoDetalleResponse> {
    return this.http.get<DocumentoDetalleResponse>(`${this.apiUrl}/${uuid}`);
  }

  obtenerEstado(uuid: string): Observable<DocumentoEstadoResponse> {
    return this.http.get<DocumentoEstadoResponse>(`${this.apiUrl}/${uuid}/estado`);
  }

  consultarAutorizacion(uuid: string): Observable<DocumentoAutorizacionManualResponse> {
    return this.http.post<DocumentoAutorizacionManualResponse>(`${this.apiUrl}/${uuid}/consultar-autorizacion`, {});
  }

  consultarAutorizacionPorClave(claveAcceso: string, incluirXml = false): Observable<DocumentoAutorizacionConsultaResponse> {
    const queryParams = new URLSearchParams();
    queryParams.set('claveAcceso', claveAcceso);
    queryParams.set('incluirXml', String(incluirXml));

    return this.http.get<DocumentoAutorizacionConsultaResponse>(`${this.apiUrl}/autorizacion?${queryParams.toString()}`);
  }

  obtenerHistorial(uuid: string): Observable<DocumentoHistorialItemResponse[]> {
    return this.http.get<DocumentoHistorialItemResponse[]>(`${this.apiUrl}/${uuid}/historial`);
  }

  listarArchivos(uuid: string): Observable<DocumentoArchivoItemResponse[]> {
    return this.http.get<DocumentoArchivoItemResponse[]>(`${this.apiUrl}/${uuid}/archivos`);
  }

  obtenerErrores(uuid: string): Observable<DocumentoErrorItemResponse[]> {
    return this.http.get<DocumentoErrorItemResponse[]>(`${this.apiUrl}/${uuid}/errores`);
  }

  obtenerIntentosSri(uuid: string): Observable<DocumentoIntentoSriResponse> {
    return this.http.get<DocumentoIntentoSriResponse>(`${this.apiUrl}/${uuid}/intentos-sri`);
  }

  obtenerSeguimientoCorreo(uuid: string): Observable<DocumentoCorreoSeguimientoResponse> {
    return this.http.get<DocumentoCorreoSeguimientoResponse>(`${this.apiUrl}/${uuid}/correo`);
  }

  reenviarCorreo(uuid: string): Observable<DocumentoCorreoReenvioResponse> {
    return this.http.post<DocumentoCorreoReenvioResponse>(`${this.apiUrl}/${uuid}/reenviar-correo`, {});
  }

  reprocesar(uuid: string, motivo?: string): Observable<DocumentoOperacionManualResponse> {
    return this.http.post<DocumentoOperacionManualResponse>(`${this.apiUrl}/${uuid}/reprocesar`, {
      motivo: motivo?.trim() || null
    });
  }

  cargarXmlSinFirmar(uuid: string, file: File, motivo?: string): Observable<DocumentoOperacionManualResponse> {
    const formData = new FormData();
    formData.append('xml', file);

    let url = `${this.apiUrl}/${uuid}/xml-sin-firmar`;
    if (motivo?.trim()) {
      const queryParams = new URLSearchParams();
      queryParams.set('motivo', motivo.trim());
      url = `${url}?${queryParams.toString()}`;
    }

    return this.http.post<DocumentoOperacionManualResponse>(url, formData);
  }

  regenerarRide(uuid: string, motivo?: string): Observable<DocumentoOperacionManualResponse> {
    return this.http.post<DocumentoOperacionManualResponse>(`${this.apiUrl}/${uuid}/regenerar-ride`, {
      motivo: motivo?.trim() || null
    });
  }

  descargarXml(uuid: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${uuid}/xml`, { responseType: 'blob' });
  }

  descargarXmlFirmado(uuid: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${uuid}/xml-firmado`, { responseType: 'blob' });
  }

  descargarXmlAutorizado(uuid: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${uuid}/xml-autorizado`, { responseType: 'blob' });
  }

  descargarRide(uuid: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${uuid}/ride`, { responseType: 'blob' });
  }

  listarDocumentos(params: {
    empresaUuid?: string;
    tipoDocumento?: string;
    estado?: string;
    busqueda?: string;
    page?: number;
    size?: number;
  }): Observable<DocumentoListadoResponse> {
    const queryParams = new URLSearchParams();

    if (params.empresaUuid) {
      queryParams.set('empresaUuid', params.empresaUuid);
    }
    if (params.tipoDocumento) {
      queryParams.set('tipoDocumento', params.tipoDocumento);
    }
    if (params.estado) {
      queryParams.set('estado', params.estado);
    }
    if (params.busqueda) {
      queryParams.set('busqueda', params.busqueda);
    }

    queryParams.set('page', String(params.page ?? 0));
    queryParams.set('size', String(params.size ?? 10));

    return this.http.get<DocumentoListadoResponse>(`${this.apiUrl}?${queryParams.toString()}`);
  }

  buscarDocumentos(q: string, page = 0, size = 10): Observable<DocumentoListadoResponse> {
    const queryParams = new URLSearchParams();
    queryParams.set('q', q);
    queryParams.set('page', String(page));
    queryParams.set('size', String(size));

    return this.http.get<DocumentoListadoResponse>(`${this.apiUrl}/search?${queryParams.toString()}`);
  }

  exportarDocumentos(params: {
    empresaUuid?: string;
    tipoDocumento?: string;
    estado?: string;
    busqueda?: string;
  }): Observable<Blob> {
    const queryParams = new URLSearchParams();

    if (params.empresaUuid) {
      queryParams.set('empresaUuid', params.empresaUuid);
    }
    if (params.tipoDocumento) {
      queryParams.set('tipoDocumento', params.tipoDocumento);
    }
    if (params.estado) {
      queryParams.set('estado', params.estado);
    }
    if (params.busqueda) {
      queryParams.set('busqueda', params.busqueda);
    }

    return this.http.get(`${this.apiUrl}/export?${queryParams.toString()}`, {
      responseType: 'blob'
    });
  }

  obtenerResumenOperativo(): Observable<DocumentoResumenOperativo> {
    return this.http.get<DocumentoResumenOperativo>(`${this.apiUrl}/resumen`);
  }
}
