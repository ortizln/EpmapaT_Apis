import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { catchError, finalize, of } from 'rxjs';
import { EmpresasService } from '../../core/services/empresas.service';
import { EmpresaAuditoriaListadoItem } from '../../models/empresa.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-auditoria-empresas-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './auditoria-empresas-page.component.html',
  styleUrl: './auditoria-empresas-page.component.scss'
})
export class AuditoriaEmpresasPageComponent {
  private readonly empresasService = inject(EmpresasService);

  protected items: EmpresaAuditoriaListadoItem[] = [];
  protected loading = false;
  protected error = '';
  protected page = 0;
  protected size = 10;
  protected totalPages = 0;
  protected totalItems = 0;
  protected readonly sizeOptions = [10, 20, 50];

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.loading = true;
    this.error = '';

    this.empresasService
      .obtenerAuditoriaReciente(this.page, this.size)
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar la auditoria administrativa de empresas.';
          this.items = [];
          this.totalPages = 0;
          this.totalItems = 0;
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.items = response.items;
        this.page = response.page;
        this.size = response.size;
        this.totalPages = response.totalPages;
        this.totalItems = response.totalItems;
      });
  }

  protected onPageChange(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }

    this.page = page;
    this.cargar();
  }

  protected onSizeChange(event: Event): void {
    const nextSize = Number((event.target as HTMLSelectElement).value);
    if (Number.isNaN(nextSize) || nextSize === this.size) {
      return;
    }

    this.size = nextSize;
    this.page = 0;
    this.cargar();
  }

  protected visiblePages(): number[] {
    if (this.totalPages <= 1) {
      return [0];
    }

    const start = Math.max(0, this.page - 1);
    const end = Math.min(this.totalPages - 1, start + 2);
    const adjustedStart = Math.max(0, end - 2);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }

  protected getRangeStart(): number {
    return this.totalItems === 0 ? 0 : this.page * this.size + 1;
  }

  protected getRangeEnd(): number {
    return Math.min((this.page + 1) * this.size, this.totalItems);
  }

  protected formatValue(value: string | null | undefined, fallback = 'No registrado'): string {
    return value && value.trim().length > 0 ? value : fallback;
  }
}
