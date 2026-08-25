import { TipoDocumento } from './documento.model';

export interface PlantillaRide {
  uuid: string;
  empresaId: string;
  tipoDocumento: TipoDocumento;
  nombre: string;
  version: string;
  predeterminada: boolean;
  activa: boolean;
  nombreArchivo: string | null;
  createdAt: string | null;
}

export interface PlantillaRideCreateRequest {
  tipoDocumento: TipoDocumento;
  nombre: string;
  version: string;
  predeterminada: boolean;
  activa: boolean;
}

export interface VerificacionPlantillaRideResponse {
  plantillaId: string;
  valida: boolean;
  mensaje: string;
  tipoDocumento: TipoDocumento;
  nombreArchivo: string | null;
}

export interface RideContratoCampo {
  nombre: string;
  valorEjemplo: string | null;
}

export interface RideContratoSeccion {
  nombre: string;
  campos: RideContratoCampo[];
}

export interface RideContratoDocumento {
  documentoId: string;
  empresaId: string;
  tipoDocumento: TipoDocumento;
  plantillaPredeterminadaId: string | null;
  parametros: RideContratoCampo[];
  detail: RideContratoSeccion;
  recursos: RideContratoCampo[];
}
