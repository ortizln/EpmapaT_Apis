import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, Observable, of } from 'rxjs';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CatalogosComercialesService } from '../../core/services/catalogos-comerciales.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import {
  ClienteCatalogo,
  ClienteCatalogoRequest,
  FormaPagoCatalogo,
  FormaPagoCatalogoRequest,
  IvaTarifaCatalogo,
  IvaTarifaCatalogoRequest,
  ProductoCatalogo,
  ProductoCatalogoRequest
} from '../../models/catalogos-comerciales.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ComercialTab } from './catalogos-comerciales-page.component';

type CatalogItem = ClienteCatalogo | ProductoCatalogo | FormaPagoCatalogo | IvaTarifaCatalogo;

@Component({
  selector: 'app-catalogo-comercial-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './catalogo-comercial-form-page.component.html',
  styleUrl: './catalogos-comerciales-page.component.scss'
})
export class CatalogoComercialFormPageComponent {
  private readonly service = inject(CatalogosComercialesService);
  protected readonly companyContext = inject(CompanyContextService);
  private readonly alerts = inject(AppAlertService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly tipo = (this.route.snapshot.paramMap.get('tipo') as ComercialTab | null) ?? 'clientes';
  protected item = history.state['item'] as CatalogItem | null;
  protected readonly isEditMode = this.route.snapshot.paramMap.has('id');
  protected readonly itemId = this.route.snapshot.paramMap.get('id');
  protected saving = false;
  protected loading = false;
  protected error = '';

  protected readonly clienteForm = this.fb.group({
    tipoIdentificacion: this.fb.nonNullable.control('04', [Validators.required]),
    identificacion: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
    razonSocial: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(300)]),
    nombreComercial: this.fb.nonNullable.control('', [Validators.maxLength(300)]),
    email: this.fb.nonNullable.control('', [Validators.maxLength(320)]),
    telefono: this.fb.nonNullable.control('', [Validators.maxLength(30)]),
    direccion: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    observacion: this.fb.nonNullable.control('', [Validators.maxLength(500)])
  });

  protected readonly productoForm = this.fb.group({
    codigo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(60)]),
    nombre: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(300)]),
    descripcion: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    unidadMedida: this.fb.nonNullable.control('UND', [Validators.maxLength(20)]),
    precioBase: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    porcentajeIva: this.fb.nonNullable.control(15, [Validators.required, Validators.min(0)])
  });

  protected readonly formaPagoForm = this.fb.group({
    codigo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
    nombre: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    descripcion: this.fb.nonNullable.control('', [Validators.maxLength(300)]),
    diasPlazo: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)])
  });

  protected readonly ivaForm = this.fb.group({
    codigo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
    nombre: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    porcentaje: this.fb.nonNullable.control(15, [Validators.required, Validators.min(0)]),
    codigoSri: this.fb.nonNullable.control('2', [Validators.maxLength(10)]),
    descripcion: this.fb.nonNullable.control('', [Validators.maxLength(300)])
  });

  constructor() {
    this.companyContext.cargarEmpresas();

    if (this.isEditMode && !this.item) {
      this.cargarItemDesdeBackend();
    } else {
      this.hidratarFormulario();
    }
  }

  protected get title(): string {
    const prefix = this.isEditMode ? 'Editar' : 'Nuevo';
    return `${prefix} ${this.getTipoSingular()}`;
  }

  protected get description(): string {
    return `Formulario dedicado para administrar ${this.getTipoPlural()} de la empresa activa.`;
  }

  protected volver(): void {
    this.router.navigate(['/administracion/catalogos-comerciales']);
  }

  protected guardar(): void {
    const empresaId = this.companyContext.empresaActiva()?.id;
    if (!empresaId) {
      this.alerts.warning('Empresa requerida', 'Selecciona una empresa activa antes de guardar.');
      return;
    }

    const form = this.getActiveForm();
    if (form.invalid) {
      form.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Completa la informacion requerida antes de guardar.');
      return;
    }

    this.saving = true;
    this.error = '';

    const request$ = this.getSaveRequest(empresaId);

    request$
      .pipe(
        catchError(() => {
          this.error = `No fue posible guardar ${this.getTipoSingular()} en este momento.`;
          this.alerts.error('No se pudo guardar', `El backend no pudo guardar ${this.getTipoSingular()}.`);
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
        })
      )
      .subscribe((response: CatalogItem | null) => {
        if (!response) {
          return;
        }

        this.alerts.success('Catalogo guardado', `${this.getTipoSingularCapitalized()} fue guardado correctamente.`);
        this.volver();
      });
  }

  private cargarItemDesdeBackend(): void {
    if (!this.itemId) {
      this.alerts.warning('Registro no encontrado', 'No se recibio el identificador del catalogo a editar.');
      this.volver();
      return;
    }

    this.loading = true;
    this.error = '';

    this.getDetailRequest(this.itemId)
      .pipe(
        catchError(() => {
          this.error = `No fue posible cargar ${this.getTipoSingular()} para edicion.`;
          this.alerts.error('No se pudo cargar el registro', `El backend no pudo devolver ${this.getTipoSingular()} solicitado.`);
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe((response: CatalogItem | null) => {
        if (!response) {
          return;
        }

        this.item = response;
        this.hidratarFormulario();
      });
  }

  protected getActiveForm() {
    switch (this.tipo) {
      case 'productos':
        return this.productoForm;
      case 'formas-pago':
        return this.formaPagoForm;
      case 'iva':
        return this.ivaForm;
      default:
        return this.clienteForm;
    }
  }

  private getSaveRequest(empresaId: string): Observable<CatalogItem> {
    switch (this.tipo) {
      case 'productos': {
        const payload: ProductoCatalogoRequest = { empresaId, ...this.productoForm.getRawValue() };
        return this.isEditMode && this.item
          ? this.service.actualizarProducto(this.item.id, payload)
          : this.service.crearProducto(payload);
      }
      case 'formas-pago': {
        const payload: FormaPagoCatalogoRequest = { empresaId, ...this.formaPagoForm.getRawValue() };
        return this.isEditMode && this.item
          ? this.service.actualizarFormaPago(this.item.id, payload)
          : this.service.crearFormaPago(payload);
      }
      case 'iva': {
        const payload: IvaTarifaCatalogoRequest = { empresaId, ...this.ivaForm.getRawValue() };
        return this.isEditMode && this.item ? this.service.actualizarIva(this.item.id, payload) : this.service.crearIva(payload);
      }
      default: {
        const payload: ClienteCatalogoRequest = { empresaId, ...this.clienteForm.getRawValue() };
        return this.isEditMode && this.item
          ? this.service.actualizarCliente(this.item.id, payload)
          : this.service.crearCliente(payload);
      }
    }
  }

  private getDetailRequest(id: string): Observable<CatalogItem> {
    switch (this.tipo) {
      case 'productos':
        return this.service.obtenerProducto(id);
      case 'formas-pago':
        return this.service.obtenerFormaPago(id);
      case 'iva':
        return this.service.obtenerIva(id);
      default:
        return this.service.obtenerCliente(id);
    }
  }

  private hidratarFormulario(): void {
    if (!this.item) {
      return;
    }

    switch (this.tipo) {
      case 'productos': {
        const item = this.item as ProductoCatalogo;
        this.productoForm.patchValue({
          codigo: item.codigo,
          nombre: item.nombre,
          descripcion: item.descripcion ?? '',
          unidadMedida: item.unidadMedida ?? 'UND',
          precioBase: item.precioBase,
          porcentajeIva: item.porcentajeIva
        });
        break;
      }
      case 'formas-pago': {
        const item = this.item as FormaPagoCatalogo;
        this.formaPagoForm.patchValue({
          codigo: item.codigo,
          nombre: item.nombre,
          descripcion: item.descripcion ?? '',
          diasPlazo: item.diasPlazo
        });
        break;
      }
      case 'iva': {
        const item = this.item as IvaTarifaCatalogo;
        this.ivaForm.patchValue({
          codigo: item.codigo,
          nombre: item.nombre,
          porcentaje: item.porcentaje,
          codigoSri: item.codigoSri ?? '2',
          descripcion: item.descripcion ?? ''
        });
        break;
      }
      default: {
        const item = this.item as ClienteCatalogo;
        this.clienteForm.patchValue({
          tipoIdentificacion: item.tipoIdentificacion,
          identificacion: item.identificacion,
          razonSocial: item.razonSocial,
          nombreComercial: item.nombreComercial ?? '',
          email: item.email ?? '',
          telefono: item.telefono ?? '',
          direccion: item.direccion ?? '',
          observacion: item.observacion ?? ''
        });
      }
    }
  }

  private getTipoSingular(): string {
    switch (this.tipo) {
      case 'productos':
        return 'producto';
      case 'formas-pago':
        return 'forma de pago';
      case 'iva':
        return 'porcentaje IVA';
      default:
        return 'cliente';
    }
  }

  private getTipoPlural(): string {
    switch (this.tipo) {
      case 'productos':
        return 'productos';
      case 'formas-pago':
        return 'formas de pago';
      case 'iva':
        return 'porcentajes IVA';
      default:
        return 'clientes y beneficiarios';
    }
  }

  private getTipoSingularCapitalized(): string {
    const value = this.getTipoSingular();
    return value.charAt(0).toUpperCase() + value.slice(1);
  }
}
