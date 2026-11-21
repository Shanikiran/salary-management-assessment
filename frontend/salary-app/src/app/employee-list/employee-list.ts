import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { Employee } from '../models';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './employee-list.html',
})
export class EmployeeListComponent implements OnInit {
  private api = inject(ApiService);

  employees: Employee[] = [];
  totalElements = 0;
  totalPages = 0;
  loading = false;
  error = '';

  name = '';
  department = '';
  country = '';
  status = '';
  sort = 'name';
  order = 'asc';
  page = 0;
  size = 20;

  departments: string[] = [];
  countries: string[] = [];
  statuses = ['ACTIVE', 'ON_LEAVE', 'EXITED'];
  sortFields = ['name', 'department', 'country', 'role', 'joiningDate'];
  pageSizes = [10, 20, 50, 100];

  ngOnInit(): void {
    this.api.insightsByDepartment().subscribe({
      next: (rows) => (this.departments = [...new Set(rows.map((r) => r.group))].sort()),
      error: () => (this.departments = []),
    });
    this.api.insightsByCountry().subscribe({
      next: (rows) => (this.countries = [...new Set(rows.map((r) => r.group))].sort()),
      error: () => (this.countries = []),
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api
      .listEmployees({
        name: this.name.trim() || undefined,
        department: this.department || undefined,
        country: this.country || undefined,
        status: this.status || undefined,
        sort: this.sort,
        order: this.order,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (res) => {
          this.employees = res.content;
          this.totalElements = res.totalElements;
          this.totalPages = res.totalPages;
          this.page = res.page;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load employees: ' + (err?.error?.message ?? err?.message ?? err.status);
          this.loading = false;
        },
      });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.name = '';
    this.department = '';
    this.country = '';
    this.status = '';
    this.sort = 'name';
    this.order = 'asc';
    this.page = 0;
    this.load();
  }

  changePage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.load();
  }

  changeSize(): void {
    this.page = 0;
    this.load();
  }

  pageNumbers(): number[] {
    const total = this.totalPages;
    const cur = this.page;
    const start = Math.max(0, Math.min(cur - 2, total - 5));
    const out: number[] = [];
    for (let i = start; i < Math.min(total, start + 5); i++) out.push(i);
    return out;
  }
}
