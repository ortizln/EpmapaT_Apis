import { Component } from '@angular/core';
import { DocumentoRecepcionPageComponent } from '../documentos/documento-recepcion-page.component';

@Component({
  selector: 'app-retencion-form-page',
  standalone: true,
  imports: [DocumentoRecepcionPageComponent],
  templateUrl: './retencion-form-page.component.html'
})
export class RetencionFormPageComponent {}
