import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { ReactiveFormsModule, UntypedFormGroup } from '@angular/forms';
import { DocumentoDetalleResponse, DocumentoEstadoResponse } from '../../models/documento.model';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-documento-seguimiento',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StatusChipComponent],
  templateUrl: './documento-seguimiento.component.html',
  styleUrl: './documento-seguimiento.component.scss'
})
export class DocumentoSeguimientoComponent {
  readonly form = input.required<UntypedFormGroup>();
  readonly detalle = input<DocumentoDetalleResponse | null>(null);
  readonly estado = input<DocumentoEstadoResponse | null>(null);
  readonly loading = input(false);
  readonly error = input('');
  readonly consultar = output<void>();
}
