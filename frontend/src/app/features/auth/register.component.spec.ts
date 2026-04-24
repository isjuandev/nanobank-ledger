import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authServiceMock: { register: jest.Mock };
  let routerMock: { navigateByUrl: jest.Mock };

  beforeEach(async () => {
    authServiceMock = { register: jest.fn() };
    routerMock = { navigateByUrl: jest.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should not submit when form is invalid', () => {
    component.submit();

    expect(authServiceMock.register).not.toHaveBeenCalled();
  });

  it('should register and navigate to dashboard', () => {
    authServiceMock.register.mockReturnValue(of({}));
    component.form.setValue({ name: 'Nano User', email: 'user@nanobank.com', password: 'secret1' });

    component.submit();

    expect(authServiceMock.register).toHaveBeenCalledWith({
      name: 'Nano User',
      email: 'user@nanobank.com',
      password: 'secret1'
    });
    expect(component.loading()).toBe(false);
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should set error on register failure', () => {
    authServiceMock.register.mockReturnValue(throwError(() => new Error('cannot register')));
    component.form.setValue({ name: 'Nano User', email: 'user@nanobank.com', password: 'secret1' });

    component.submit();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('No fue posible registrar la cuenta');
  });
});
