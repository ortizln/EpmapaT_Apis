import { Component } from '@angular/core';
import { DocumentosTipoPageComponent } from '../documentos/documentos-tipo-page.component';

@Component({
  selector: 'app-facturacion-page',
  standalone: true,
  imports: [DocumentosTipoPageComponent],
  templateUrl: './facturacion-page.component.html'
})
export class FacturacionPageComponent {}
