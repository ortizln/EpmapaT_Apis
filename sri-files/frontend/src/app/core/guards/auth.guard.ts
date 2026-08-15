import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth.store';
import { TokenService } from '../auth/token.service';

export const authGuard: CanActivateFn = () => {
  const tokenService = inject(TokenService);
  const authStore = inject(AuthStore);
  const router = inject(Router);

  if (tokenService.getToken() && authStore.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
