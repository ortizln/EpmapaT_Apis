import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { AbstractControl, ReactiveFormsModule, UntypedFormArray, UntypedFormGroup } from '@angular/forms';
import {
  DocumentoCampoContrato,
  DocumentoContrato,
  DocumentoRecepcionResponse,
  DocumentoSeccionContrato
} from '../../models/documento.model';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';
import {
  ClienteCatalogo,
  FormaPagoCatalogo,
  IvaTarifaCatalogo,
  ProductoCatalogo
} from '../../models/catalogos-comerciales.model';

type DynamicFormGroup = UntypedFormGroup;
type InputKind = 'text' | 'number' | 'date' | 'email' | 'select' | 'textarea';

interface CampoOption {
  value: string;
  label: string;
}

interface CampoUiConfig {
  input: InputKind;
  options?: CampoOption[];
  min?: number;
  max?: number;
  step?: string;
  rows?: number;
}

@Component({
  selector: 'app-documento-form-dinamico',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StatusChipComponent],
  templateUrl: './documento-form-dinamico.component.html',
  styleUrl: './documento-form-dinamico.component.scss'
})
export class DocumentoFormDinamicoComponent {
  readonly contrato = input<DocumentoContrato | null>(null);
  readonly form = input.required<UntypedFormGroup>();
  readonly seccionesSimples = input<DocumentoSeccionContrato[]>([]);
  readonly seccionesMultiples = input<DocumentoSeccionContrato[]>([]);
  readonly recepcionResponse = input<DocumentoRecepcionResponse | null>(null);
  readonly errorRecepcion = input('');
  readonly submitting = input(false);
  readonly loadingContrato = input(false);
  readonly clientesCatalogo = input<ClienteCatalogo[]>([]);
  readonly productosCatalogo = input<ProductoCatalogo[]>([]);
  readonly formasPagoCatalogo = input<FormaPagoCatalogo[]>([]);
  readonly ivaCatalogo = input<IvaTarifaCatalogo[]>([]);

  readonly reiniciar = output<void>();
  readonly enviar = output<void>();
  readonly agregarItem = output<string>();
  readonly quitarItem = output<{ sectionName: string; index: number }>();
  protected xmlRetencionNombre = '';
  protected xmlRetencionError = '';
  protected xmlRetencionPreview = '';
  protected xmlRetencionMetadata: Array<{ label: string; value: string }> = [];
  protected xmlRetencionWarnings: string[] = [];
  protected xmlRetencionValidationErrors: string[] = [];

  private readonly sriCatalogs: Record<string, CampoOption[]> = {
    ambiente: [
      { value: '1', label: 'Pruebas' },
      { value: '2', label: 'Produccion' }
    ],
    tipoEmision: [{ value: '1', label: 'Normal' }],
    obligadoContabilidad: [
      { value: 'SI', label: 'Si' },
      { value: 'NO', label: 'No' }
    ],
    llevaContabilidad: [
      { value: 'SI', label: 'Si' },
      { value: 'NO', label: 'No' }
    ],
    tipoIdentificacionComprador: [
      { value: '04', label: 'RUC' },
      { value: '05', label: 'Cedula' },
      { value: '06', label: 'Pasaporte' },
      { value: '07', label: 'Consumidor final' }
    ],
    tipoIdentificacion: [
      { value: '04', label: 'RUC' },
      { value: '05', label: 'Cedula' },
      { value: '06', label: 'Pasaporte' }
    ],
    moneda: [{ value: 'DOLAR', label: 'Dolar' }],
    codDoc: [
      { value: '01', label: 'Factura' },
      { value: '04', label: 'Nota de credito' },
      { value: '05', label: 'Nota de debito' },
      { value: '06', label: 'Guia de remision' },
      { value: '07', label: 'Comprobante de retencion' }
    ],
    unidadMedida: [
      { value: 'UNI', label: 'Unidad' },
      { value: 'KG', label: 'Kilogramo' },
      { value: 'LT', label: 'Litro' },
      { value: 'MTS', label: 'Metro' }
    ],
    formaPago: [
      { value: '01', label: 'Sin utilizacion del sistema financiero' },
      { value: '15', label: 'Compensacion de deudas' },
      { value: '16', label: 'Tarjeta de debito' },
      { value: '17', label: 'Dinero electronico' },
      { value: '19', label: 'Tarjeta prepago' },
      { value: '20', label: 'Otros con utilizacion del sistema financiero' }
    ]
  };

