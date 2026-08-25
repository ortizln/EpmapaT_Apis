import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  EmpresaAuditoriaListadoResponse,
  CorreoConfiguracion,
  CorreoConfiguracionRequest,
  Empresa,
  EmpresaListadoResponse,
  EmpresaConfiguracion,
  EmpresaConfiguracionRequest,
  EmpresaRequest,
  SriConfiguracion,
  SriConfiguracionRequest
} from '../../models/empresa.model';

@Injectable({
  providedIn: 'root'
})
export class EmpresasService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/empresas`;

  listar(): Observable<Empresa[]> {
    return this.http
      .get<EmpresaListadoResponse | Empresa[] | null>(`${this.apiUrl}?page=0&size=200`)
      .pipe(
        map((response) => {
          if (Array.isArray(response)) {
            return response;
          }

          return response?.items ?? [];
        })
      );
  }

  listarPaginado(page = 0, size = 10): Observable<EmpresaListadoResponse> {
    return this.http.get<EmpresaListadoResponse>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  crear(payload: EmpresaRequest): Observable<Empresa> {
    return this.http.post<Empresa>(this.apiUrl, payload);
  }

  actualizar(id: string, payload: EmpresaRequest): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.apiUrl}/${id}`, payload);
  }

  actualizarEstado(id: string, activo: boolean): Observable<Empresa> {
    return this.http.patch<Empresa>(`${this.apiUrl}/${id}/estado`, { activo });
  }

  obtenerConfiguracion(id: string): Observable<EmpresaConfiguracion> {
    return this.http.get<EmpresaConfiguracion>(`${this.apiUrl}/${id}/configuracion`);
  }

  actualizarConfiguracion(id: string, payload: EmpresaConfiguracionRequest): Observable<EmpresaConfiguracion> {
    return this.http.put<EmpresaConfiguracion>(`${this.apiUrl}/${id}/configuracion`, payload);
  }

  obtenerConfiguracionSri(id: string): Observable<SriConfiguracion> {
    return this.http.get<SriConfiguracion>(`${this.apiUrl}/${id}/configuracion-sri`);
  }

  actualizarConfiguracionSri(id: string, payload: SriConfiguracionRequest): Observable<SriConfiguracion> {
    return this.http.put<SriConfiguracion>(`${this.apiUrl}/${id}/configuracion-sri`, payload);
  }

  obtenerConfiguracionCorreo(id: string): Observable<CorreoConfiguracion> {
    return this.http.get<CorreoConfiguracion>(`${this.apiUrl}/${id}/configuracion-correo`);
  }

  actualizarConfiguracionCorreo(id: string, payload: CorreoConfiguracionRequest): Observable<CorreoConfiguracion> {
    return this.http.put<CorreoConfiguracion>(`${this.apiUrl}/${id}/configuracion-correo`, payload);
  }

  obtenerAuditoriaReciente(page = 0, size = 10): Observable<EmpresaAuditoriaListadoResponse> {
    return this.http.get<EmpresaAuditoriaListadoResponse>(`${this.apiUrl}/auditoria-reciente?page=${page}&size=${size}`);
  }
}
