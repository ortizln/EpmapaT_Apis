import { HttpInterceptorFn } from '@angular/common/http';

export const requestIdInterceptor: HttpInterceptorFn = (req, next) =>
  next(
    req.clone({
      setHeaders: {
        'X-Request-Id': crypto.randomUUID()
      }
    })
  );
