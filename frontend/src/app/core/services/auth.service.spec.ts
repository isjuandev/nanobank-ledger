import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AuthResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let storage: Record<string, string>;

  const authResponse: AuthResponse = {
    token: 'header.payload.signature',
    userId: 1,
    email: 'user@nanobank.com',
    name: 'Nano User'
  };

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
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('should store token in localStorage after successful login', () => {
    const setItemSpy = jest.spyOn(localStorage, 'setItem');

    service.login({ email: 'user@nanobank.com', password: 'secret' }).subscribe();
    const req = httpMock.expectOne('/api/auth/login');
    req.flush(authResponse);

    expect(setItemSpy).toHaveBeenCalledWith('nanobank_token', 'header.payload.signature');
    expect(setItemSpy).toHaveBeenCalledWith(
      'nanobank_user',
      JSON.stringify({ id: 1, email: 'user@nanobank.com', name: 'Nano User' })
    );
  });

  it('should update currentUser signal after login', () => {
    service.login({ email: 'user@nanobank.com', password: 'secret' }).subscribe();
    const req = httpMock.expectOne('/api/auth/login');
    req.flush(authResponse);

    expect(service.currentUser()).toEqual({
      id: 1,
      email: 'user@nanobank.com',
      name: 'Nano User'
    });
  });

  it('should clear token and signal on logout', () => {
    service.login({ email: 'user@nanobank.com', password: 'secret' }).subscribe();
    httpMock.expectOne('/api/auth/login').flush(authResponse);

    const removeSpy = jest.spyOn(localStorage, 'removeItem');
    service.logout();

    expect(removeSpy).toHaveBeenCalledWith('nanobank_token');
    expect(removeSpy).toHaveBeenCalledWith('nanobank_user');
    expect(service.currentUser()).toBeNull();
  });

  it('isAuthenticated should return true when token exists', () => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
    jest.spyOn(localStorage, 'getItem').mockReturnValue(`header.${payload}.signature`);

    const result = TestBed.runInInjectionContext(() => service.isAuthenticated());

    expect(result).toBe(true);
  });

  it('isAuthenticated should return false when token does not exist', () => {
    jest.spyOn(localStorage, 'getItem').mockReturnValue(null);

    const result = TestBed.runInInjectionContext(() => service.isAuthenticated());

    expect(result).toBe(false);
  });

  it('isAuthenticated should return false when token is expired', () => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 3600 }));
    jest.spyOn(localStorage, 'getItem').mockReturnValue(`header.${payload}.signature`);

    const result = TestBed.runInInjectionContext(() => service.isAuthenticated());

    expect(result).toBe(false);
  });

  it('should call register endpoint and persist session', () => {
    const setItemSpy = jest.spyOn(localStorage, 'setItem');

    service.register({ email: 'new@nanobank.com', password: 'secret', name: 'New User' }).subscribe();
    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({
      token: 'register-token',
      userId: 2,
      email: 'new@nanobank.com',
      name: 'New User'
    } as AuthResponse);

    expect(setItemSpy).toHaveBeenCalledWith('nanobank_token', 'register-token');
    expect(setItemSpy).toHaveBeenCalledWith(
      'nanobank_user',
      JSON.stringify({ id: 2, email: 'new@nanobank.com', name: 'New User' })
    );
    expect(service.currentUser()).toEqual({
      id: 2,
      email: 'new@nanobank.com',
      name: 'New User'
    });
  });

  it('should hydrate currentUser from localStorage on init', () => {
    storage['nanobank_user'] = JSON.stringify({
      id: 9,
      email: 'cached@nanobank.com',
      name: 'Cached User'
    });

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const hydratedService = TestBed.inject(AuthService);

    expect(hydratedService.currentUser()).toEqual({
      id: 9,
      email: 'cached@nanobank.com',
      name: 'Cached User'
    });
  });
});
