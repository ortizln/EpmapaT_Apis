import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth.store';

export const permissionGuard: CanActivateFn = (route) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const requiredRole = route.data?.['role'] as string | undefined;

  if (!requiredRole) {
    return true;
  }

  const usuario = authStore.usuario();
  if (usuario?.roles.includes(requiredRole)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
