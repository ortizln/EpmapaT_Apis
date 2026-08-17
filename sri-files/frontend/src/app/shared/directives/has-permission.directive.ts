import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { AccessControlService } from '../../core/auth/access-control.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly accessControl = inject(AccessControlService);

  readonly appHasPermission = input<string | string[]>('');
  readonly appHasPermissionMode = input<'all' | 'any'>('all');

  constructor() {
    effect(() => {
      const requirement = this.appHasPermission();
      const mode = this.appHasPermissionMode();
      const permissions = Array.isArray(requirement) ? requirement.filter(Boolean) : [requirement].filter(Boolean);
      const allowed =
        permissions.length === 0
          ? true
          : mode === 'any'
            ? this.accessControl.hasAnyPermission(permissions)
            : permissions.every((permission) => this.accessControl.hasPermission(permission));

      this.viewContainer.clear();
      if (allowed) {
        this.viewContainer.createEmbeddedView(this.templateRef);
      }
    });
  }
}
