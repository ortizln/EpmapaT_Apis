import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AuthStore } from '../../core/auth/auth.store';
import { AppAlertService } from '../../core/services/app-alert.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { UsuarioSistema } from '../../models/auth.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-usuario-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './usuario-form-page.component.html',
  styleUrl: './usuarios-page.component.scss'
})
export class UsuarioFormPageComponent {
  private readonly usuariosService = inject(UsuariosService);
  private readonly authStore = inject(AuthStore);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alerts = inject(AppAlertService);

  protected usuario = history.state['usuario'] as UsuarioSistema | null;
  protected saving = false;
  protected error = '';

  protected readonly createForm = this.fb.group({
    username: ['', [Validators.required]],
    nombre: ['', [Validators.required]],
    correo: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    rol: ['OPERADOR', [Validators.required]]
  });

  protected readonly resetPasswordForm = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmacion: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor() {
    if (!this.usuario && this.route.snapshot.paramMap.get('id')) {
      this.alerts.warning(
        'Edicion limitada',
        'Para editar un usuario debes abrirlo desde el listado actual; el backend aun no expone un endpoint de detalle.'
      );
      this.volver();
    }
  }

  protected volver(): void {
    this.router.navigate(['/usuarios']);
  }

  protected crearUsuario(): void {
    this.error = '';

    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Completa la informacion requerida del usuario.');
      return;
    }

    this.saving = true;

    this.usuariosService
      .crear({
        username: this.createForm.controls.username.value ?? '',
        nombre: this.createForm.controls.nombre.value ?? '',
        correo: this.createForm.controls.correo.value ?? '',
        password: this.createForm.controls.password.value ?? '',
        rol: this.createForm.controls.rol.value ?? 'OPERADOR'
      })
      .pipe(
        catchError(() => {
          this.error = 'No fue posible crear el usuario. Revisa si el username o correo ya existen.';
          this.alerts.error('Usuario no creado', 'El backend no pudo crear el usuario solicitado.');
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.alerts.success('Usuario creado', `El usuario ${response.username} fue creado correctamente.`);
        this.volver();
      });
  }

  protected confirmarResetPassword(): void {
    if (!this.usuario) {
      return;
    }

    this.error = '';

    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Ingresa y confirma la nueva contrasena.');
      return;
    }

    const password = this.resetPasswordForm.controls.password.value ?? '';
    const confirmacion = this.resetPasswordForm.controls.confirmacion.value ?? '';

    if (password !== confirmacion) {
      this.error = 'La confirmacion de contrasena no coincide.';
      return;
    }

    this.saving = true;

    this.usuariosService
      .resetearPassword(this.usuario.id, password)
      .pipe(
        catchError(() => {
          this.error = `No fue posible resetear la contrasena de ${this.usuario?.username}.`;
          this.alerts.error('Contrasena no actualizada', 'No se pudo actualizar la clave del usuario.');
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.usuario = response;
        this.resetPasswordForm.reset({
          password: '',
          confirmacion: ''
        });
        this.alerts.success('Contrasena actualizada', `La clave de ${response.username} fue actualizada correctamente.`);
      });
  }

  protected toggleEstado(usuario: UsuarioSistema): void {
    this.saving = true;
    this.error = '';

    this.usuariosService
      .actualizarEstado(usuario.id, !usuario.activo)
      .pipe(
        catchError(() => {
          this.error = `No fue posible actualizar el estado de ${usuario.username}.`;
          this.alerts.error('Estado no actualizado', 'No se pudo cambiar el estado del usuario.');
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.usuario = response;
        this.alerts.success('Estado actualizado', `El estado de ${response.username} fue actualizado correctamente.`);
      });
  }

  protected isSelfAdminDeactivateBlocked(usuario: UsuarioSistema): boolean {
    const actual = this.authStore.usuario();
    return !!actual && actual.id === usuario.id && usuario.activo && actual.roles.includes('ADMIN');
  }
}
