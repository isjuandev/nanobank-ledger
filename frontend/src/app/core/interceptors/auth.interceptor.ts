import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isAuthRoute = req.url.includes('/auth/');
  if (isAuthRoute) {
    return next(req);
  }

  const token = localStorage.getItem('nanobank_token');
  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};
