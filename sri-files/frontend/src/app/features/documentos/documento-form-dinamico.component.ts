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

  readonly reiniciar = output<void>();
  readonly enviar = output<void>();
  readonly agregarItem = output<string>();
  readonly quitarItem = output<{ sectionName: string; index: number }>();

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
}
