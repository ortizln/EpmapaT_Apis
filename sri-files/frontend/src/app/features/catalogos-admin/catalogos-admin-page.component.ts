import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { EmpresasPageComponent } from '../empresas/empresas-page.component';
import { EstablecimientosPageComponent } from '../establecimientos/establecimientos-page.component';
import { PuntosEmisionPageComponent } from '../puntos-emision/puntos-emision-page.component';
import { SecuencialesPageComponent } from '../secuenciales/secuenciales-page.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

type CatalogoTab = 'empresas' | 'establecimientos' | 'puntos-emision' | 'secuenciales';

interface CatalogoTabItem {
  id: CatalogoTab;
  label: string;
  description: string;
}

@Component({
  selector: 'app-catalogos-admin-page',
  standalone: true,
  imports: [
    CommonModule,
    PageHeaderComponent,
    EmpresasPageComponent,
    EstablecimientosPageComponent,
    PuntosEmisionPageComponent,
    SecuencialesPageComponent
  ],
  templateUrl: './catalogos-admin-page.component.html',
  styleUrl: './catalogos-admin-page.component.scss'
})
export class CatalogosAdminPageComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly tabs: CatalogoTabItem[] = [
    {
      id: 'empresas',
      label: 'Empresas',
      description: 'Datos del emisor y configuracion SRI/correo.'
    },
    {
      id: 'establecimientos',
      label: 'Establecimientos',
      description: 'Sucursales operativas por empresa.'
    },
    {
      id: 'puntos-emision',
      label: 'Puntos de emision',
      description: 'Codigos operativos por establecimiento.'
    },
    {
      id: 'secuenciales',
      label: 'Secuenciales',
      description: 'Numeracion activa por tipo documental.'
    }
  ];

  protected readonly activeTab$ = this.route.queryParamMap.pipe(
    map((params) => this.resolveTab(params.get('tab')))
  );

  protected selectTab(tab: CatalogoTab): void {
    this.router.navigate(['/catalogos'], {
      queryParams: { tab }
    });
  }

  private resolveTab(tab: string | null): CatalogoTab {
    if (tab === 'empresas' || tab === 'establecimientos' || tab === 'puntos-emision' || tab === 'secuenciales') {
      return tab;
    }

    const routeTab = this.route.snapshot.data['tab'];
    if (
      routeTab === 'empresas' ||
      routeTab === 'establecimientos' ||
      routeTab === 'puntos-emision' ||
      routeTab === 'secuenciales'
    ) {
      return routeTab;
    }

    return 'empresas';
  }
}
