import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { AppUiService } from '../services/app-ui.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const uiService = inject(AppUiService);
  uiService.beginRequest();

  return next(req).pipe(finalize(() => uiService.endRequest()));
};