  protected formatLabel(value: string): string {
    return value
      .replace(/([A-Z])/g, ' $1')
      .replace(/[_-]+/g, ' ')
      .trim()
      .replace(/^./, (letter) => letter.toUpperCase());
  }

  protected formatSectionTitle(value: string): string {
    return this.formatLabel(value);
  }

  protected getSectionDescription(seccion: DocumentoSeccionContrato, multiple: boolean): string {
    const base =
      seccion.nombre === 'emisor'
        ? 'Datos de la empresa emisora y configuracion operativa.'
        : seccion.nombre === 'receptor'
          ? 'Datos del cliente, proveedor o destinatario del comprobante.'
          : seccion.nombre === 'documento'
            ? 'Informacion principal del comprobante y sus totales.'
            : seccion.nombre === 'detalles'
              ? 'Items o lineas que componen el documento.'
              : seccion.nombre === 'motivos'
                ? 'Motivos o conceptos que justifican el ajuste.'
                : seccion.nombre === 'destinatarios'
                  ? 'Destinatarios asociados al traslado o entrega.'
                  : 'Seccion construida dinamicamente desde el backend.';

    return multiple ? `${base} Puedes registrar varios items.` : base;
  }

  protected getTotalCampos(): number {
    const simples = this.seccionesSimples().reduce((total, seccion) => total + seccion.campos.length, 0);
    const multiples = this.seccionesMultiples().reduce((total, seccion) => total + seccion.campos.length, 0);
    return simples + multiples + 3;
  }

  protected getFieldConfig(campo: DocumentoCampoContrato): CampoUiConfig {
    const key = campo.nombre.toLowerCase();
    const exactCatalog = this.sriCatalogs[campo.nombre];
    if (exactCatalog) {
      return { input: 'select', options: exactCatalog };
    }
    if (campo.nombre === 'formaPago' && this.formasPagoCatalogo().length > 0) {
      return {
        input: 'select',
        options: this.formasPagoCatalogo().map((item) => ({
          value: item.codigo,
          label: `${item.codigo} - ${item.nombre}`
        }))
      };
    }
    if (campo.nombre === 'codigoPorcentaje' && this.ivaCatalogo().length > 0) {
      return {
        input: 'select',
        options: this.ivaCatalogo().map((item) => ({
          value: item.codigo,
          label: `${item.codigo} - ${item.nombre} (${item.porcentaje}%)`
        }))
      };
    }
    if (key.includes('direccion') || key.includes('motivo') || key.includes('descripcion')) {
      return { input: 'textarea', rows: 3 };
    }
    if (key.includes('fecha')) {
      return { input: 'date' };
    }
    if (key.includes('mail') || key.includes('correo') || key.includes('email')) {
      return { input: 'email' };
    }
    if (
      key.includes('porcentaje') ||
      key.includes('valor') ||
      key.includes('importe') ||
      key.includes('cantidad') ||
      key.includes('precio') ||
      key.includes('descuento') ||
      campo.tipo.toLowerCase().includes('decimal')
    ) {
      return { input: 'number', min: 0, step: '0.01' };
    }
    if (
      key.includes('secuencial') ||
      key.includes('plazo') ||
      key.includes('codigo') ||
      campo.tipo.toLowerCase().includes('number')
    ) {
      return { input: 'number', min: 0, step: '1' };
    }
    return { input: 'text' };
  }

  protected getSimpleSectionGroup(sectionName: string): DynamicFormGroup {
    return this.form().get(sectionName) as DynamicFormGroup;
  }

  protected getMultipleSectionArray(sectionName: string): UntypedFormArray {
    return this.form().get(sectionName) as UntypedFormArray;
  }

