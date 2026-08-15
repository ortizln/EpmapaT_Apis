import { Component } from '@angular/core';
import { DocumentosTipoPageComponent } from '../documentos/documentos-tipo-page.component';

@Component({
  selector: 'app-retenciones-page',
  standalone: true,
  imports: [DocumentosTipoPageComponent],
  templateUrl: './retenciones-page.component.html'
})
export class RetencionesPageComponent {}
