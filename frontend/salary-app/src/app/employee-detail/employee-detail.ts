import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { EmployeeDetail, SalaryCreate, ValidationErrorBody } from '../models';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './employee-detail.html',
})
export class EmployeeDetailComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  detail: EmployeeDetail | null = null;
  loading = false;
  error = '';
  deleteError = '';

  basePay: number | null = null;
  currency = 'INR';
  payFrequency: 'ANNUAL' | 'MONTHLY' = 'ANNUAL';
  effectiveFrom = '';
  revisionError = '';
  revisionDetails: string[] = [];
  revisionSuccess = '';
  submittingRevision = false;

  currencies = ['INR', 'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'JPY', 'BRL', 'SGD'];

  ngOnInit(): void {
    this.load();
  }

  id(): string {
    return this.route.snapshot.paramMap.get('id') ?? '';
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.getEmployee(this.id()).subscribe({
      next: (d) => {
        this.detail = d;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.status === 404 ? 'Employee not found.' : 'Failed to load employee.';
        this.loading = false;
      },
    });
  }

  submitRevision(): void {
    this.revisionError = '';
    this.revisionDetails = [];
    this.revisionSuccess = '';
    if (this.basePay === null || this.basePay === undefined || !this.currency || !this.effectiveFrom) {
      this.revisionError = 'Base pay, currency and effective date are all required.';
      return;
    }
    const body: SalaryCreate = {
      basePay: this.basePay,
      currency: this.currency,
      payFrequency: this.payFrequency,
      effectiveFrom: this.effectiveFrom,
    };
    this.submittingRevision = true;
    this.api.addSalary(this.id(), body).subscribe({
      next: (rec) => {
        this.submittingRevision = false;
        this.revisionSuccess =
          'Salary revision added: ' + rec.basePay + ' ' + rec.currency + ' ' + rec.payFrequency + ' from ' + rec.effectiveFrom + '.';
        this.basePay = null;
        this.effectiveFrom = '';
        this.load();
      },
      error: (err) => {
        this.submittingRevision = false;
        const bodyErr = err?.error as ValidationErrorBody | undefined;
        this.revisionError = bodyErr?.error ?? bodyErr?.message ?? 'Failed to add salary revision (HTTP ' + (err?.status ?? '?') + ').';
        this.revisionDetails = bodyErr?.details ?? [];
      },
    });
  }

  deleteEmployee(): void {
    if (!this.detail) return;
    const name = this.detail.employee.name;
    const id = this.detail.employee.id;
    if (!window.confirm('Delete employee ' + name + ' (' + id + ')? This cannot be undone.')) return;
    this.deleteError = '';
    this.api.deleteEmployee(id).subscribe({
      next: () => this.router.navigate(['/employees']),
      error: (err) => {
        this.deleteError = 'Delete failed (HTTP ' + (err?.status ?? '?') + ').';
      },
    });
  }
}
