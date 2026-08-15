import { Component } from '@angular/core';
import { DocumentoRecepcionPageComponent } from '../documentos/documento-recepcion-page.component';

@Component({
  selector: 'app-nota-debito-form-page',
  standalone: true,
  imports: [DocumentoRecepcionPageComponent],
  templateUrl: './nota-debito-form-page.component.html'
})
export class NotaDebitoFormPageComponent {}
