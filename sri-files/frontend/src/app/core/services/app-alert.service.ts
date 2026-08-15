import { Injectable, signal } from '@angular/core';

export type AppAlertTone = 'success' | 'error' | 'warning' | 'info';

export interface AppAlert {
  id: number;
  tone: AppAlertTone;
  title: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AppAlertService {
  private nextId = 1;
  private readonly alertsSignal = signal<AppAlert[]>([]);

  readonly alerts = this.alertsSignal.asReadonly();

  show(tone: AppAlertTone, title: string, message?: string): void {
    const alert: AppAlert = {
      id: this.nextId++,
      tone,
      title,
      message
    };

    this.alertsSignal.update((value) => [...value, alert]);

    setTimeout(() => {
      this.dismiss(alert.id);
    }, 4000);
  }

  success(title: string, message?: string): void {
    this.show('success', title, message);
  }

  error(title: string, message?: string): void {
    this.show('error', title, message);
  }

  warning(title: string, message?: string): void {
    this.show('warning', title, message);
  }

  info(title: string, message?: string): void {
    this.show('info', title, message);
  }

  dismiss(id: number): void {
    this.alertsSignal.update((value) => value.filter((alert) => alert.id !== id));
  }
}
