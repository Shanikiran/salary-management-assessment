import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { EmployeeCreate, EmployeeStatus, EmployeeUpdate, PayFrequency, ValidationErrorBody } from '../models';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './employee-form.html',
})
export class EmployeeFormComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  isEdit = false;
  editId = '';
  loading = false;
  error = '';
  errorDetails: string[] = [];
  submitting = false;

  name = '';
  department = '';
  role = '';
  country = '';
  city = '';
  joiningDate = '';
  status: EmployeeStatus = 'ACTIVE';

  // Optional initial salary (create only)
  basePay: number | null = null;
  currency = 'INR';
  payFrequency: PayFrequency = 'ANNUAL';
  effectiveFrom = '';

  departments = ['Customer Success', 'Engineering', 'Finance', 'Human Resources', 'Marketing', 'Operations', 'Product', 'Sales'];
  countries = ['Australia', 'Brazil', 'Canada', 'France', 'Germany', 'India', 'Japan', 'Singapore', 'United Kingdom', 'United States'];
  statuses: EmployeeStatus[] = ['ACTIVE', 'ON_LEAVE', 'EXITED'];
  currencies = ['INR', 'USD', 'EUR', 'GBP', 'AUD', 'CAD', 'JPY', 'BRL', 'SGD'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.editId = id;
      this.loading = true;
      this.api.getEmployee(id).subscribe({
        next: (d) => {
          const e = d.employee;
          this.name = e.name;
          this.department = e.department;
          this.role = e.role;
          this.country = e.country;
          this.city = e.city ?? '';
          this.joiningDate = e.joiningDate;
          this.status = e.status;
          this.loading = false;
        },
        error: () => {
          this.error = 'Employee not found.';
          this.loading = false;
        },
      });
    }
  }

  submit(): void {
    this.error = '';
    this.errorDetails = [];
    if (!this.name.trim() || !this.department || !this.role.trim() || !this.country || !this.joiningDate) {
      this.error = 'Name, department, role, country and joining date are all required.';
      return;
    }
    this.submitting = true;
    if (this.isEdit) {
      const body: EmployeeUpdate = {
        name: this.name.trim(),
        department: this.department,
        role: this.role.trim(),
        country: this.country,
        city: this.city.trim() || null,
        joiningDate: this.joiningDate,
        status: this.status,
      };
      this.api.updateEmployee(this.editId, body).subscribe({
        next: (e) => this.router.navigate(['/employees', e.id]),
        error: (err) => this.handleError(err),
      });
    } else {
      const body: EmployeeCreate = {
        name: this.name.trim(),
        department: this.department,
        role: this.role.trim(),
        country: this.country,
        city: this.city.trim() || undefined,
        joiningDate: this.joiningDate,
        status: this.status,
      };
      if (this.basePay !== null && this.basePay !== undefined) {
        body.basePay = this.basePay;
        body.currency = this.currency;
        body.payFrequency = this.payFrequency;
        body.effectiveFrom = this.effectiveFrom || this.joiningDate;
      }
      this.api.createEmployee(body).subscribe({
        next: (e) => this.router.navigate(['/employees', e.id]),
        error: (err) => this.handleError(err),
      });
    }
  }

  private handleError(err: { status?: number; error?: ValidationErrorBody }): void {
    this.submitting = false;
    const bodyErr = err?.error;
    this.error = bodyErr?.error ?? bodyErr?.message ?? 'Save failed (HTTP ' + (err?.status ?? '?') + ').';
    this.errorDetails = bodyErr?.details ?? [];
  }
}
