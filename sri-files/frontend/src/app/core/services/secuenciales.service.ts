import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Secuencial, SecuencialRequest } from '../../models/secuencial.model';

@Injectable({
  providedIn: 'root'
})
export class SecuencialesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}`;

  listarPorPuntoEmision(puntoEmisionId: string): Observable<Secuencial[]> {
    return this.http.get<Secuencial[]>(`${this.apiUrl}/puntos-emision/${puntoEmisionId}/secuenciales`);
  }

  actualizar(puntoEmisionId: string, tipoDocumento: string, payload: SecuencialRequest): Observable<Secuencial> {
    return this.http.put<Secuencial>(
      `${this.apiUrl}/puntos-emision/${puntoEmisionId}/secuenciales/${tipoDocumento}`,
      payload
    );
  }
}
