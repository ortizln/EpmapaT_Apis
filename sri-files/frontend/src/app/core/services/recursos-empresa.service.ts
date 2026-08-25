import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RecursoEmpresa, RecursoEmpresaTipo } from '../../models/recurso-empresa.model';

@Injectable({
  providedIn: 'root'
})
export class RecursosEmpresaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listar(empresaId: string): Observable<RecursoEmpresa[]> {
    return this.http.get<RecursoEmpresa[]>(`${this.apiUrl}/empresas/${empresaId}/recursos`);
  }

  crear(empresaId: string, tipo: RecursoEmpresaTipo, nombre: string, file: File): Observable<RecursoEmpresa> {
    const formData = new FormData();
    formData.append('tipo', tipo);
    formData.append('nombre', nombre);
    formData.append('file', file);
    return this.http.post<RecursoEmpresa>(`${this.apiUrl}/empresas/${empresaId}/recursos`, formData);
  }

  actualizarEstado(uuid: string, activo: boolean): Observable<RecursoEmpresa> {
    return this.http.patch<RecursoEmpresa>(`${this.apiUrl}/recursos/${uuid}/estado`, { activo });
  }
}
