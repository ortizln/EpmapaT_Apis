import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss'
})
export class PageHeaderComponent {
  readonly eyebrow = input('Modulo');
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly badge = input<string | null>(null);
}
