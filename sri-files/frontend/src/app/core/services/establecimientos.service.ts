import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Establecimiento, EstablecimientoRequest } from '../../models/establecimiento.model';

@Injectable({
  providedIn: 'root'
})
export class EstablecimientosService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}`;

  listarPorEmpresa(empresaId: string): Observable<Establecimiento[]> {
    return this.http.get<Establecimiento[]>(`${this.apiUrl}/empresas/${empresaId}/establecimientos`);
  }

  crear(empresaId: string, payload: EstablecimientoRequest): Observable<Establecimiento> {
    return this.http.post<Establecimiento>(`${this.apiUrl}/empresas/${empresaId}/establecimientos`, payload);
  }

  actualizar(id: string, payload: EstablecimientoRequest): Observable<Establecimiento> {
    return this.http.put<Establecimiento>(`${this.apiUrl}/establecimientos/${id}`, payload);
  }

  actualizarEstado(id: string, activo: boolean): Observable<Establecimiento> {
    return this.http.patch<Establecimiento>(`${this.apiUrl}/establecimientos/${id}/estado`, { activo });
  }
}
