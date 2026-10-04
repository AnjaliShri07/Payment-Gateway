import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, switchMap, timeout, TimeoutError } from 'rxjs';
import { CardType, Currency, UserRole } from './enums';

type WorkspaceView = 'overview' | 'analytics' | 'payments' | 'transaction-detail' | 'profile' | 'security' | 'admin';

interface ApiEnvelope<T> {
  data: T;
  message?: string;
}

interface AuthSession {
  accessToken: string;
  refreshToken: string;
  id: number;
  username: string;
  email: string;
  roles: UserRole[];
}

interface UserProfile {
  id: number;
  username: string;
  email: string;
  roles: UserRole[];
  enabled: boolean;
  createdAt?: string;
}

interface Payment {
  transactionId: string;
  userId: number;
  cardHolderName: string;
  cardLastFour: string;
  amount: number;
  currency: Currency;
  status: string;
  description: string;
  cardType: CardType;
  createdAt: string;
  processedAt?: string;
  refundedAt?: string;
  cardProvider?: string;
  authCode?: string;
  acquirerReferenceNumber?: string;
  failureReason?: string;
  refundAmount?: number;
  refundReason?: string;
}

interface PaymentAnalytics {
  totalTransactions: number;
  completedTransactions: number;
  failedTransactions: number;
  refundedTransactions: number;
  authorizedTransactions: number;
  successRatePercentage: number;
  declineRatePercentage: number;
  totalProcessedVolume: number;
  primaryCurrency: string;
  statusBreakdown: Record<string, number>;
}

interface PaymentToken {
  cardToken: string;
  cardLastFour: string;
}

interface AdminUser extends UserProfile {}

interface AdminAccessRequest {
  id: number;
  userId: number;
  username: string;
  email: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
  decisionNote?: string;
}

const AUTH_API = 'http://localhost:8082/api/v1';
const USER_API = 'http://localhost:8081/api/v1';
const PAYMENT_API = 'http://localhost:8083/api/v1';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  templateUrl: './dashboard.html',
})
export class App implements OnInit {
  readonly cardTypes = Object.values(CardType);
  readonly currencies = Object.values(Currency);

  view: WorkspaceView = 'overview';
  authMode: 'login' | 'register' = 'login';
  token = '';
  profile: UserProfile | null = null;
  payments: Payment[] = [];
  paymentAnalytics: PaymentAnalytics | null = null;
  selectedPayment: Payment | null = null;
  adminUsers: AdminUser[] = [];
  adminAccessRequests: AdminAccessRequest[] = [];
  myAdminAccessRequest: AdminAccessRequest | null = null;
  userSearch = '';
  loading = false;
  message = '';
  error = '';

  loginForm = { usernameOrEmail: '', password: '' };
  registerForm = {
    username: '',
    email: '',
    password: '',
    requestAdminAccess: false,
    adminAccessReason: '',
  };
  adminAccessReason = '';
  profileForm = { username: '', email: '' };
  passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
  paymentForm = {
    cardType: CardType.Debit,
    cardHolderName: '',
    cardNumber: '',
    expiryMonth: '',
    expiryYear: '',
    cvv: '',
    amount: '',
    currency: Currency.USD,
    description: '',
  };

  constructor(
    private readonly http: HttpClient,
    private readonly changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    if (typeof sessionStorage === 'undefined') return;
    const savedToken = sessionStorage.getItem('globePayAccessToken');
    if (!savedToken) return;
    this.token = savedToken;
    this.loadProfile(true);
  }

  get isAdmin(): boolean {
    return this.profile?.roles?.includes(UserRole.Admin) ?? false;
  }

  get filteredUsers(): AdminUser[] {
    const query = this.userSearch.trim().toLowerCase();
    if (!query) return this.adminUsers;
    return this.adminUsers.filter((user) =>
      `${user.username} ${user.email} ${user.id}`.toLowerCase().includes(query),
    );
  }

  get completedPayments(): Payment[] {
    return this.payments.filter((payment) => payment.status === 'COMPLETED');
  }

  get paymentVolume(): number {
    return this.completedPayments.reduce((total, payment) => total + Number(payment.amount || 0), 0);
  }

  get currency(): Currency {
    return this.payments[0]?.currency ?? this.paymentForm.currency;
  }

  get currencySymbol(): string {
    return new Intl.NumberFormat('en', {
      style: 'currency',
      currency: this.paymentForm.currency,
    }).formatToParts(0).find((part) => part.type === 'currency')?.value ?? this.paymentForm.currency;
  }

