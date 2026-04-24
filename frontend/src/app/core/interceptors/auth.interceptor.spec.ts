import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let storage: Record<string, string>;

  beforeEach(() => {
    storage = {};
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: jest.fn((key: string) => storage[key] ?? null),
        setItem: jest.fn((key: string, value: string) => {
          storage[key] = value;
        }),
        removeItem: jest.fn((key: string) => {
          delete storage[key];
        }),
        clear: jest.fn(() => {
          storage = {};
        })
      },
      configurable: true
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('should add Authorization header when token exists in localStorage', () => {
    jest.spyOn(localStorage, 'getItem').mockReturnValue('token-123');

    TestBed.runInInjectionContext(() => {
      http.get('/api/wallets').subscribe();
    });

    const req = httpMock.expectOne('/api/wallets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-123');
    req.flush([]);
  });

  it('should NOT add header for /auth/ routes', () => {
    jest.spyOn(localStorage, 'getItem').mockReturnValue('token-123');

    TestBed.runInInjectionContext(() => {
      http.post('/api/auth/login', { email: 'a@b.com', password: 'x' }).subscribe();
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ token: 'abc', userId: 1, email: 'a@b.com', name: 'A' });
  });
});
