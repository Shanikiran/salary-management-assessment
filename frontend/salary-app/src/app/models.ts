export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'EXITED';
export type PayFrequency = 'ANNUAL' | 'MONTHLY';

export interface Employee {
  id: string;
  name: string;
  department: string;
  role: string;
  country: string;
  city: string | null;
  joiningDate: string;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SalaryRecord {
  id: number;
  employeeId: string;
  basePay: number;
  currency: string;
  payFrequency: PayFrequency;
  effectiveFrom: string;
  createdAt: string;
}

export interface EmployeeDetail {
  employee: Employee;
  currentSalary: SalaryRecord | null;
  salaryHistory: SalaryRecord[];
}

export interface EmployeePage {
  content: Employee[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EmployeeCreate {
  name: string;
  department: string;
  role: string;
  country: string;
  city?: string;
  joiningDate: string;
  status: EmployeeStatus;
  basePay?: number | null;
  currency?: string;
  payFrequency?: PayFrequency;
  effectiveFrom?: string;
}

export interface EmployeeUpdate {
  name: string;
  department: string;
  role: string;
  country: string;
  city: string | null;
  joiningDate: string;
  status: EmployeeStatus;
}

export interface SalaryCreate {
  basePay: number;
  currency: string;
  payFrequency: PayFrequency;
  effectiveFrom: string;
}

export interface InsightRow {
  group: string;
  currency: string;
  headcount: number;
  average: number;
  median: number;
  min: number;
  max: number;
}

export interface PayBand {
  label: string;
  from: number | null;
  to: number | null;
  headcount: number;
}

export interface DistributionRow {
  currency: string;
  bands: PayBand[];
}

export interface ImportError {
  row: number;
  reason: string;
}

export interface ImportResult {
  imported: number;
  failed: number;
  errors: ImportError[];
}

export interface ValidationErrorBody {
  error?: string;
  details?: string[];
  message?: string;
}