  submitLogin(): void {
    this.beginRequest();
    this.http.post<ApiEnvelope<AuthSession>>(`${AUTH_API}/auth/login`, this.loginForm)
      .pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest()))
      .subscribe({
        next: (response) => {
          this.token = response.data.accessToken;
          sessionStorage.setItem('globePayAccessToken', response.data.accessToken);
          sessionStorage.setItem('globePayRefreshToken', response.data.refreshToken);
          this.profile = {
            id: response.data.id,
            username: response.data.username,
            email: response.data.email,
            roles: response.data.roles || [],
            enabled: true,
          };
          this.profileForm = { username: response.data.username, email: response.data.email };
          this.view = 'overview';
          this.message = `Welcome back, ${this.profile.username}.`;
          this.loadPayments();
          this.loadMyAdminAccessRequest();
        },
        error: (error: unknown) => this.failRequest(error),
      });
  }

  submitRegistration(): void {
    this.beginRequest();
    this.http.post<ApiEnvelope<void>>(`${AUTH_API}/auth/register`, {
      ...this.registerForm,
    }).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: (response) => {
        const requestedAdminAccess = this.registerForm.requestAdminAccess;
        this.loginForm.usernameOrEmail = this.registerForm.email;
        this.loginForm.password = '';
        this.registerForm = {
          username: '',
          email: '',
          password: '',
          requestAdminAccess: false,
          adminAccessReason: '',
        };
        this.authMode = 'login';
        this.message = response.message || (requestedAdminAccess
          ? 'Your account is ready. Your administrator access request is pending review.'
          : 'Your account is ready. You can sign in now.');
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  loadProfile(loadPayments = false): void {
    this.http.get<ApiEnvelope<UserProfile>>(`${AUTH_API}/users/me`, { headers: this.authHeaders() }).subscribe({
      next: (response) => {
        this.profile = response.data;
        this.profileForm = { username: response.data.username, email: response.data.email };
        if (loadPayments) this.loadPayments();
        this.loadMyAdminAccessRequest();
        if (this.isAdmin) this.loadAdminUsers();
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clearSession();
        } else {
          this.error = this.errorMessage(error);
        }
      },
    });
  }

  loadPayments(): void {
    this.http.get<Payment[]>(`${PAYMENT_API}/payments`, { headers: this.authHeaders() }).subscribe({
      next: (payments) => this.payments = payments || [],
      error: (error: unknown) => this.error = this.errorMessage(error),
    });
  }

  loadPaymentAnalytics(): void {
    this.http.get<PaymentAnalytics>(`${PAYMENT_API}/payments/analytics`, {
      headers: this.authHeaders(),
    }).subscribe({
      next: (analytics) => this.paymentAnalytics = analytics,
      error: (error: unknown) => this.error = this.errorMessage(error),
    });
  }

  openTransaction(payment: Payment): void {
    this.selectedPayment = payment;
    this.selectView('transaction-detail');
    this.beginRequest();
    this.http.get<Payment>(`${PAYMENT_API}/payments/${encodeURIComponent(payment.transactionId)}`, {
      headers: this.authHeaders(),
    }).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: (details) => this.selectedPayment = details,
      error: (error: unknown) => this.failRequest(error),
    });
  }

  loadAdminUsers(): void {
    this.http.get<ApiEnvelope<AdminUser[]>>(`${AUTH_API}/admin/users`, { headers: this.authHeaders() }).subscribe({
      next: (response) => this.adminUsers = response.data || [],
      error: (error: unknown) => this.error = this.errorMessage(error),
    });
  }

  loadAdminAccessRequests(): void {
    this.http.get<ApiEnvelope<AdminAccessRequest[]>>(
      `${AUTH_API}/admin/admin-access-requests`,
      { headers: this.authHeaders() },
    ).subscribe({
      next: (response) => this.adminAccessRequests = response.data || [],
      error: (error: unknown) => this.error = this.errorMessage(error),
    });
  }

  loadMyAdminAccessRequest(): void {
    this.http.get<ApiEnvelope<AdminAccessRequest | null>>(
      `${AUTH_API}/users/me/admin-access-request`,
      { headers: this.authHeaders() },
    ).subscribe({
      next: (response) => this.myAdminAccessRequest = response.data,
      error: (error: unknown) => this.error = this.errorMessage(error),
    });
  }

  submitAdminAccessRequest(): void {
    this.beginRequest();
    this.http.post<ApiEnvelope<AdminAccessRequest>>(
      `${AUTH_API}/users/me/admin-access-requests`,
      { reason: this.adminAccessReason },
      { headers: this.authHeaders() },
    ).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: (response) => {
        this.myAdminAccessRequest = response.data;
        this.adminAccessReason = '';
        this.message = response.message || 'Your administrator access request was submitted.';
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  decideAdminAccessRequest(request: AdminAccessRequest, status: 'APPROVED' | 'REJECTED'): void {
    this.beginRequest();
    this.http.post<ApiEnvelope<AdminAccessRequest>>(
      `${AUTH_API}/admin/admin-access-requests/${request.id}/decision`,
      { status },
      { headers: this.authHeaders() },
    ).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: (response) => {
        this.adminAccessRequests = this.adminAccessRequests.filter((item) => item.id !== request.id);
        this.message = response.message || `Request ${status.toLowerCase()}.`;
        if (status === 'APPROVED') this.loadAdminUsers();
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  saveProfile(): void {
    const profile = this.profile;
    if (!profile) return;
    this.beginRequest();
    this.http.put<ApiEnvelope<{ username: string; email: string }>>(
      `${USER_API}/users/${profile.id}`,
      this.profileForm,
      { headers: this.authHeaders() },
    ).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: () => {
        this.profile = { ...profile, ...this.profileForm };
        this.message = 'Profile changes saved.';
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  changePassword(): void {
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.error = 'Your new passwords do not match.';
      return;
    }
    this.beginRequest();
    this.http.post<ApiEnvelope<void>>(`${AUTH_API}/auth/change-password`, {
      currentPassword: this.passwordForm.currentPassword,
      newPassword: this.passwordForm.newPassword,
    }, { headers: this.authHeaders() }).pipe(timeout({ first: 15000 }), finalize(() => this.finishRequest())).subscribe({
      next: () => {
        this.clearSession();
        this.message = 'Password changed. Sign in again with your new password.';
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  submitPayment(): void {
    this.beginRequest();
    const card = {
      cardType: this.paymentForm.cardType,
      cardNumber: this.paymentForm.cardNumber.replace(/\s/g, ''),
      cardHolderName: this.paymentForm.cardHolderName,
      expiryMonth: this.paymentForm.expiryMonth,
      expiryYear: this.paymentForm.expiryYear,
      cvv: this.paymentForm.cvv,
    };
    this.http.post<PaymentToken>(`${PAYMENT_API}/payments/tokenize`, card, {
      headers: this.authHeaders(),
    }).pipe(
      timeout({ first: 15000 }),
      switchMap((tokenized) => this.http.post<Payment>(`${PAYMENT_API}/payments`, {
        cardType: this.paymentForm.cardType,
        cardToken: tokenized.cardToken,
        cardHolderName: this.paymentForm.cardHolderName,
        amount: Number(this.paymentForm.amount),
        currency: this.paymentForm.currency,
        description: this.paymentForm.description,
        threeDSecureRequired: true,
      }, { headers: this.authHeaders() }).pipe(timeout({ first: 15000 }))),
      finalize(() => this.finishRequest()),
    ).subscribe({
      next: (payment) => {
        this.payments = [payment, ...this.payments];
        this.paymentForm = {
          cardType: CardType.Debit, cardHolderName: '', cardNumber: '', expiryMonth: '',
          expiryYear: '', cvv: '', amount: '', currency: Currency.USD, description: '',
        };
        this.view = 'payments';
        this.message = `Payment ${payment.status.toLowerCase()}. Reference ${payment.transactionId}.`;
      },
      error: (error: unknown) => this.failRequest(error),
    });
  }

  signOut(): void {
    const refreshToken = sessionStorage.getItem('globePayRefreshToken');
    if (refreshToken) {
      this.http.post(`${AUTH_API}/auth/logout`, { refreshToken }).subscribe({
        error: (error: unknown) => {
          this.error = `You have been signed out locally, but server logout could not be confirmed: ${this.errorMessage(error)}`;
          this.changeDetector.markForCheck();
        },
      });
    }
    this.clearSession();
    this.message = 'You have been signed out.';
  }

  selectView(view: WorkspaceView): void {
    if (view === 'admin' && !this.isAdmin) return;
    this.view = view;
    this.error = '';
    this.message = '';
    if (view === 'payments') this.loadPayments();
    if (view === 'analytics') {
      this.loadPayments();
      this.loadPaymentAnalytics();
    }
    if (view === 'admin') {
      this.loadAdminUsers();
      this.loadAdminAccessRequests();
    }
  }

  formatDate(value: string | undefined): string {
    return value ? new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—';
  }

  private authHeaders(): { Authorization: string } {
    return { Authorization: `Bearer ${this.token}` };
  }

  private beginRequest(): void {
    this.loading = true;
    this.error = '';
    this.message = '';
  }

  private finishRequest(): void {
    this.loading = false;
    this.changeDetector.markForCheck();
  }

  private failRequest(error: unknown): void {
    this.loading = false;
    this.error = this.errorMessage(error);
    this.changeDetector.markForCheck();
  }

  private errorMessage(error: unknown): string {
    if (error instanceof TimeoutError || (error instanceof Error && error.name === 'TimeoutError')) {
      return 'The request is taking too long. Please try again.';
    }
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string; error?: string } | null;
      return body?.message || body?.error || error.message || 'The request could not be completed.';
    }
    return 'The request could not be completed.';
  }

  private clearSession(): void {
    sessionStorage.removeItem('globePayAccessToken');
    sessionStorage.removeItem('globePayRefreshToken');
    this.token = '';
    this.profile = null;
    this.payments = [];
    this.adminUsers = [];
    this.adminAccessRequests = [];
    this.myAdminAccessRequest = null;
    this.view = 'overview';
    this.loading = false;
  }
}
