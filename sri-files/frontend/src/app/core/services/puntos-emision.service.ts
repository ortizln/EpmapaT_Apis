import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PuntoEmision, PuntoEmisionRequest } from '../../models/punto-emision.model';

@Injectable({
  providedIn: 'root'
})
export class PuntosEmisionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}`;

  listarPorEstablecimiento(establecimientoId: string): Observable<PuntoEmision[]> {
    return this.http.get<PuntoEmision[]>(`${this.apiUrl}/establecimientos/${establecimientoId}/puntos-emision`);
  }

  crear(establecimientoId: string, payload: PuntoEmisionRequest): Observable<PuntoEmision> {
    return this.http.post<PuntoEmision>(`${this.apiUrl}/establecimientos/${establecimientoId}/puntos-emision`, payload);
  }

  actualizar(id: string, payload: PuntoEmisionRequest): Observable<PuntoEmision> {
    return this.http.put<PuntoEmision>(`${this.apiUrl}/puntos-emision/${id}`, payload);
  }

  actualizarEstado(id: string, activo: boolean): Observable<PuntoEmision> {
    return this.http.patch<PuntoEmision>(`${this.apiUrl}/puntos-emision/${id}/estado`, { activo });
  }
}
