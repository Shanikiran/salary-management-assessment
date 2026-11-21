import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { DistributionRow, InsightRow, PayBand } from '../models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);

  byDepartment: InsightRow[] = [];
  byCountry: InsightRow[] = [];
  byRole: InsightRow[] = [];
  distribution: DistributionRow[] = [];
  headcountByBand: Record<string, PayBand[]> = {};
  bandCurrencies: string[] = [];

  roles: string[] = [];
  compareRole = 'Software Engineer';
  compareRows: InsightRow[] = [];
  compareLoading = false;
  compareError = '';

  loading = false;
  error = '';

  ngOnInit(): void {
    this.loading = true;
    this.api.insightsByDepartment().subscribe({ next: (r) => (this.byDepartment = r), error: (e) => this.fail(e) });
    this.api.insightsByCountry().subscribe({ next: (r) => (this.byCountry = r), error: (e) => this.fail(e) });
    this.api.insightsByRole().subscribe({
      next: (r) => {
        this.byRole = r;
        this.roles = [...new Set(r.map((x) => x.group))].sort();
        if (!this.roles.includes(this.compareRole) && this.roles.length > 0) this.compareRole = this.roles[0];
        this.loading = false;
        this.runCompare();
      },
      error: (e) => this.fail(e),
    });
    this.api.distribution().subscribe({ next: (r) => (this.distribution = r), error: (e) => this.fail(e) });
    this.api.headcountByBand().subscribe({
      next: (m) => {
        this.headcountByBand = m;
        this.bandCurrencies = Object.keys(m).sort();
      },
      error: (e) => this.fail(e),
    });
  }

  private fail(err: { message?: string }): void {
    this.error = 'Failed to load insights: ' + (err?.message ?? 'unknown error');
    this.loading = false;
  }

  runCompare(): void {
    if (!this.compareRole) return;
    this.compareLoading = true;
    this.compareError = '';
    this.api.compareRole(this.compareRole).subscribe({
      next: (r) => {
        this.compareRows = r;
        this.compareLoading = false;
      },
      error: (err) => {
        this.compareError = 'Comparison failed (HTTP ' + (err?.status ?? '?') + ').';
        this.compareLoading = false;
      },
    });
  }

  exportUrl(view: string): string {
    return this.api.exportUrl(view);
  }

  bandRange(b: PayBand): string {
    const fmt = (n: number | null) => (n === null || n === undefined ? '—' : n.toLocaleString('en-IN'));
    if (b.from === null || b.from === undefined) return '≤ ' + fmt(b.to);
    if (b.to === null || b.to === undefined) return '> ' + fmt(b.from);
    return fmt(b.from) + ' – ' + fmt(b.to);
  }
}
