export interface ClienteCatalogo {
  id: string;
  empresaId: string;
  tipoIdentificacion: string;
  identificacion: string;
  razonSocial: string;
  nombreComercial: string | null;
  email: string | null;
  telefono: string | null;
  direccion: string | null;
  observacion: string | null;
  activo: boolean;
}

export interface ClienteCatalogoRequest {
  empresaId: string;
  tipoIdentificacion: string;
  identificacion: string;
  razonSocial: string;
  nombreComercial?: string | null;
  email?: string | null;
  telefono?: string | null;
  direccion?: string | null;
  observacion?: string | null;
}

export interface ProductoCatalogo {
  id: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  unidadMedida: string | null;
  precioBase: number;
  porcentajeIva: number;
  activo: boolean;
}

export interface ProductoCatalogoRequest {
  empresaId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  unidadMedida?: string | null;
  precioBase: number;
  porcentajeIva: number;
}

export interface FormaPagoCatalogo {
  id: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  diasPlazo: number;
  activo: boolean;
}

export interface FormaPagoCatalogoRequest {
  empresaId: string;
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  diasPlazo: number;
}

export interface IvaTarifaCatalogo {
  id: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  porcentaje: number;
  codigoSri: string | null;
  descripcion: string | null;
  activo: boolean;
}

export interface IvaTarifaCatalogoRequest {
  empresaId: string;
  codigo: string;
  nombre: string;
  porcentaje: number;
  codigoSri?: string | null;
  descripcion?: string | null;
}
