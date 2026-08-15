import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app-modal.component.html',
  styleUrl: './app-modal.component.scss'
})
export class AppModalComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly eyebrow = input<string | null>(null);
  readonly wide = input(false);
  readonly closed = output<void>();
}
