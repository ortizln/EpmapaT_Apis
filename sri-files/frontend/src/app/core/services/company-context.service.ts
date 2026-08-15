import { Injectable, computed, inject, signal } from '@angular/core';
import { Empresa } from '../../models/empresa.model';
import { EmpresasService } from './empresas.service';
import { catchError, of } from 'rxjs';

const STORAGE_KEY = 'sri-files.empresa-activa';

@Injectable({
  providedIn: 'root'
})
export class CompanyContextService {
  private readonly empresasService = inject(EmpresasService);

  private readonly empresasSignal = signal<Empresa[]>([]);
  private readonly empresaActivaIdSignal = signal<string | null>(this.readStoredCompanyId());
  private readonly loadingSignal = signal(false);

  readonly empresas = this.empresasSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly empresaActiva = computed(() => {
    const activaId = this.empresaActivaIdSignal();
    const empresas = this.empresasSignal();

    if (!empresas.length) {
      return null;
    }

    return empresas.find((empresa) => empresa.id === activaId) ?? empresas[0] ?? null;
  });

  cargarEmpresas(): void {
    if (this.loadingSignal()) {
      return;
    }

    this.loadingSignal.set(true);

    this.empresasService
      .listar()
      .pipe(
        catchError(() => {
          this.empresasSignal.set([]);
          return of([]);
        })
      )
      .subscribe((empresas) => {
        const empresasNormalizadas = Array.isArray(empresas) ? empresas : [];
        this.empresasSignal.set(empresasNormalizadas);

        const activaActual = this.empresaActivaIdSignal();
        const existeActiva = empresasNormalizadas.some((empresa) => empresa.id === activaActual);
        const siguienteActiva = existeActiva ? activaActual : (empresasNormalizadas[0]?.id ?? null);

        this.empresaActivaIdSignal.set(siguienteActiva);
        this.persistCompanyId(siguienteActiva);
        this.loadingSignal.set(false);
      });
  }

  seleccionarEmpresa(id: string): void {
    this.empresaActivaIdSignal.set(id);
    this.persistCompanyId(id);
  }

  private readStoredCompanyId(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }

  private persistCompanyId(id: string | null): void {
    try {
      if (!id) {
        localStorage.removeItem(STORAGE_KEY);
        return;
      }

      localStorage.setItem(STORAGE_KEY, id);
    } catch {
      // Ignore storage errors in restricted environments.
    }
  }
}