  protected showFieldError(group: DynamicFormGroup, fieldName: string): boolean {
    const control = group.get(fieldName);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  protected asDynamicGroup(control: AbstractControl): DynamicFormGroup {
    return control as DynamicFormGroup;
  }

  protected getFieldErrorMessage(group: DynamicFormGroup, campo: DocumentoCampoContrato): string {
    const control = group.get(campo.nombre);
    if (!control?.errors) {
      return '';
    }
    if (control.errors['required']) {
      return 'Este campo es obligatorio.';
    }
    if (control.errors['email']) {
      return 'Ingresa un correo valido.';
    }
    if (control.errors['min']) {
      return 'El valor debe ser mayor o igual al minimo permitido.';
    }
    if (control.errors['max']) {
      return 'El valor excede el maximo permitido.';
    }
    if (control.errors['pattern']) {
      return 'El formato ingresado no es valido.';
    }
    return 'Verifica el valor ingresado.';
  }

  protected hasClienteHelper(seccion: DocumentoSeccionContrato): boolean {
    return seccion.nombre === 'receptor' && this.clientesCatalogo().length > 0;
  }

  protected hasRetencionXmlHelper(seccion: DocumentoSeccionContrato): boolean {
    return this.contrato()?.tipoDocumento === 'RETENCION' && seccion.nombre === 'documento';
  }

  protected hasProductoHelper(seccion: DocumentoSeccionContrato): boolean {
    return seccion.nombre === 'detalles' && this.productosCatalogo().length > 0;
  }

  protected hasDestinatarioHelper(seccion: DocumentoSeccionContrato): boolean {
    return seccion.nombre === 'destinatarios' && this.clientesCatalogo().length > 0;
  }

  protected applyClienteToReceptor(clienteId: string): void {
    const cliente = this.clientesCatalogo().find((item) => item.id === clienteId);
    const receptorGroup = this.form().get('receptor') as DynamicFormGroup | null;
    if (!cliente || !receptorGroup) {
      return;
    }

    if (receptorGroup.contains('tipoIdentificacion')) {
      receptorGroup.get('tipoIdentificacion')?.setValue(cliente.tipoIdentificacion);
    }
    if (receptorGroup.contains('identificacion')) {
      receptorGroup.get('identificacion')?.setValue(cliente.identificacion);
    }
    if (receptorGroup.contains('razonSocial')) {
      receptorGroup.get('razonSocial')?.setValue(cliente.razonSocial);
    }
    if (receptorGroup.contains('email')) {
      receptorGroup.get('email')?.setValue(cliente.email ?? '');
    }
    if (receptorGroup.contains('direccion')) {
      receptorGroup.get('direccion')?.setValue(cliente.direccion ?? '');
    }
  }

  protected applyProductoToDetalle(sectionName: string, index: number, productoId: string): void {
    const producto = this.productosCatalogo().find((item) => item.id === productoId);
    const itemGroup = this.getMultipleSectionArray(sectionName).at(index) as DynamicFormGroup | null;
    if (!producto || !itemGroup) {
      return;
    }

    if (itemGroup.contains('codigo')) {
      itemGroup.get('codigo')?.setValue(producto.codigo);
    }
    if (itemGroup.contains('descripcion')) {
      itemGroup.get('descripcion')?.setValue(producto.nombre);
    }
    if (itemGroup.contains('precioUnitario')) {
      itemGroup.get('precioUnitario')?.setValue(String(producto.precioBase));
    }
    if (itemGroup.contains('codigoPorcentaje')) {
      const iva = this.ivaCatalogo().find((item) => Number(item.porcentaje) === Number(producto.porcentajeIva));
      itemGroup.get('codigoPorcentaje')?.setValue(iva?.codigo ?? '');
    }
  }

  protected onRetencionXmlSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    this.xmlRetencionError = '';

    if (!file) {
      this.xmlRetencionNombre = '';
      this.resetRetencionXmlAnalysis();
      return;
    }

    const isXml = file.name.toLowerCase().endsWith('.xml') || file.type === 'text/xml' || file.type === 'application/xml';
    if (!isXml) {
      this.xmlRetencionNombre = '';
      this.xmlRetencionError = 'Selecciona un archivo XML valido para la retencion.';
      this.resetRetencionXmlAnalysis();
      if (input) {
        input.value = '';
      }
      return;
    }

    this.xmlRetencionNombre = file.name;
    const reader = new FileReader();
    reader.onload = () => {
      const xml = `${reader.result ?? ''}`.trim();
      const documentoGroup = this.form().get('documento') as DynamicFormGroup | null;
      if (!documentoGroup?.contains('xml')) {
        this.xmlRetencionError = 'El contrato actual no expone el campo xml para la retencion.';
        this.resetRetencionXmlAnalysis();
        return;
      }

      if (!xml) {
        this.xmlRetencionError = 'No fue posible leer contenido del archivo XML seleccionado.';
        this.resetRetencionXmlAnalysis();
        return;
      }

      documentoGroup.get('xml')?.setValue(xml);
      this.analyzeRetencionXml(xml);
    };
    reader.onerror = () => {
      this.xmlRetencionError = 'No fue posible leer el archivo XML seleccionado.';
      this.resetRetencionXmlAnalysis();
    };
    reader.readAsText(file, 'utf-8');
  }

