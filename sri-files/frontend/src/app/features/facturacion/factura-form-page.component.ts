import { Component } from '@angular/core';
import { DocumentoRecepcionPageComponent } from '../documentos/documento-recepcion-page.component';

@Component({
  selector: 'app-factura-form-page',
  standalone: true,
  imports: [DocumentoRecepcionPageComponent],
  templateUrl: './factura-form-page.component.html'
})
export class FacturaFormPageComponent {}
