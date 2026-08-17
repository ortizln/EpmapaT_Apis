import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MonitorHealthResponse, MonitorResumen } from '../../models/monitor.model';
import { CorreoPendienteResponse, MonitorPendientesResponse } from '../../models/monitor-queue.model';

@Injectable({
  providedIn: 'root'
})
export class MonitorService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/monitoreo`;

  obtenerHealth(): Observable<MonitorHealthResponse> {
    return this.http.get<MonitorHealthResponse>(`${this.apiUrl}/health`);
  }

  obtenerResumen(): Observable<MonitorResumen> {
    return this.http.get<MonitorResumen>(`${this.apiUrl}/resumen`);
  }

  obtenerPendientes(): Observable<MonitorPendientesResponse> {
    return this.http.get<MonitorPendientesResponse>(`${this.apiUrl}/pendientes`);
  }

  obtenerCorreosPendientes(): Observable<CorreoPendienteResponse> {
    return this.http.get<CorreoPendienteResponse>(`${this.apiUrl}/correos`);
  }
}
