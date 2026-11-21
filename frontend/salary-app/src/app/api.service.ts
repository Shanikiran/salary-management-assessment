import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DistributionRow, Employee, EmployeeCreate, EmployeeDetail, EmployeePage,
  EmployeeUpdate, ImportResult, InsightRow, SalaryCreate, SalaryRecord,
} from './models';

export interface EmployeeQuery {
  name?: string;
  department?: string;
  country?: string;
  status?: string;
  sort?: string;
  order?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = '/api';

  listEmployees(q: EmployeeQuery): Observable<EmployeePage> {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(q)) {
      if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
    }
    return this.http.get<EmployeePage>(`${this.base}/employees`, { params });
  }

  getEmployee(id: string): Observable<EmployeeDetail> {
    return this.http.get<EmployeeDetail>(`${this.base}/employees/${encodeURIComponent(id)}`);
  }

  createEmployee(body: EmployeeCreate): Observable<Employee> {
    return this.http.post<Employee>(`${this.base}/employees`, body);
  }

  updateEmployee(id: string, body: EmployeeUpdate): Observable<Employee> {
    return this.http.put<Employee>(`${this.base}/employees/${encodeURIComponent(id)}`, body);
  }

  deleteEmployee(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/employees/${encodeURIComponent(id)}`);
  }

  addSalary(id: string, body: SalaryCreate): Observable<SalaryRecord> {
    return this.http.post<SalaryRecord>(`${this.base}/employees/${encodeURIComponent(id)}/salaries`, body);
  }

  importCsv(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportResult>(`${this.base}/employees/import`, form);
  }

  insightsByDepartment(): Observable<InsightRow[]> {
    return this.http.get<InsightRow[]>(`${this.base}/insights/by-department`);
  }

  insightsByCountry(): Observable<InsightRow[]> {
    return this.http.get<InsightRow[]>(`${this.base}/insights/by-country`);
  }

  insightsByRole(): Observable<InsightRow[]> {
    return this.http.get<InsightRow[]>(`${this.base}/insights/by-role`);
  }

  compareRole(role: string): Observable<InsightRow[]> {
    return this.http.get<InsightRow[]>(`${this.base}/insights/compare-role`, {
      params: new HttpParams().set('role', role),
    });
  }

  distribution(): Observable<DistributionRow[]> {
    return this.http.get<DistributionRow[]>(`${this.base}/insights/distribution`);
  }

  headcountByBand(): Observable<Record<string, { label: string; from: number | null; to: number | null; headcount: number }[]>> {
    return this.http.get<Record<string, { label: string; from: number | null; to: number | null; headcount: number }[]>>(
      `${this.base}/insights/headcount-by-band`,
    );
  }

  exportUrl(view: string): string {
    return `${this.base}/insights/export?view=${encodeURIComponent(view)}`;
  }
}
