import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  Empresa,
  EmpresaListadoResponse,
  EmpresaConfiguracion,
  EmpresaConfiguracionRequest,
  EmpresaRequest
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
}
