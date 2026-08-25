export type RecursoEmpresaTipo = 'LOGO_PRINCIPAL' | 'LOGO_SECUNDARIO' | 'MARCA_AGUA';

export interface RecursoEmpresa {
  uuid: string;
  empresaId: string;
  tipo: RecursoEmpresaTipo;
  nombre: string;
  nombreArchivo: string;
  mimeType: string | null;
  activo: boolean;
  createdAt: string | null;
}
