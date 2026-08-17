import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AccessControlService } from '../auth/access-control.service';
import { AuthStore } from '../auth/auth.store';

export const permissionGuard: CanActivateFn = (route) => {
  const authStore = inject(AuthStore);
  const accessControl = inject(AccessControlService);
  const router = inject(Router);
  const requiredRole = route.data?.['role'] as string | undefined;
  const requiredPermission = route.data?.['permission'] as string | undefined;

  const usuario = authStore.usuario();

  if (requiredRole && !usuario?.roles.includes(requiredRole)) {
    return router.createUrlTree(['/dashboard']);
  }

  if (requiredPermission && !accessControl.hasPermission(requiredPermission)) {
    return router.createUrlTree(['/dashboard']);
  }

  if (!requiredRole && !requiredPermission) {
    return true;
  }

  return true;
};
