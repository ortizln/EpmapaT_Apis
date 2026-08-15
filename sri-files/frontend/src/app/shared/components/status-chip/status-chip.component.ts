import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  templateUrl: './status-chip.component.html',
  styleUrl: './status-chip.component.scss'
})
export class StatusChipComponent {
  readonly label = input.required<string>();
  readonly isOk = computed(() => ['AUTORIZADO', 'ACTIVO', 'ENVIADO'].includes(this.label().toUpperCase()));
}
