import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../api.service';
import { ImportResult } from '../models';

@Component({
  selector: 'app-import',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './import.html',
})
export class ImportComponent {
  private api = inject(ApiService);

  selectedFile: File | null = null;
  uploading = false;
  error = '';
  result: ImportResult | null = null;

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
    this.result = null;
    this.error = '';
  }

  upload(): void {
    if (!this.selectedFile) {
      this.error = 'Choose a CSV file first.';
      return;
    }
    this.uploading = true;
    this.error = '';
    this.result = null;
    this.api.importCsv(this.selectedFile).subscribe({
      next: (r) => {
        this.result = r;
        this.uploading = false;
      },
      error: (err) => {
        this.error = 'Import failed (HTTP ' + (err?.status ?? '?') + '): ' + (err?.error?.message ?? err?.message ?? 'unknown error');
        this.uploading = false;
      },
    });
  }
}
