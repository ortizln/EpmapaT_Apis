import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
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

@Injectable({
  providedIn: 'root'
})
export class CatalogosComercialesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/catalogos-comerciales`;

  listarClientes(empresaId: string): Observable<ClienteCatalogo[]> {
    return this.http.get<ClienteCatalogo[]>(`${this.apiUrl}/clientes?empresaId=${empresaId}`);
  }

  obtenerCliente(id: string): Observable<ClienteCatalogo> {
    return this.http.get<ClienteCatalogo>(`${this.apiUrl}/clientes/${id}`);
  }

  crearCliente(payload: ClienteCatalogoRequest): Observable<ClienteCatalogo> {
    return this.http.post<ClienteCatalogo>(`${this.apiUrl}/clientes`, payload);
  }

  actualizarCliente(id: string, payload: ClienteCatalogoRequest): Observable<ClienteCatalogo> {
    return this.http.put<ClienteCatalogo>(`${this.apiUrl}/clientes/${id}`, payload);
  }

  actualizarEstadoCliente(id: string, activo: boolean): Observable<ClienteCatalogo> {
    return this.http.patch<ClienteCatalogo>(`${this.apiUrl}/clientes/${id}/estado`, { activo });
  }

  listarProductos(empresaId: string): Observable<ProductoCatalogo[]> {
    return this.http.get<ProductoCatalogo[]>(`${this.apiUrl}/productos?empresaId=${empresaId}`);
  }

  obtenerProducto(id: string): Observable<ProductoCatalogo> {
    return this.http.get<ProductoCatalogo>(`${this.apiUrl}/productos/${id}`);
  }

  crearProducto(payload: ProductoCatalogoRequest): Observable<ProductoCatalogo> {
    return this.http.post<ProductoCatalogo>(`${this.apiUrl}/productos`, payload);
  }

  actualizarProducto(id: string, payload: ProductoCatalogoRequest): Observable<ProductoCatalogo> {
    return this.http.put<ProductoCatalogo>(`${this.apiUrl}/productos/${id}`, payload);
  }

  actualizarEstadoProducto(id: string, activo: boolean): Observable<ProductoCatalogo> {
    return this.http.patch<ProductoCatalogo>(`${this.apiUrl}/productos/${id}/estado`, { activo });
  }

  listarFormasPago(empresaId: string): Observable<FormaPagoCatalogo[]> {
    return this.http.get<FormaPagoCatalogo[]>(`${this.apiUrl}/formas-pago?empresaId=${empresaId}`);
  }

  obtenerFormaPago(id: string): Observable<FormaPagoCatalogo> {
    return this.http.get<FormaPagoCatalogo>(`${this.apiUrl}/formas-pago/${id}`);
  }

  crearFormaPago(payload: FormaPagoCatalogoRequest): Observable<FormaPagoCatalogo> {
    return this.http.post<FormaPagoCatalogo>(`${this.apiUrl}/formas-pago`, payload);
  }

  actualizarFormaPago(id: string, payload: FormaPagoCatalogoRequest): Observable<FormaPagoCatalogo> {
    return this.http.put<FormaPagoCatalogo>(`${this.apiUrl}/formas-pago/${id}`, payload);
  }

  actualizarEstadoFormaPago(id: string, activo: boolean): Observable<FormaPagoCatalogo> {
    return this.http.patch<FormaPagoCatalogo>(`${this.apiUrl}/formas-pago/${id}/estado`, { activo });
  }

  listarIva(empresaId: string): Observable<IvaTarifaCatalogo[]> {
    return this.http.get<IvaTarifaCatalogo[]>(`${this.apiUrl}/iva?empresaId=${empresaId}`);
  }

  obtenerIva(id: string): Observable<IvaTarifaCatalogo> {
    return this.http.get<IvaTarifaCatalogo>(`${this.apiUrl}/iva/${id}`);
  }

  crearIva(payload: IvaTarifaCatalogoRequest): Observable<IvaTarifaCatalogo> {
    return this.http.post<IvaTarifaCatalogo>(`${this.apiUrl}/iva`, payload);
  }

  actualizarIva(id: string, payload: IvaTarifaCatalogoRequest): Observable<IvaTarifaCatalogo> {
    return this.http.put<IvaTarifaCatalogo>(`${this.apiUrl}/iva/${id}`, payload);
  }

  actualizarEstadoIva(id: string, activo: boolean): Observable<IvaTarifaCatalogo> {
    return this.http.patch<IvaTarifaCatalogo>(`${this.apiUrl}/iva/${id}/estado`, { activo });
  }
}
