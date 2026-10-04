import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TimeoutError } from 'rxjs';
import { App } from './app';
import { CardType, Currency, UserRole } from './enums';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App, HttpClientTestingModule],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the sign-in screen', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h2')?.textContent).toContain('Welcome back');
    expect(compiled.textContent).toContain('Login');
  });

  it('should switch to login after successful registration', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);

    app.authMode = 'register';
    app.registerForm = {
      username: 'alice',
      email: 'alice@example.com',
      password: 'securepass',
      requestAdminAccess: false,
      adminAccessReason: '',
    };

    app.submitRegistration();

    const req = http.expectOne('http://localhost:8082/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      username: 'alice',
      email: 'alice@example.com',
      password: 'securepass',
      requestAdminAccess: false,
      adminAccessReason: '',
    });
    req.flush({}, { headers: undefined, status: 200, statusText: 'OK' });

    expect(app.authMode).toBe('login');
    expect(app.message).toContain('Your account is ready');
  });

  it('should submit an admin access request as part of normal user registration', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);

    app.registerForm = {
      username: 'alice',
      email: 'alice@example.com',
      password: 'securepass',
      requestAdminAccess: true,
      adminAccessReason: 'I manage user onboarding',
    };
    app.submitRegistration();

    const req = http.expectOne('http://localhost:8082/api/v1/auth/register');
    expect(req.request.body).toEqual({
      username: 'alice',
      email: 'alice@example.com',
      password: 'securepass',
      requestAdminAccess: true,
      adminAccessReason: 'I manage user onboarding',
    });
    req.flush({ message: 'Your administrator access request is pending review.' });

    expect(app.message).toContain('pending review');
    expect(app.registerForm.requestAdminAccess).toBe(false);
  });

  it('should submit an administrator access request from the signed-in profile', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);

    app.token = 'access-token';
    app.adminAccessReason = 'I manage user onboarding';
    app.submitAdminAccessRequest();

    const req = http.expectOne('http://localhost:8082/api/v1/users/me/admin-access-requests');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'I manage user onboarding' });
    expect(req.request.headers.get('Authorization')).toBe('Bearer access-token');
    req.flush({
      data: {
        id: 21,
        userId: 7,
        username: 'alice',
        email: 'alice@example.com',
        reason: 'I manage user onboarding',
        status: 'PENDING',
        requestedAt: '2026-10-04T00:00:00Z',
      },
      message: 'Administrator access request submitted',
    });

    expect(app.myAdminAccessRequest?.status).toBe('PENDING');
    expect(app.adminAccessReason).toBe('');
  });

  it('should stop the login loading state when the server rejects credentials', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);

    app.loginForm = { usernameOrEmail: 'alice@example.com', password: 'wrong-password' };
    app.submitLogin();

    const req = http.expectOne('http://localhost:8082/api/v1/auth/login');
    req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    fixture.detectChanges();
    expect(app.loading).toBe(false);
    expect(app.error).toBe('Invalid credentials');
    expect(fixture.nativeElement.querySelector('form button[type="submit"]').disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('form button[type="submit"]').textContent).toContain('Login');
  });

  it('should populate profile fields from a successful login response', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);

    app.loginForm = { usernameOrEmail: 'alice@example.com', password: 'securepass' };
    app.submitLogin();

    http.expectOne('http://localhost:8082/api/v1/auth/login').flush({
      data: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        id: 7,
        username: 'alice',
        email: 'alice@example.com',
        roles: [UserRole.User],
      },
    });
    http.expectOne('http://localhost:8083/api/v1/payments').flush([]);

    expect(app.profileForm).toEqual({ username: 'alice', email: 'alice@example.com' });
  });

  it('should preserve the session when the profile service is temporarily unavailable', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    app.token = 'access-token';
    app.profile = {
      id: 7,
      username: 'alice',
      email: 'alice@example.com',
      roles: [UserRole.User],
      enabled: true,
    };

    app.loadProfile();
    http.expectOne('http://localhost:8082/api/v1/users/me')
      .flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(app.token).toBe('access-token');
    expect(app.profile?.username).toBe('alice');
    expect(app.error).toContain('503');
  });

  it('should clear the session when the profile service rejects the access token', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    app.token = 'expired-token';
    app.profile = {
      id: 7,
      username: 'alice',
      email: 'alice@example.com',
      roles: [UserRole.User],
      enabled: true,
    };

    app.loadProfile();
    http.expectOne('http://localhost:8082/api/v1/users/me')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(app.token).toBe('');
    expect(app.profile).toBeNull();
  });

  it('should report server logout failures after clearing the local session', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    sessionStorage.setItem('globePayRefreshToken', 'refresh-token');
    app.token = 'access-token';
    app.signOut();

    const request = http.expectOne('http://localhost:8082/api/v1/auth/logout');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-token' });
    expect(app.token).toBe('');
    request.flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(app.error).toContain('server logout could not be confirmed');
  });

  it('should update the amount symbol when the selected currency changes', () => {
    const app = TestBed.createComponent(App).componentInstance;

    app.paymentForm.currency = Currency.INR;
    expect(app.currencySymbol).toBe('₹');

    app.paymentForm.currency = Currency.EUR;
    expect(app.currencySymbol).toBe('€');
  });

  it('should load account analytics and payments when opening Analytics', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    app.token = 'access-token';

    app.selectView('analytics');

    expect(app.view).toBe('analytics');
    http.expectOne('http://localhost:8083/api/v1/payments').flush([]);
    http.expectOne('http://localhost:8083/api/v1/payments/analytics').flush({
      totalTransactions: 4,
      completedTransactions: 2,
      failedTransactions: 1,
      refundedTransactions: 1,
      authorizedTransactions: 0,
      successRatePercentage: 50,
      declineRatePercentage: 25,
      totalProcessedVolume: 125.5,
      primaryCurrency: 'USD',
      statusBreakdown: { COMPLETED: 2, FAILED: 1, REFUNDED: 1 },
    });

    expect(app.paymentAnalytics?.totalTransactions).toBe(4);
    expect(app.paymentAnalytics?.primaryCurrency).toBe('USD');
  });

  it('should open transaction details using the payment service response', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    app.token = 'access-token';
    const payment = {
      transactionId: 'tx-detail',
      userId: 7,
      cardHolderName: 'Alice Example',
      cardLastFour: '1111',
      amount: 125.5,
      currency: Currency.USD,
      status: 'COMPLETED',
      description: 'Invoice payment',
      cardType: CardType.Debit,
      createdAt: '2026-10-04T00:00:00Z',
    };

    app.openTransaction(payment);

    const request = http.expectOne('http://localhost:8083/api/v1/payments/tx-detail');
    expect(request.request.headers.has('Authorization')).toBe(true);
    request.flush({ ...payment, authCode: 'AUTH-123' });

    expect(app.view).toBe('transaction-detail');
    expect(app.selectedPayment?.authCode).toBe('AUTH-123');
  });

  it('should tokenize a card then submit the payment with the selected currency', () => {
    const app = TestBed.createComponent(App).componentInstance;
    const http = TestBed.inject(HttpTestingController);
    app.token = 'access-token';
    app.paymentForm = {
      cardType: CardType.Debit,
      cardHolderName: 'Alice Example',
      cardNumber: '4111 1111 1111 1111',
      expiryMonth: '12',
      expiryYear: '2030',
      cvv: '123',
      amount: '250',
      currency: Currency.INR,
      description: 'Test payment',
    };

    app.submitPayment();

    const tokenizeRequest = http.expectOne('http://localhost:8083/api/v1/payments/tokenize');
    expect(tokenizeRequest.request.headers.get('Authorization')).toMatch(/^Bearer /);
    expect(tokenizeRequest.request.body.cardNumber).toBe('4111111111111111');
    tokenizeRequest.flush({ cardToken: 'tok_test', cardLastFour: '1111' });

    const paymentRequest = http.expectOne('http://localhost:8083/api/v1/payments');
    expect(paymentRequest.request.body).toEqual({
      cardType: CardType.Debit,
      cardToken: 'tok_test',
      cardHolderName: 'Alice Example',
      amount: 250,
      currency: Currency.INR,
      description: 'Test payment',
      threeDSecureRequired: true,
    });
    paymentRequest.flush({
      transactionId: 'tx-test',
      userId: 7,
      cardHolderName: 'Alice Example',
      cardLastFour: '1111',
      cardType: CardType.Debit,
      amount: 250,
      currency: Currency.INR,
      status: 'INITIATED',
      description: 'Test payment',
      createdAt: '2026-10-04T00:00:00Z',
    });

    expect(app.loading).toBe(false);
    expect(app.payments[0].transactionId).toBe('tx-test');
    expect(app.paymentForm.currency).toBe(Currency.USD);
  });

  it('should surface a timeout message instead of leaving the button stuck in loading', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    const timeoutError = new Error('Request timed out') as Error & { name: string };
    timeoutError.name = 'TimeoutError';

    const message = app['errorMessage'](timeoutError);

    expect(message).toContain('taking too long');
  });
});
