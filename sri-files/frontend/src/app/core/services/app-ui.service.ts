import { Injectable, computed, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AppUiService {
  private readonly pendingRequestsSignal = signal(0);
  private readonly sidebarCollapsedSignal = signal(false);
  private readonly mobileSidebarOpenSignal = signal(false);

  readonly loading = computed(() => this.pendingRequestsSignal() > 0);
  readonly sidebarCollapsed = computed(() => this.sidebarCollapsedSignal());
  readonly mobileSidebarOpen = computed(() => this.mobileSidebarOpenSignal());

  beginRequest(): void {
    this.pendingRequestsSignal.update((value) => value + 1);
  }

  endRequest(): void {
    this.pendingRequestsSignal.update((value) => Math.max(0, value - 1));
  }

  toggleSidebar(): void {
    if (typeof window !== 'undefined' && window.innerWidth <= 960) {
      this.mobileSidebarOpenSignal.update((value) => !value);
      return;
    }

    this.sidebarCollapsedSignal.update((value) => !value);
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpenSignal.set(false);
  }
}
