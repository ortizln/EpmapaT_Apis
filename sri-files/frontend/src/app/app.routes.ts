import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { authGuard } from './core/guards/auth.guard';
import { adminLayoutRoutes } from './layout/admin-layout/admin-layout-routes';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    canActivate: [authGuard],
    children: adminLayoutRoutes
  },
  {
    path: '**',
    redirectTo: ''
  }
];
