import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { AccessControlService } from '../../core/auth/access-control.service';
import { AuthStore } from '../../core/auth/auth.store';
import { AppUiService } from '../../core/services/app-ui.service';

type SidebarMenuItem = {
  label: string;
  icon: string;
  route: string;
  title: string;
  permission?: string;
  anyPermissions?: string[];
};

type SidebarMenuGroup = {
  id: string;
  heading: string;
  items: SidebarMenuItem[];
};

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  protected readonly accessControl = inject(AccessControlService);
  protected readonly authStore = inject(AuthStore);
  protected readonly ui = inject(AppUiService);
  private readonly router = inject(Router);
  private readonly expandedGroups = signal<Record<string, boolean>>({
    general: true,
    documentos: false,
    administracion: false,
    control: false
  });

  private readonly menuGroups: SidebarMenuGroup[] = [
    {
      id: 'general',
      heading: 'General',
      items: [{ label: 'Dashboard', icon: '📊', route: '/dashboard', title: 'Dashboard' }]
    },
    {
      id: 'documentos',
      heading: 'Documentos',
      items: [
        { label: 'Documentos', icon: '📁', route: '/documentos', title: 'Documentos' },
        { label: 'Facturacion', icon: '🧾', route: '/facturacion', title: 'Facturacion' },
        { label: 'Retenciones', icon: '📌', route: '/retenciones', title: 'Retenciones' },
        { label: 'Guias remision', icon: '🚚', route: '/guias-remision', title: 'Guias de remision' },
        { label: 'Notas credito', icon: '💸', route: '/notas-credito', title: 'Notas de credito' },
        { label: 'Notas debito', icon: '💳', route: '/notas-debito', title: 'Notas de debito' },
        { label: 'Liquidaciones', icon: '🛒', route: '/liquidaciones-compra', title: 'Liquidaciones de compra' }
      ]
    },
    {
      id: 'administracion',
      heading: 'Administracion',
      items: [
        {
          label: 'Catalogos operativos',
          icon: '🗂️',
          route: '/catalogos',
          title: 'Catalogos operativos',
          permission: 'CATALOGO_ADMINISTRAR'
        },
        { label: 'Usuarios', icon: '👥', route: '/seguridad/usuarios', title: 'Usuarios', permission: 'USUARIO_VER' },
        {
          label: 'Roles y permisos',
          icon: '🛡️',
          route: '/seguridad/roles',
          title: 'Roles y permisos',
          permission: 'ROL_VER'
        },
        {
          label: 'Certificados',
          icon: '🔐',
          route: '/administracion/certificados',
          title: 'Certificados',
          permission: 'CERTIFICADO_ADMINISTRAR'
        },
        {
          label: 'Configuracion SRI',
          icon: '⚙️',
          route: '/administracion/configuracion-sri',
          title: 'Configuracion SRI',
          permission: 'CONFIGURACION_CORREO_ADMINISTRAR'
        },
        {
          label: 'Correo',
          icon: '✉️',
          route: '/administracion/configuracion-correo',
          title: 'Correo',
          permission: 'CONFIGURACION_CORREO_ADMINISTRAR'
        },
        {
          label: 'Plantillas RIDE',
          icon: '📄',
          route: '/administracion/plantillas-ride',
          title: 'Plantillas RIDE',
          permission: 'CERTIFICADO_ADMINISTRAR'
        },
        {
          label: 'Catalogos comerciales',
          icon: '🧾',
          route: '/administracion/catalogos-comerciales',
          title: 'Catalogos comerciales',
          permission: 'CATALOGO_ADMINISTRAR'
        }
      ]
    },
    {
      id: 'control',
      heading: 'Control',
      items: [
        { label: 'Errores', icon: '⚠️', route: '/control/errores', title: 'Errores', permission: 'CONTROL_ERRORES_VER' },
        {
          label: 'Control correos',
          icon: '📨',
          route: '/control/correos',
          title: 'Control de correos',
          permission: 'CONTROL_CORREOS_VER'
        },
        { label: 'Monitoreo', icon: '📡', route: '/control/monitoreo', title: 'Monitoreo', permission: 'MONITOREO_VER' },
        {
          label: 'Auditoria',
          icon: '🧭',
          route: '/control/auditoria',
          title: 'Auditoria',
          anyPermissions: ['DOCUMENTO_AUDITORIA_VER', 'USUARIO_AUDITORIA_VER', 'ROL_AUDITORIA_VER', 'EMPRESA_AUDITORIA_VER']
        },
        {
          label: 'Auditoria documentos',
          icon: '📚',
          route: '/control/auditoria-documentos',
          title: 'Auditoria documental',
          permission: 'DOCUMENTO_AUDITORIA_VER'
        },
        {
          label: 'Auditoria usuarios',
          icon: '🙍',
          route: '/control/auditoria-usuarios',
          title: 'Auditoria de usuarios',
          permission: 'USUARIO_AUDITORIA_VER'
        },
        {
          label: 'Auditoria roles',
          icon: '🪪',
          route: '/control/auditoria-roles',
          title: 'Auditoria de roles',
          permission: 'ROL_AUDITORIA_VER'
        },
        {
          label: 'Auditoria empresas',
          icon: '🏢',
          route: '/control/auditoria-empresas',
          title: 'Auditoria de empresas',
          permission: 'EMPRESA_AUDITORIA_VER'
        }
      ]
    }
  ];

  protected readonly visibleGroups = computed(() =>
    this.menuGroups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => this.hasAccess(item))
      }))
      .filter((group) => group.items.length > 0)
  );

  constructor() {
    this.syncExpandedGroupsWithActiveRoute();
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.syncExpandedGroupsWithActiveRoute());
  }

  protected toggleGroup(groupId: string): void {
    if (this.ui.sidebarCollapsed()) {
      return;
    }

    this.expandedGroups.update((state) => ({
      ...state,
      [groupId]: !state[groupId]
    }));
  }

  protected isGroupExpanded(groupId: string): boolean {
    return this.ui.sidebarCollapsed() || this.expandedGroups()[groupId] !== false;
  }

  protected isGroupActive(group: SidebarMenuGroup): boolean {
    return group.items.some((item) => this.router.isActive(item.route, {
      paths: 'subset',
      queryParams: 'ignored',
      matrixParams: 'ignored',
      fragment: 'ignored'
    }));
  }

  protected trackByGroup(_: number, group: SidebarMenuGroup): string {
    return group.id;
  }

  protected trackByItem(_: number, item: SidebarMenuItem): string {
    return item.route;
  }

  private hasAccess(item: SidebarMenuItem): boolean {
    if (item.permission) {
      return this.accessControl.hasPermission(item.permission);
    }

    if (item.anyPermissions?.length) {
      return this.accessControl.hasAnyPermission(item.anyPermissions);
    }

    return true;
  }

  private syncExpandedGroupsWithActiveRoute(): void {
    const groups = this.visibleGroups();

    this.expandedGroups.update((state) => {
      const nextState = { ...state };

      groups.forEach((group) => {
        const hasActiveRoute = group.items.some((item) => this.isRouteActive(item.route));
        nextState[group.id] = hasActiveRoute ? true : state[group.id] ?? false;
      });

      return nextState;
    });
  }

  private isRouteActive(route: string): boolean {
    return this.router.isActive(route, {
      paths: 'subset',
      queryParams: 'ignored',
      matrixParams: 'ignored',
      fragment: 'ignored'
    });
  }
}
