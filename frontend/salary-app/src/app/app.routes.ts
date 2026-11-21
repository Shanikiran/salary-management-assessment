import { Routes } from '@angular/router';
import { EmployeeListComponent } from './employee-list/employee-list';
import { EmployeeDetailComponent } from './employee-detail/employee-detail';
import { EmployeeFormComponent } from './employee-form/employee-form';
import { DashboardComponent } from './dashboard/dashboard';
import { ImportComponent } from './import/import';

export const routes: Routes = [
  { path: '', redirectTo: '/employees', pathMatch: 'full' },
  { path: 'employees', component: EmployeeListComponent },
  { path: 'employees/new', component: EmployeeFormComponent },
  { path: 'employees/:id', component: EmployeeDetailComponent },
  { path: 'employees/:id/edit', component: EmployeeFormComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'import', component: ImportComponent },
  { path: '**', redirectTo: '/employees' },
];
