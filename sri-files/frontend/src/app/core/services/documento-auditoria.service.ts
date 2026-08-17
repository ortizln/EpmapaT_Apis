import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentoAuditoriaResumenResponse } from '../../models/documento.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentoAuditoriaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/documentos`;

  obtenerAuditoriaReciente(): Observable<DocumentoAuditoriaResumenResponse> {
    return this.http.get<DocumentoAuditoriaResumenResponse>(`${this.apiUrl}/auditoria`);
  }
}