  protected clearRetencionXml(): void {
    this.xmlRetencionNombre = '';
    this.xmlRetencionError = '';
    this.resetRetencionXmlAnalysis();
    const documentoGroup = this.form().get('documento') as DynamicFormGroup | null;
    if (documentoGroup?.contains('xml')) {
      documentoGroup.get('xml')?.setValue('');
    }
  }

  protected applyClienteToDestinatario(sectionName: string, index: number, clienteId: string): void {
    const cliente = this.clientesCatalogo().find((item) => item.id === clienteId);
    const itemGroup = this.getMultipleSectionArray(sectionName).at(index) as DynamicFormGroup | null;
    if (!cliente || !itemGroup) {
      return;
    }

    if (itemGroup.contains('identificacion')) {
      itemGroup.get('identificacion')?.setValue(cliente.identificacion);
    }
    if (itemGroup.contains('razonSocial')) {
      itemGroup.get('razonSocial')?.setValue(cliente.razonSocial);
    }
    if (itemGroup.contains('direccion')) {
      itemGroup.get('direccion')?.setValue(cliente.direccion ?? '');
    }
  }

  private analyzeRetencionXml(xml: string): void {
    this.resetRetencionXmlAnalysis();
    this.xmlRetencionPreview = this.prettyPrintXml(xml);

    try {
      const parser = new DOMParser();
      const document = parser.parseFromString(xml, 'application/xml');
      const parserError = document.querySelector('parsererror');
      if (parserError) {
        this.xmlRetencionValidationErrors = ['XML corrupto o no parseable. Revisa la estructura del archivo seleccionado.'];
        return;
      }

      const root = document.documentElement;
      const rootName = root.localName || root.nodeName;
      const version = root.getAttribute('version')?.trim() ?? '';
      const id = root.getAttribute('id')?.trim() ?? '';
      const claveAcceso = this.firstNodeText(document, 'claveAcceso');
      const ambiente = this.firstNodeText(document, 'ambiente');
      const codDoc = this.firstNodeText(document, 'codDoc');
      const ruc = this.firstNodeText(document, 'ruc');
      const secuencial = this.firstNodeText(document, 'secuencial');
      const fechaEmision = this.firstNodeText(document, 'fechaEmision');
      const sujeto = this.firstNodeText(document, 'razonSocialSujetoRetenido');
      const periodoFiscal = this.firstNodeText(document, 'periodoFiscal');
      const impuestos = document.querySelectorAll('impuesto, retencion').length;

      this.xmlRetencionMetadata = [
        { label: 'Raiz', value: rootName || 'N/D' },
        { label: 'Version', value: version || 'N/D' },
        { label: 'Clave acceso', value: claveAcceso || 'N/D' },
        { label: 'Ambiente', value: ambiente || 'N/D' },
        { label: 'RUC', value: ruc || 'N/D' },
        { label: 'Secuencial', value: secuencial || 'N/D' },
        { label: 'Fecha emision', value: fechaEmision || 'N/D' },
        { label: 'Sujeto retenido', value: sujeto || 'N/D' },
        { label: 'Periodo fiscal', value: periodoFiscal || 'N/D' },
        { label: 'Items retencion', value: String(impuestos) }
      ];

      if (rootName !== 'comprobanteRetencion') {
        this.xmlRetencionValidationErrors.push(`Raiz invalida. Se esperaba <comprobanteRetencion> y se recibio <${rootName}>.`);
      }
      if (id !== 'comprobante') {
        this.xmlRetencionValidationErrors.push('El atributo id del comprobante debe ser "comprobante".');
      }
      if (!version) {
        this.xmlRetencionValidationErrors.push('El atributo version es obligatorio en <comprobanteRetencion>.');
      } else if (!['1.0.0', '2.0.0'].includes(version)) {
        this.xmlRetencionWarnings.push(`Version de retencion no reconocida por el validador local: ${version}.`);
      }

      this.requireXmlField(claveAcceso, 'infoTributaria.claveAcceso');
      this.requireXmlField(ambiente, 'infoTributaria.ambiente');
      this.requireXmlField(this.firstNodeText(document, 'tipoEmision'), 'infoTributaria.tipoEmision');
      this.requireXmlField(codDoc, 'infoTributaria.codDoc');
      this.requireXmlField(ruc, 'infoTributaria.ruc');
      this.requireXmlField(secuencial, 'infoTributaria.secuencial');
      this.requireXmlField(fechaEmision, 'infoCompRetencion.fechaEmision');
      this.requireXmlField(periodoFiscal, 'infoCompRetencion.periodoFiscal');
      this.requireXmlField(this.firstNodeText(document, 'tipoIdentificacionSujetoRetenido'), 'infoCompRetencion.tipoIdentificacionSujetoRetenido');
      this.requireXmlField(this.firstNodeText(document, 'identificacionSujetoRetenido'), 'infoCompRetencion.identificacionSujetoRetenido');

      if (codDoc && codDoc !== '07') {
        this.xmlRetencionValidationErrors.push(`codDoc invalido para retencion. Se esperaba 07 y se recibio ${codDoc}.`);
      }
      if (ambiente && !['1', '2'].includes(ambiente)) {
        this.xmlRetencionValidationErrors.push('ambiente invalido. Valores permitidos: 1 o 2.');
      }
      if (ruc && !/^\d{13}$/.test(ruc)) {
        this.xmlRetencionValidationErrors.push('RUC invalido. Debe tener 13 digitos.');
      }
      if (!impuestos) {
        this.xmlRetencionValidationErrors.push('La retencion no contiene impuestos retenidos.');
      }
      if (claveAcceso && !/^\d{49}$/.test(claveAcceso)) {
        this.xmlRetencionValidationErrors.push('Clave de acceso invalida. Debe tener 49 digitos.');
      }
    } catch {
      this.xmlRetencionValidationErrors = ['No fue posible analizar el XML de retencion seleccionado.'];
    }
  }

