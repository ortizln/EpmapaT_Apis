import { Component, effect, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { CompanyContextService } from '../../core/services/company-context.service';
import {
  DocumentoCampoContrato,
  DocumentoContrato,
  DocumentoRecepcionRequest,
  DocumentoRecepcionResponse,
  DocumentoSeccionContrato,
  TipoDocumento
} from '../../models/documento.model';
import { DocumentoContratoService } from '../../core/services/documento-contrato.service';
import { DocumentoFormDinamicoComponent } from './documento-form-dinamico.component';
import { AppAlertService } from '../../core/services/app-alert.service';
import { CatalogosComercialesService } from '../../core/services/catalogos-comerciales.service';
import {
  ClienteCatalogo,
  FormaPagoCatalogo,
  IvaTarifaCatalogo,
  ProductoCatalogo
} from '../../models/catalogos-comerciales.model';

type DynamicFormGroup = UntypedFormGroup;

@Component({
  selector: 'app-documento-recepcion-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, DocumentoFormDinamicoComponent],
  templateUrl: './documento-recepcion-page.component.html',
  styleUrl: './documento-recepcion-page.component.scss'
})
export class DocumentoRecepcionPageComponent {
  private readonly documentoContratoService = inject(DocumentoContratoService);
  protected readonly companyContext = inject(CompanyContextService);
  private readonly fb = inject(UntypedFormBuilder);
  private readonly router = inject(Router);
  private readonly alerts = inject(AppAlertService);
  private readonly catalogosComercialesService = inject(CatalogosComercialesService);

  readonly forcedTipoDocumento = input<TipoDocumento | null>(null);
  readonly pageTitle = input('Nuevo documento');
  readonly pageDescription = input('Recepcion dinamica de comprobantes usando el contrato entregado por el backend.');
  readonly backRoute = input('/documentos');

  protected readonly tiposDocumento: TipoDocumento[] = [
    'FACTURA',
    'RETENCION',
    'GUIA_REMISION',
    'NOTA_CREDITO',
    'NOTA_DEBITO',
    'LIQUIDACION_COMPRA'
  ];

  protected tipoSeleccionado: TipoDocumento = 'FACTURA';
  protected contrato: DocumentoContrato | null = null;
  protected loadingContrato = false;
  protected errorContrato = '';
  protected submitting = false;
  protected errorRecepcion = '';
  protected recepcionResponse: DocumentoRecepcionResponse | null = null;
  protected seccionesSimples: DocumentoSeccionContrato[] = [];
  protected seccionesMultiples: DocumentoSeccionContrato[] = [];
  protected clientesCatalogo: ClienteCatalogo[] = [];
  protected productosCatalogo: ProductoCatalogo[] = [];
  protected formasPagoCatalogo: FormaPagoCatalogo[] = [];
  protected ivaCatalogo: IvaTarifaCatalogo[] = [];
  private syncingTotales = false;

  protected readonly form = this.fb.group({
    externalId: this.fb.control(''),
    correoEnviar: this.fb.control(false),
    correoDestinatarios: this.fb.control('')
  });

  constructor() {
    this.companyContext.cargarEmpresas();
    this.cargarContrato(this.tipoSeleccionado);

    effect(
      () => {
        const forcedTipo = this.forcedTipoDocumento();
        if (!forcedTipo || forcedTipo === this.tipoSeleccionado) {
          return;
        }

        this.tipoSeleccionado = forcedTipo;
        this.cargarContrato(forcedTipo);
      },
      { allowSignalWrites: true }
    );

    effect(
      () => {
        this.companyContext.empresaActiva()?.id;
        this.syncEmisorWithActiveCompany();
        this.cargarCatalogosComerciales();
      },
      { allowSignalWrites: true }
    );
  }

  protected volverAlListado(): void {
    this.router.navigate([this.backRoute()]);
  }

  protected onTipoChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as TipoDocumento;
    this.tipoSeleccionado = value;
    this.cargarContrato(value);
  }

  protected resetForm(): void {
    if (!this.contrato) {
      return;
    }

    this.recepcionResponse = null;
    this.errorRecepcion = '';
    this.form.reset({
      externalId: '',
      correoEnviar: false,
      correoDestinatarios: ''
    });
    this.mountContractSections(this.contrato);
  }

  protected submit(): void {
    if (!this.contrato || this.submitting) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alerts.warning('Formulario incompleto', 'Revisa los campos requeridos antes de enviar el documento.');
      return;
    }

    this.submitting = true;
    this.errorRecepcion = '';
    this.recepcionResponse = null;

    this.documentoContratoService
      .recibirDocumento(this.buildPayload())
      .pipe(
        catchError(() => {
          this.errorRecepcion =
            'No fue posible registrar el documento. Revisa los datos requeridos y confirma que el backend este activo.';
          this.alerts.error('Recepcion fallida', 'El backend no pudo registrar el documento enviado.');
          return of(null);
        }),
        finalize(() => {
          this.submitting = false;
        })
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        this.recepcionResponse = response;
        this.alerts.success('Documento recepcionado', `El documento fue registrado con ID ${response.id}.`);
      });
  }

  protected addMultipleItem(sectionName: string): void {
    const seccion = this.seccionesMultiples.find((item) => item.nombre === sectionName);
    if (!seccion) {
      return;
    }

    this.getMultipleSectionArray(sectionName).push(this.buildSectionItemGroup(seccion.campos));
    this.recalcularTotalesDocumento();
  }

  protected removeMultipleItem(sectionName: string, index: number): void {
    const formArray = this.getMultipleSectionArray(sectionName);
    if (formArray.length > 1) {
      formArray.removeAt(index);
      this.recalcularTotalesDocumento();
    }
  }

  private cargarContrato(tipoDocumento: TipoDocumento): void {
    this.loadingContrato = true;
    this.errorContrato = '';
    this.recepcionResponse = null;
    this.errorRecepcion = '';

    this.documentoContratoService
      .obtenerContrato(tipoDocumento)
      .pipe(
        catchError(() => {
          this.errorContrato =
            'No fue posible obtener el contrato desde el backend. Verifica que la API este disponible en /api/v1.';
          this.contrato = null;
          this.seccionesSimples = [];
          this.seccionesMultiples = [];
          this.alerts.error('Contrato no disponible', 'No se pudo cargar la estructura del documento seleccionado.');
          return of(null);
        }),
        finalize(() => {
          this.loadingContrato = false;
        })
      )
      .subscribe((contrato) => {
        this.contrato = contrato;
        if (contrato) {
          this.mountContractSections(contrato);
        }
      });
  }

  private mountContractSections(contrato: DocumentoContrato): void {
    this.removeDynamicControls();
    this.seccionesSimples = contrato.secciones.filter((seccion) => !seccion.multiple);
    this.seccionesMultiples = contrato.secciones.filter((seccion) => seccion.multiple);

    for (const seccion of this.seccionesSimples) {
      this.form.addControl(seccion.nombre, this.buildSectionItemGroup(seccion.campos));
    }

    for (const seccion of this.seccionesMultiples) {
      this.form.addControl(seccion.nombre, this.fb.array([this.buildSectionItemGroup(seccion.campos)]));
    }

    this.syncEmisorWithActiveCompany();
    this.aplicarDefaultsPorTipoDocumento();
    this.recalcularTotalesDocumento();
  }

  private removeDynamicControls(): void {
    for (const key of Object.keys(this.form.controls)) {
      if (!['externalId', 'correoEnviar', 'correoDestinatarios'].includes(key)) {
        this.form.removeControl(key);
      }
    }
  }

  private buildSectionItemGroup(campos: DocumentoCampoContrato[]): DynamicFormGroup {
    const group: Record<string, FormControl<string | null>> = {};

    for (const campo of campos) {
      const validators = campo.requerido ? [Validators.required] : [];
      const key = campo.nombre.toLowerCase();
      const tipo = campo.tipo.toLowerCase();

      if (key.includes('mail') || key.includes('correo') || key.includes('email')) {
        validators.push(Validators.email);
      }

      if (
        key.includes('porcentaje') ||
        key.includes('valor') ||
        key.includes('importe') ||
        key.includes('cantidad') ||
        key.includes('precio') ||
        key.includes('descuento') ||
        tipo.includes('decimal') ||
        tipo.includes('number')
      ) {
        validators.push(Validators.min(0));
      }

      if (key.includes('ruc')) {
        validators.push(Validators.pattern(/^\d{13}$/));
      }

      if (key.includes('identificacion') || key.includes('cedula')) {
        validators.push(Validators.pattern(/^\d{10,13}$/));
      }

      group[campo.nombre] = new FormControl(campo.ejemplo ?? '', validators);
    }

    const formGroup = new FormGroup(group);
    formGroup.valueChanges.subscribe(() => {
      this.recalcularTotalesDocumento();
    });
    return formGroup;
  }

  private buildPayload(): DocumentoRecepcionRequest {
    const correoDestinatarios = (this.form.controls['correoDestinatarios'].value ?? '')
      .split(',')
      .map((item: string) => item.trim())
      .filter(Boolean);

    return {
      tipoDocumento: this.tipoSeleccionado,
      externalId: this.form.controls['externalId'].value || null,
      emisor: this.getSimpleSectionValue('emisor'),
      receptor: this.getSimpleSectionValue('receptor'),
      documento: this.getSimpleSectionValue('documento'),
      detalles: this.getMultipleSectionValue('detalles'),
      destinatarios: this.getMultipleSectionValue('destinatarios'),
      motivos: this.getMultipleSectionValue('motivos'),
      impuestos: this.getMultipleSectionValue('impuestos'),
      informacionAdicional: this.getSimpleSectionValue('informacionAdicional'),
      correo: {
        enviar: this.form.controls['correoEnviar'].value ?? false,
        destinatarios: correoDestinatarios
      }
    };
  }

  private getMultipleSectionArray(sectionName: string): UntypedFormArray {
    return this.form.get(sectionName) as UntypedFormArray;
  }

  private getSimpleSectionValue(sectionName: string): Record<string, unknown> {
    const control = this.form.get(sectionName);
    if (!control || !(control instanceof FormGroup)) {
      return {};
    }

    return this.normalizeObject(control.getRawValue());
  }

  private getMultipleSectionValue(sectionName: string): Record<string, unknown>[] {
    const control = this.form.get(sectionName);
    if (!control || !(control instanceof UntypedFormArray)) {
      return [];
    }

    return control.controls
      .map((item: AbstractControl) => this.normalizeObject(item.getRawValue()))
      .filter((value: Record<string, unknown>) => Object.keys(value).length > 0);
  }

  private normalizeObject(value: Record<string, unknown>): Record<string, unknown> {
    return Object.entries(value).reduce<Record<string, unknown>>((acc, [key, current]) => {
      if (typeof current !== 'string') {
        acc[key] = current;
        return acc;
      }

      const trimmed = current.trim();
      if (!trimmed) {
        return acc;
      }

      if (!Number.isNaN(Number(trimmed))) {
        acc[key] = trimmed.includes('.') ? Number.parseFloat(trimmed) : Number.parseInt(trimmed, 10);
        return acc;
      }

      acc[key] = trimmed;
      return acc;
    }, {});
  }

  private syncEmisorWithActiveCompany(): void {
    const empresaActiva = this.companyContext.empresaActiva();
    const emisorGroup = this.form.get('emisor');
    const documentoGroup = this.form.get('documento');
    if (!empresaActiva || !(emisorGroup instanceof FormGroup)) {
      return;
    }

    if (emisorGroup.contains('ruc')) {
      emisorGroup.get('ruc')?.setValue(empresaActiva.ruc);
    }

    if (emisorGroup.contains('ambiente')) {
      emisorGroup.get('ambiente')?.setValue(String(empresaActiva.ambienteSri ?? 1));
    }

    if (documentoGroup instanceof FormGroup && documentoGroup.contains('formaPago')) {
      const currentValue = `${documentoGroup.get('formaPago')?.value ?? ''}`.trim();
      const defaultFormaPago = this.formasPagoCatalogo[0]?.codigo ?? '';
      if (!currentValue && defaultFormaPago) {
        documentoGroup.get('formaPago')?.setValue(defaultFormaPago);
      }
    }
  }

  private aplicarDefaultsPorTipoDocumento(): void {
    const documentoGroup = this.form.get('documento');
    if (!(documentoGroup instanceof FormGroup)) {
      return;
    }

    const hoyIso = this.getTodayIso();
    const hoyLatam = this.getTodayLatam();

    if (documentoGroup.contains('fechaEmision')) {
      const fechaEmisionActual = `${documentoGroup.get('fechaEmision')?.value ?? ''}`.trim();
      if (!fechaEmisionActual) {
        documentoGroup.get('fechaEmision')?.setValue(hoyIso);
      }
    }

    if (this.tipoSeleccionado !== 'GUIA_REMISION') {
      return;
    }

    if (documentoGroup.contains('fechaInicioTransporte')) {
      const actual = `${documentoGroup.get('fechaInicioTransporte')?.value ?? ''}`.trim();
      if (!actual) {
        documentoGroup.get('fechaInicioTransporte')?.setValue(hoyLatam);
      }
    }

    if (documentoGroup.contains('fechaFinTransporte')) {
      const actual = `${documentoGroup.get('fechaFinTransporte')?.value ?? ''}`.trim();
      if (!actual) {
        documentoGroup.get('fechaFinTransporte')?.setValue(hoyLatam);
      }
    }

    if (documentoGroup.contains('motivoTraslado')) {
      const actual = `${documentoGroup.get('motivoTraslado')?.value ?? ''}`.trim();
      if (!actual) {
        documentoGroup.get('motivoTraslado')?.setValue('Entrega programada');
      }
    }

    const destinatariosArray = this.form.get('destinatarios');
    if (!(destinatariosArray instanceof UntypedFormArray)) {
      return;
    }

    for (const control of destinatariosArray.controls) {
      if (!(control instanceof FormGroup)) {
        continue;
      }

      if (control.contains('codDocSustento')) {
        const actual = `${control.get('codDocSustento')?.value ?? ''}`.trim();
        if (!actual) {
          control.get('codDocSustento')?.setValue('01');
        }
      }

      if (control.contains('motivoTraslado')) {
        const actual = `${control.get('motivoTraslado')?.value ?? ''}`.trim();
        if (!actual) {
          control.get('motivoTraslado')?.setValue('Entrega programada');
        }
      }
    }
  }

  private recalcularTotalesDocumento(): void {
    if (this.syncingTotales) {
      return;
    }

    if (this.tipoSeleccionado === 'NOTA_DEBITO') {
      this.recalcularNotaDebito();
      return;
    }

    if (!['FACTURA', 'LIQUIDACION_COMPRA', 'NOTA_CREDITO'].includes(this.tipoSeleccionado)) {
      return;
    }

    const documentoGroup = this.form.get('documento');
    const detallesArray = this.form.get('detalles');
    if (!(documentoGroup instanceof FormGroup) || !(detallesArray instanceof UntypedFormArray) || detallesArray.length === 0) {
      return;
    }

    let subtotal = 0;
    let impuestos = 0;

    for (const control of detallesArray.controls) {
      if (!(control instanceof FormGroup)) {
        continue;
      }

      const cantidad = this.asNumber(control.get('cantidad')?.value, 0);
      const precioUnitario = this.asNumber(control.get('precioUnitario')?.value, 0);
      const descuento = this.asNumber(control.get('descuento')?.value, 0);
      const base = Math.max(0, cantidad * precioUnitario - descuento);
      const porcentajeIva = this.resolverPorcentajeIva(control.get('codigoPorcentaje')?.value);
      const valorImpuesto = this.round2(base * (porcentajeIva / 100));
      const totalLinea = this.round2(base);

      subtotal += totalLinea;
      impuestos += valorImpuesto;

      this.syncingTotales = true;
      if (control.contains('precioTotalSinImpuesto')) {
        control.get('precioTotalSinImpuesto')?.setValue(this.formatNumber(totalLinea), { emitEvent: false });
      }
      if (control.contains('baseImponible')) {
        control.get('baseImponible')?.setValue(this.formatNumber(totalLinea), { emitEvent: false });
      }
      if (control.contains('valorImpuesto')) {
        control.get('valorImpuesto')?.setValue(this.formatNumber(valorImpuesto), { emitEvent: false });
      }
      this.syncingTotales = false;
    }

    const total = subtotal + impuestos;
    this.syncingTotales = true;
    if (documentoGroup.contains('subtotal')) {
      documentoGroup.get('subtotal')?.setValue(this.formatNumber(subtotal), { emitEvent: false });
    }
    if (documentoGroup.contains('impuestos')) {
      documentoGroup.get('impuestos')?.setValue(this.formatNumber(impuestos), { emitEvent: false });
    }
    if (documentoGroup.contains('total')) {
      documentoGroup.get('total')?.setValue(this.formatNumber(total), { emitEvent: false });
    }
    this.syncingTotales = false;
  }

  private recalcularNotaDebito(): void {
    const documentoGroup = this.form.get('documento');
    const motivosArray = this.form.get('motivos');
    if (!(documentoGroup instanceof FormGroup) || !(motivosArray instanceof UntypedFormArray) || motivosArray.length === 0) {
      return;
    }

    let subtotal = 0;
    for (const control of motivosArray.controls) {
      if (!(control instanceof FormGroup)) {
        continue;
      }
      subtotal += this.asNumber(control.get('valor')?.value, 0);
    }

    const impuestos = this.asNumber(documentoGroup.get('impuestos')?.value, 0);
    const total = subtotal + impuestos;

    this.syncingTotales = true;
    if (documentoGroup.contains('subtotal')) {
      documentoGroup.get('subtotal')?.setValue(this.formatNumber(subtotal), { emitEvent: false });
    }
    if (documentoGroup.contains('total')) {
      documentoGroup.get('total')?.setValue(this.formatNumber(total), { emitEvent: false });
    }
    this.syncingTotales = false;
  }

  private cargarCatalogosComerciales(): void {
    const empresaId = this.companyContext.empresaActiva()?.id;
    if (!empresaId) {
      this.clientesCatalogo = [];
      this.productosCatalogo = [];
      this.formasPagoCatalogo = [];
      this.ivaCatalogo = [];
      return;
    }

    this.catalogosComercialesService.listarClientes(empresaId).subscribe((items) => {
      this.clientesCatalogo = items.filter((item) => item.activo);
    });
    this.catalogosComercialesService.listarProductos(empresaId).subscribe((items) => {
      this.productosCatalogo = items.filter((item) => item.activo);
    });
    this.catalogosComercialesService.listarFormasPago(empresaId).subscribe((items) => {
      this.formasPagoCatalogo = items.filter((item) => item.activo);
      this.syncEmisorWithActiveCompany();
    });
    this.catalogosComercialesService.listarIva(empresaId).subscribe((items) => {
      this.ivaCatalogo = items.filter((item) => item.activo);
      this.recalcularTotalesDocumento();
    });
  }

  private resolverPorcentajeIva(codigoPorcentaje: unknown): number {
    const codigo = typeof codigoPorcentaje === 'string' ? codigoPorcentaje.trim() : '';
    if (!codigo) {
      return 0;
    }

    const tarifa = this.ivaCatalogo.find((item) => item.codigo === codigo);
    return tarifa ? Number(tarifa.porcentaje) : 0;
  }

  private asNumber(value: unknown, fallback: number): number {
    if (typeof value === 'number') {
      return Number.isFinite(value) ? value : fallback;
    }
    if (typeof value === 'string') {
      const normalized = Number(value.trim());
      return Number.isFinite(normalized) ? normalized : fallback;
    }
    return fallback;
  }

  private round2(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  private formatNumber(value: number): string {
    return this.round2(value).toFixed(2);
  }

  private getTodayIso(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const day = `${now.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private getTodayLatam(): string {
    const now = new Date();
    const day = `${now.getDate()}`.padStart(2, '0');
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const year = now.getFullYear();
    return `${day}/${month}/${year}`;
  }
}
