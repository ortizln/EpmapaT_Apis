import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DashboardDocumentoDia,
  DashboardDocumentoEstado,
  DashboardDocumentoTipo,
  DashboardErrorEtapa,
  DashboardResumen,
  DashboardSnapshot,
  DashboardTiempos
} from '../../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  obtenerResumen(empresaUuid?: string): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(this.withEmpresa(`${this.apiUrl}/resumen`, empresaUuid));
  }

  obtenerDocumentosPorTipo(empresaUuid?: string): Observable<DashboardDocumentoTipo[]> {
    return this.http.get<DashboardDocumentoTipo[]>(this.withEmpresa(`${this.apiUrl}/documentos-por-tipo`, empresaUuid));
  }

  obtenerDocumentosPorEstado(empresaUuid?: string): Observable<DashboardDocumentoEstado[]> {
    return this.http.get<DashboardDocumentoEstado[]>(this.withEmpresa(`${this.apiUrl}/documentos-por-estado`, empresaUuid));
  }

  obtenerDocumentosPorDia(empresaUuid?: string): Observable<DashboardDocumentoDia[]> {
    return this.http.get<DashboardDocumentoDia[]>(this.withEmpresa(`${this.apiUrl}/documentos-por-dia`, empresaUuid));
  }

  obtenerErroresPorEtapa(empresaUuid?: string): Observable<DashboardErrorEtapa[]> {
    return this.http.get<DashboardErrorEtapa[]>(this.withEmpresa(`${this.apiUrl}/errores-por-etapa`, empresaUuid));
  }

  obtenerTiempos(empresaUuid?: string): Observable<DashboardTiempos> {
    return this.http.get<DashboardTiempos>(this.withEmpresa(`${this.apiUrl}/tiempos`, empresaUuid));
  }

  obtenerSnapshot(empresaUuid?: string): Observable<DashboardSnapshot> {
    return forkJoin({
      resumen: this.obtenerResumen(empresaUuid),
      porTipo: this.obtenerDocumentosPorTipo(empresaUuid),
      porEstado: this.obtenerDocumentosPorEstado(empresaUuid),
      porDia: this.obtenerDocumentosPorDia(empresaUuid),
      erroresPorEtapa: this.obtenerErroresPorEtapa(empresaUuid),
      tiempos: this.obtenerTiempos(empresaUuid)
    });
  }

  private withEmpresa(url: string, empresaUuid?: string): string {
    if (!empresaUuid) {
      return url;
    }

    const queryParams = new URLSearchParams();
    queryParams.set('empresaUuid', empresaUuid);
    return `${url}?${queryParams.toString()}`;
  }
}