  private requireXmlField(value: string, field: string): void {
    if (!value) {
      this.xmlRetencionValidationErrors.push(`Campo obligatorio ausente: ${field}.`);
    }
  }

  private firstNodeText(document: Document, tagName: string): string {
    const node = document.getElementsByTagName(tagName).item(0);
    return node?.textContent?.trim().replace(/\s+/g, '') ?? '';
  }

  private prettyPrintXml(xml: string): string {
    const normalized = xml.replace(/>\s*</g, '><').trim();
    const parts = normalized.split(/>\s*</);
    let indent = 0;
    return parts
      .map((part, index) => {
        let current = part;
        if (index === 0) {
          current = `${current}>`;
        } else if (index === parts.length - 1) {
          current = `<${current}`;
        } else {
          current = `<${current}>`;
        }

        if (current.match(/^<\//)) {
          indent = Math.max(indent - 1, 0);
        }
        const line = `${'  '.repeat(indent)}${current}`;
        if (current.match(/^<[^!?/][^>]*[^/]>/) && !current.includes(`</`)) {
          indent += 1;
        }
        return line;
      })
      .join('\n');
  }

  private resetRetencionXmlAnalysis(): void {
    this.xmlRetencionPreview = '';
    this.xmlRetencionMetadata = [];
    this.xmlRetencionWarnings = [];
    this.xmlRetencionValidationErrors = [];
  }
}
