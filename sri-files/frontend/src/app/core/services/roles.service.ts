import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PermisoSistema, RolAuditoriaListadoResponse, RolSistema } from '../../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listarRoles(): Observable<RolSistema[]> {
    return this.http.get<RolSistema[]>(`${this.apiUrl}/roles`);
  }

  listarPermisos(): Observable<PermisoSistema[]> {
    return this.http.get<PermisoSistema[]>(`${this.apiUrl}/permisos`);
  }

  actualizarRol(codigo: string, payload: { nombre: string; descripcion: string; permisos: string[] }): Observable<RolSistema> {
    return this.http.put<RolSistema>(`${this.apiUrl}/roles/${codigo}`, payload);
  }

  obtenerAuditoriaReciente(page = 0, size = 10): Observable<RolAuditoriaListadoResponse> {
    return this.http.get<RolAuditoriaListadoResponse>(`${this.apiUrl}/roles/auditoria-reciente?page=${page}&size=${size}`);
  }
}
