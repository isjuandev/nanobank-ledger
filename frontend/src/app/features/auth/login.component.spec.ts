import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authServiceMock: { login: jest.Mock };
  let routerMock: { navigateByUrl: jest.Mock };

  beforeEach(async () => {
    authServiceMock = { login: jest.fn() };
    routerMock = { navigateByUrl: jest.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not submit when form is invalid', () => {
    component.submit();

    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('should login and navigate to dashboard', () => {
    authServiceMock.login.mockReturnValue(of({}));
    component.form.setValue({ email: 'user@nanobank.com', password: 'secret' });

    component.submit();

    expect(authServiceMock.login).toHaveBeenCalledWith({ email: 'user@nanobank.com', password: 'secret' });
    expect(component.loading()).toBe(false);
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should set error on login failure', () => {
    authServiceMock.login.mockReturnValue(throwError(() => new Error('invalid')));
    component.form.setValue({ email: 'user@nanobank.com', password: 'bad' });

    component.submit();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Credenciales inválidas');
  });
});
