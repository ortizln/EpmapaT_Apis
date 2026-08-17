import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UsuarioAuditoria, UsuarioAuditoriaListadoResponse, UsuarioCrearRequest, UsuarioSistema, UsuarioSistemaListadoResponse } from '../../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/usuarios`;

  listar(page = 0, size = 10): Observable<UsuarioSistemaListadoResponse> {
    return this.http.get<UsuarioSistemaListadoResponse>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  crear(payload: UsuarioCrearRequest): Observable<UsuarioSistema> {
    return this.http.post<UsuarioSistema>(this.apiUrl, payload);
  }

  actualizarEstado(id: string, activo: boolean): Observable<UsuarioSistema> {
    return this.http.patch<UsuarioSistema>(`${this.apiUrl}/${id}/estado`, { activo });
  }

  resetearPassword(id: string, password: string): Observable<UsuarioSistema> {
    return this.http.patch<UsuarioSistema>(`${this.apiUrl}/${id}/password`, { password });
  }

  obtenerAuditoria(id: string): Observable<UsuarioAuditoria[]> {
    return this.http.get<UsuarioAuditoria[]>(`${this.apiUrl}/${id}/auditoria`);
  }

  obtenerAuditoriaReciente(page = 0, size = 10): Observable<UsuarioAuditoriaListadoResponse> {
    return this.http.get<UsuarioAuditoriaListadoResponse>(`${this.apiUrl}/auditoria-reciente?page=${page}&size=${size}`);
  }
}
