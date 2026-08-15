export interface PuntoEmision {
  id: string;
  establecimientoId: string;
  establecimientoCodigo: string;
  empresaId: string;
  empresaRazonSocial: string;
  codigo: string;
  nombre: string | null;
  activo: boolean;
}

export interface PuntoEmisionRequest {
  codigo: string;
  nombre: string;
}
