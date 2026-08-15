export interface Establecimiento {
  id: string;
  empresaId: string;
  empresaRazonSocial: string;
  codigo: string;
  nombre: string | null;
  direccion: string | null;
  activo: boolean;
}

export interface EstablecimientoRequest {
  codigo: string;
  nombre: string;
  direccion: string;
}
