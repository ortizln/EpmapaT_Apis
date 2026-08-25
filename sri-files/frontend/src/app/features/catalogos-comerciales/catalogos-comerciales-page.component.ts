import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { CatalogosComercialesService } from '../../core/services/catalogos-comerciales.service';
import { CompanyContextService } from '../../core/services/company-context.service';
import { AppAlertService } from '../../core/services/app-alert.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import {
  ClienteCatalogo,
  FormaPagoCatalogo,
  IvaTarifaCatalogo,
  ProductoCatalogo
} from '../../models/catalogos-comerciales.model';

export type ComercialTab = 'clientes' | 'productos' | 'formas-pago' | 'iva';

@Component({
  selector: 'app-catalogos-comerciales-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './catalogos-comerciales-page.component.html',
  styleUrl: './catalogos-comerciales-page.component.scss'
})
export class CatalogosComercialesPageComponent {
  protected readonly companyContext = inject(CompanyContextService);
  private readonly service = inject(CatalogosComercialesService);
  private readonly alerts = inject(AppAlertService);
  private readonly router = inject(Router);

  protected readonly tabs: Array<{ id: ComercialTab; label: string; description: string }> = [
    { id: 'clientes', label: 'Clientes / beneficiarios', description: 'Receptores frecuentes y beneficiarios del circuito documental.' },
    { id: 'productos', label: 'Productos', description: 'Bienes y servicios listos para usar en facturacion.' },
    { id: 'formas-pago', label: 'Formas de pago', description: 'Medios de pago y plazos de cobro por empresa.' },
    { id: 'iva', label: 'Porcentajes IVA', description: 'Tarifas tributarias y equivalencias SRI administrables.' }
  ];

  protected activeTab: ComercialTab = 'clientes';
  protected loading = false;
  protected error = '';

  protected clientes: ClienteCatalogo[] = [];
  protected productos: ProductoCatalogo[] = [];
  protected formasPago: FormaPagoCatalogo[] = [];
  protected tarifasIva: IvaTarifaCatalogo[] = [];

  constructor() {
    this.companyContext.cargarEmpresas();
    effect(() => {
      this.companyContext.empresaActiva()?.id;
      this.cargarTodo();
    });
  }

  protected selectTab(tab: ComercialTab): void {
    this.activeTab = tab;
  }

  protected irANuevo(tipo: ComercialTab): void {
    this.router.navigate([`/administracion/catalogos-comerciales/${tipo}/nuevo`]);
  }

  protected irAEditar(tipo: ComercialTab, item: ClienteCatalogo | ProductoCatalogo | FormaPagoCatalogo | IvaTarifaCatalogo): void {
    this.router.navigate([`/administracion/catalogos-comerciales/${tipo}/${item.id}/editar`], {
      state: { item }
    });
  }

  protected cambiarEstadoCliente(item: ClienteCatalogo): void {
    this.service.actualizarEstadoCliente(item.id, !item.activo).subscribe({
      next: () => {
        this.alerts.success('Estado actualizado', 'El cliente/beneficiario cambio su estado operativo.');
        this.cargarClientes(item.empresaId);
      },
      error: () => this.alerts.error('No se pudo cambiar el estado', 'Intenta nuevamente en unos segundos.')
    });
  }

  protected cambiarEstadoProducto(item: ProductoCatalogo): void {
    this.service.actualizarEstadoProducto(item.id, !item.activo).subscribe({
      next: () => {
        this.alerts.success('Estado actualizado', 'El producto cambio su disponibilidad operativa.');
        this.cargarProductos(item.empresaId);
      },
      error: () => this.alerts.error('No se pudo cambiar el estado', 'Intenta nuevamente en unos segundos.')
    });
  }

  protected cambiarEstadoFormaPago(item: FormaPagoCatalogo): void {
    this.service.actualizarEstadoFormaPago(item.id, !item.activo).subscribe({
      next: () => {
        this.alerts.success('Estado actualizado', 'La forma de pago cambio su estado operativo.');
        this.cargarFormasPago(item.empresaId);
      },
      error: () => this.alerts.error('No se pudo cambiar el estado', 'Intenta nuevamente en unos segundos.')
    });
  }

  protected cambiarEstadoIva(item: IvaTarifaCatalogo): void {
    this.service.actualizarEstadoIva(item.id, !item.activo).subscribe({
      next: () => {
        this.alerts.success('Estado actualizado', 'La tarifa IVA cambio su estado operativo.');
        this.cargarIva(item.empresaId);
      },
      error: () => this.alerts.error('No se pudo cambiar el estado', 'Intenta nuevamente en unos segundos.')
    });
  }

  private cargarTodo(): void {
    const empresaId = this.companyContext.empresaActiva()?.id;
    if (!empresaId) {
      this.error = 'Selecciona una empresa activa para administrar los catalogos comerciales.';
      this.clientes = [];
      this.productos = [];
      this.formasPago = [];
      this.tarifasIva = [];
      return;
    }

    this.loading = true;
    this.error = '';
    forkJoin({
      clientes: this.service.listarClientes(empresaId),
      productos: this.service.listarProductos(empresaId),
      formasPago: this.service.listarFormasPago(empresaId),
      iva: this.service.listarIva(empresaId)
    })
      .pipe(
        catchError(() => {
          this.error = 'No fue posible cargar los catalogos comerciales de la empresa activa.';
          this.clientes = [];
          this.productos = [];
          this.formasPago = [];
          this.tarifasIva = [];
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

        this.clientes = response.clientes;
        this.productos = response.productos;
        this.formasPago = response.formasPago;
        this.tarifasIva = response.iva;
      });
  }

  private cargarClientes(empresaId: string): void {
    this.service.listarClientes(empresaId).subscribe((items) => {
      this.clientes = items;
    });
  }

  private cargarProductos(empresaId: string): void {
    this.service.listarProductos(empresaId).subscribe((items) => {
      this.productos = items;
    });
  }

  private cargarFormasPago(empresaId: string): void {
    this.service.listarFormasPago(empresaId).subscribe((items) => {
      this.formasPago = items;
    });
  }

  private cargarIva(empresaId: string): void {
    this.service.listarIva(empresaId).subscribe((items) => {
      this.tarifasIva = items;
    });
  }
}
