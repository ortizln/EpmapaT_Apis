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

  obtenerResumenOperativo(): Observable<DocumentoResumenOperativo> {
    return this.http.get<DocumentoResumenOperativo>(`${this.apiUrl}/resumen`);
  }
}
