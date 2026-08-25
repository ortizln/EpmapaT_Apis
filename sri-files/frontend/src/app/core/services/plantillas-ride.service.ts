import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TipoDocumento } from '../../models/documento.model';
import {
  PlantillaRide,
  PlantillaRideCreateRequest,
  RideContratoDocumento,
  VerificacionPlantillaRideResponse
} from '../../models/plantilla-ride.model';

@Injectable({
  providedIn: 'root'
})
export class PlantillasRideService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listar(empresaId: string): Observable<PlantillaRide[]> {
    return this.http.get<PlantillaRide[]>(`${this.apiUrl}/empresas/${empresaId}/plantillas-ride`);
  }

  crear(empresaId: string, data: PlantillaRideCreateRequest, file?: File | null): Observable<PlantillaRide> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.post<PlantillaRide>(`${this.apiUrl}/empresas/${empresaId}/plantillas-ride`, formData);
  }

  actualizar(uuid: string, data: PlantillaRideCreateRequest, file?: File | null): Observable<PlantillaRide> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.put<PlantillaRide>(`${this.apiUrl}/plantillas-ride/${uuid}`, formData);
  }

  actualizarEstado(uuid: string, activa: boolean): Observable<PlantillaRide> {
    return this.http.patch<PlantillaRide>(`${this.apiUrl}/plantillas-ride/${uuid}/estado`, { activa });
  }

  verificar(uuid: string): Observable<VerificacionPlantillaRideResponse> {
    return this.http.post<VerificacionPlantillaRideResponse>(`${this.apiUrl}/plantillas-ride/${uuid}/verificar`, {});
  }

  descargarBase(tipoDocumento: TipoDocumento): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/plantillas-ride/base/${tipoDocumento}`, { responseType: 'blob' });
  }

  preview(uuid: string, documentoUuid: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/plantillas-ride/${uuid}/preview/${documentoUuid}`, { responseType: 'blob' });
  }

  contratoDocumento(documentoUuid: string): Observable<RideContratoDocumento> {
    return this.http.get<RideContratoDocumento>(`${this.apiUrl}/documentos/${documentoUuid}/ride/contrato`);
  }
}
