import { Injectable, computed, signal } from '@angular/core';
import { UsuarioAutenticado } from '../../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  private readonly usuarioSignal = signal<UsuarioAutenticado | null>(null);

  readonly usuario = this.usuarioSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.usuarioSignal() !== null);

  setUsuario(usuario: UsuarioAutenticado | null): void {
    this.usuarioSignal.set(usuario);
  }
}
