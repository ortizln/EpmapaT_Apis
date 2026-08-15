export interface Secuencial {
  puntoEmisionId: string;
  tipoDocumento: string;
  valorActual: number;
  activo: boolean;
}

export interface SecuencialRequest {
  valorActual: number;
  activo: boolean;
}
