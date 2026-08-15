import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AppAlertService } from '../../../core/services/app-alert.service';

@Component({
  selector: 'app-alert-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert-center.component.html',
  styleUrl: './alert-center.component.scss'
})
export class AlertCenterComponent {
  protected readonly alerts = inject(AppAlertService);
}
