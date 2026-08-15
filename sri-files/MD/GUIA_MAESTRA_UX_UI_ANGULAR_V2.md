# Guía Maestra UX/UI para Aplicación Angular

## 1. Rol

Actúa como un **Senior UX/UI Designer + Senior Frontend Developer especializado en Angular**.

Tu objetivo es mejorar exclusivamente la **presentación visual, estructura frontend, experiencia de usuario, consistencia visual y responsive design** de la aplicación existente.

La aplicación ya posee funcionalidades implementadas y funcionando.

### REGLA PRINCIPAL

> **NO CAMBIAR LA FUNCIONALIDAD EXISTENTE.**

El trabajo debe centrarse en:

* UX/UI
* Diseño visual
* Estructura HTML
* Componentización
* CSS/SCSS
* Responsive Design
* Organización visual
* Accesibilidad
* Consistencia entre pantallas
* Jerarquía visual
* Espaciado
* Tipografía
* Estados visuales

No modificar la lógica de negocio salvo que sea estrictamente necesario para soportar un comportamiento visual.

---

# 2. Objetivo general

Transformar la aplicación actual en una interfaz:

* Moderna
* Profesional
* Limpia
* Consistente
* Responsive
* Intuitiva
* Escalable
* Fácil de mantener
* Visualmente uniforme

La aplicación debe sentirse como un **producto profesional**, no como un conjunto de pantallas desarrolladas independientemente.

Todas las pantallas deben compartir el mismo lenguaje visual.

---

# 3. Principio de conservación de funcionalidad

Antes de realizar cualquier modificación, identificar claramente:

* Rutas existentes
* Componentes existentes
* Servicios
* APIs
* Modelos
* Interfaces TypeScript
* Formularios
* Validaciones
* Eventos
* Métodos
* Pipes
* Directivas
* Permisos
* Autenticación
* Tablas
* Paginación
* Filtros
* Modales
* Procesos CRUD

### No modificar:

* Endpoints
* URLs de APIs
* Servicios HTTP
* Modelos de datos
* Interfaces
* Reglas de negocio
* Cálculos
* Validaciones funcionales
* Permisos
* Autenticación
* Navegación
* Procesos CRUD
* Consultas
* Estructura de base de datos
* Lógica de negocio

### Sí se puede modificar:

* HTML
* CSS
* SCSS
* Clases visuales
* Layout
* Grid
* Flexbox
* Componentización visual
* Iconos
* Tipografía
* Espaciado
* Tamaños
* Responsive design
* Estados visuales
* Organización de formularios
* Apariencia de tablas
* Apariencia de botones
* Apariencia de inputs

---

# 4. Arquitectura visual principal

La aplicación debe utilizar una estructura global similar a:

```text
APP
│
├── HEADER
│   ├── Logo
│   ├── Botón Menu
│   ├── Breadcrumb / Título
│   ├── Acciones
│   └── Perfil de usuario
│
├── MAIN LAYOUT
│   │
│   ├── MAIN SIDEBAR
│   │   ├── Logo
│   │   ├── Menú principal
│   │   ├── Submenús
│   │   └── Usuario / acciones
│   │
│   └── MAIN CONTENT
│       ├── Page Header
│       ├── Breadcrumb
│       ├── Page Content
│       └── Componentes
│
└── FOOTER
```

---

# 5. Componentización global

Evitar construir toda la aplicación dentro de un único componente.

Crear componentes reutilizables cuando exista repetición visual.

Estructura recomendada:

```text
src/app/
│
├── core/
│   ├── services/
│   ├── guards/
│   └── interceptors/
│
├── shared/
│   │
│   ├── components/
│   │   ├── app-header/
│   │   ├── app-sidebar/
│   │   ├── app-footer/
│   │   ├── page-header/
│   │   ├── breadcrumb/
│   │   ├── data-table/
│   │   ├── search-box/
│   │   ├── form-field/
│   │   ├── confirm-dialog/
│   │   ├── loading/
│   │   └── empty-state/
│   │
│   ├── directives/
│   ├── pipes/
│   └── styles/
│
├── layout/
│   ├── main-layout/
│   └── auth-layout/
│
└── features/
    ├── modulo-1/
    ├── modulo-2/
    └── modulo-3/
```

No crear componentes innecesarios únicamente por dividir archivos.

La componentización debe mejorar:

* Reutilización
* Mantenimiento
* Consistencia
* Legibilidad
* Escalabilidad

---

# 6. Header

Crear un componente:

```text
app-header
```

Responsabilidades:

* Botón de menú
* Logo
* Nombre de aplicación
* Título de sección
* Breadcrumb cuando corresponda
* Acciones globales
* Notificaciones
* Perfil de usuario

### Botón menú

Debe controlar el estado del Sidebar.

Estados:

```text
Sidebar abierto
Sidebar colapsado
Sidebar oculto en móvil
```

El botón debe ser claramente visible y accesible.

Ejemplo conceptual:

```text
☰  LOGO / APLICACIÓN              🔔  👤
```

No modificar la lógica de navegación existente.

---

# 7. Main Sidebar

Crear un componente:

```text
app-sidebar
```

El Sidebar debe ser completamente **toggleable**.

## Estado abierto

```text
┌─────────────────────┐
│ LOGO                │
├─────────────────────┤
│ 🏠 Dashboard        │
│ 👥 Usuarios         │
│ 📄 Documentos       │
│ 📊 Reportes         │
│ ⚙ Configuración     │
└─────────────────────┘
```

## Estado colapsado

```text
┌─────┐
│ LOG │
├─────┤
│ 🏠  │
│ 👥  │
│ 📄  │
│ 📊  │
│ ⚙   │
└─────┘
```

### Requisitos

* Animación suave
* No modificar rutas
* Mantener navegación existente
* Mostrar iconos cuando esté colapsado
* Mostrar icono + texto cuando esté expandido
* Tooltip en modo colapsado
* Estado activo claramente visible
* Submenús correctamente identificados
* Responsive

---

# 8. Comportamiento responsive del Sidebar

### Desktop

Sidebar visible.

```text
HEADER
────────────────────────────────────────────
SIDEBAR │ CONTENT
SIDEBAR │ CONTENT
SIDEBAR │ CONTENT
────────────────────────────────────────────
FOOTER
```

### Desktop colapsado

```text
HEADER
────────────────────────────────────────────
S │ CONTENT
S │ CONTENT
S │ CONTENT
────────────────────────────────────────────
FOOTER
```

### Mobile

El Sidebar debe convertirse en un menú lateral tipo drawer.

```text
HEADER
────────────────────

CONTENT

────────────────────
FOOTER
```

Al presionar:

```text
☰
```

debe aparecer:

```text
┌───────────────────┐
│ MENU          ✕   │
├───────────────────┤
│ 🏠 Dashboard      │
│ 👥 Usuarios       │
│ 📄 Documentos     │
│ 📊 Reportes       │
└───────────────────┘
```

Agregar overlay cuando corresponda.

---

# 9. Main Content

Crear una estructura consistente para todas las páginas:

```text
Page
│
├── Page Header
│   ├── Título
│   ├── Descripción
│   └── Acciones
│
├── Filters / Search
│
├── Main Content
│
└── Footer Actions
```

Ejemplo:

```text
┌─────────────────────────────────────────────┐
│ Usuarios                         [+ Nuevo]  │
│ Administración de usuarios                  │
├─────────────────────────────────────────────┤
│ Buscar usuario     [____________] [Buscar] │
├─────────────────────────────────────────────┤
│ TABLA                                       │
│                                             │
│                                             │
└─────────────────────────────────────────────┘
```

---

# 10. Footer

Crear:

```text
app-footer
```

Debe ser discreto.

Incluir únicamente información relevante:

```text
© 2026 Nombre de la aplicación
Versión 1.0.0
```

No ocupar demasiado espacio vertical.

---

# 11. Sistema de diseño

Crear un lenguaje visual global.

Definir variables para:

```text
Colores
Tipografía
Border radius
Sombras
Espaciado
Alturas
Tamaños
Breakpoints
Transiciones
```

Ejemplo conceptual:

```scss
--spacing-xs
--spacing-sm
--spacing-md
--spacing-lg
--spacing-xl

--radius-sm
--radius-md
--radius-lg

--font-size-xs
--font-size-sm
--font-size-md
--font-size-lg
```

Evitar valores arbitrarios diferentes en cada componente.

---

# 12. Tamaño compacto de componentes

La aplicación debe utilizar un estilo **compacto / SM**.

La prioridad es aprovechar mejor el espacio sin perjudicar la legibilidad.

## Inputs

Preferir:

```text
height: 36px - 38px
font-size: 13px - 14px
```

## Botones

Preferir:

```text
height: 34px - 38px
font-size: 13px - 14px
padding: compacto
```

## Labels

Preferir:

```text
font-size: 12px - 13px
font-weight: 500 - 600
```

## Tablas

Utilizar:

```text
font-size: 12px - 13px
padding: compacto
```

El objetivo es obtener una interfaz profesional y eficiente en espacio.

---

# 13. Botones

Todos los botones deben compartir un sistema visual.

Tipos:

```text
Primary
Secondary
Success
Warning
Danger
Info
Ghost
Outline
Icon
```

Ejemplo:

```text
[ + Nuevo ]
[ Guardar ]
[ Cancelar ]
[ Editar ]
[ Eliminar ]
```

### Reglas

* Mismo height
* Mismo border-radius
* Misma tipografía
* Iconos consistentes
* Estados hover
* Estado active
* Estado disabled
* Estado loading

No crear botones visualmente diferentes en cada pantalla.

---

# 14. Inputs

Todos los inputs deben utilizar el mismo sistema.

Aplicar a:

* input
* select
* textarea
* datepicker
* autocomplete
* checkbox
* radio
* switch

Estructura:

```text
LABEL
INPUT
HELP / ERROR
```

Ejemplo:

```text
Nombre completo
[____________________________]

Correo electrónico
[____________________________]
El correo es obligatorio.
```

Los errores deben aparecer cerca del campo correspondiente.

---

# 15. Formularios

Los formularios deben estar visualmente estructurados.

No colocar campos de manera desordenada.

### Formulario simple

```text
┌─────────────────────────────────────┐
│ Información personal                │
├─────────────────────────────────────┤
│ Nombre              Apellido         │
│ [____________]      [____________]  │
│                                     │
│ Correo              Teléfono         │
│ [____________]      [____________]  │
│                                     │
│ Dirección                           │
│ [_______________________________]   │
├─────────────────────────────────────┤
│                    [Cancelar] [Guardar]
└─────────────────────────────────────┘
```

Utilizar CSS Grid.

Ejemplo conceptual:

```scss
.form-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 12px;
}
```

Los campos deben ocupar columnas según su importancia.

---

# 16. Formularios responsive

Desktop:

```text
Nombre              Apellido
Correo              Teléfono
Dirección
Descripción
```

Tablet:

```text
Nombre              Apellido
Correo              Teléfono
Dirección
```

Mobile:

```text
Nombre
Apellido
Correo
Teléfono
Dirección
```

Nunca permitir que los campos se desborden horizontalmente.

---

# 17. Tablas

Todas las tablas deben seguir el mismo patrón.

Características:

* Compactas
* Responsive
* Header claramente diferenciado
* Hover por fila
* Filas con altura consistente
* Acciones agrupadas
* Badges para estados
* Paginación uniforme
* Mensaje cuando no existen registros
* Loading state

Ejemplo:

```text
┌──────────────────────────────────────────────────────┐
│ Usuario │ Correo           │ Estado    │ Acciones    │
├──────────────────────────────────────────────────────┤
│ Alexis  │ alex@email.com   │ ACTIVO    │ ✏  🗑       │
│ Juan    │ juan@email.com   │ INACTIVO  │ ✏  🗑       │
└──────────────────────────────────────────────────────┘
```

---

# 18. Acciones de tabla

No colocar botones grandes dentro de las tablas.

Preferir:

```text
✏
🗑
👁
⋮
```

Usar botones icon-only cuando la acción sea evidente.

Agregar tooltip.

Para acciones destructivas utilizar confirmación.

---

# 19. Estados visuales

Todos los componentes deben contemplar:

### Loading

```text
Cargando...
```

Preferir skeleton cuando sea apropiado.

### Empty

```text
No existen registros.
```

### Error

```text
No fue posible cargar la información.
[Reintentar]
```

### Success

Mostrar feedback breve y claro.

---

# 20. Cards

Utilizar cards únicamente cuando ayuden a agrupar información.

Evitar llenar toda la aplicación de cards innecesarias.

Una card debe tener:

```text
Título
Descripción opcional
Contenido
Acciones opcionales
```

Mantener:

* Border radius consistente
* Padding consistente
* Sombra ligera
* Jerarquía visual

---

# 21. Modales

Todos los modales deben compartir:

```text
Header
Content
Footer
```

Ejemplo:

```text
┌────────────────────────────────────┐
│ Nuevo usuario                  ✕   │
├────────────────────────────────────┤
│                                    │
│ Formulario                         │
│                                    │
├────────────────────────────────────┤
│                 Cancelar   Guardar  │
└────────────────────────────────────┘
```

No cambiar la lógica de apertura/cierre existente.

---

# 22. Tipografía

Utilizar una jerarquía consistente.

### Page title

```text
20px - 24px
```

### Section title

```text
16px - 18px
```

### Body

```text
13px - 14px
```

### Small text

```text
11px - 12px
```

Evitar utilizar demasiados tamaños diferentes.

---

# 23. Espaciado

Utilizar una escala consistente.

Ejemplo:

```text
4px
8px
12px
16px
20px
24px
32px
```

No utilizar:

```text
7px
13px
17px
19px
23px
```

salvo que exista una razón específica.

---

# 24. Iconografía

Utilizar un único sistema de iconos siempre que sea posible.

No mezclar indiscriminadamente:

```text
Font Awesome
Material Icons
Bootstrap Icons
SVG personalizados
Emoji
```

Elegir un sistema y mantener consistencia.

Los iconos deben tener:

* Tamaño consistente
* Alineación correcta
* Espaciado uniforme
* Tooltip cuando no exista texto

---

# 25. Responsive Design

La aplicación debe funcionar correctamente en:

```text
Desktop
Laptop
Tablet
Mobile
```

Revisar especialmente:

* Sidebar
* Header
* Tablas
* Formularios
* Modales
* Botones
* Cards
* Menús
* Inputs

Nunca permitir:

```text
overflow horizontal
texto cortado
botones fuera de pantalla
inputs desbordados
tablas imposibles de utilizar
```

---

# 26. Accesibilidad

Aplicar buenas prácticas:

* Labels correctamente asociados
* Contraste adecuado
* Focus visible
* Navegación mediante teclado
* `aria-label` para botones de iconos
* Tooltips cuando sean necesarios
* Estados disabled claros
* Mensajes de error comprensibles

No sacrificar accesibilidad por estética.

---

# 27. Consistencia global

Antes de modificar una pantalla, revisar las demás.

Si se modifica:

```text
Button
```

la modificación debe aplicarse al sistema global.

Si se modifica:

```text
Input
```

debe aplicarse globalmente.

Si se modifica:

```text
Table
```

debe existir un patrón global.

No resolver el problema únicamente en una pantalla.

---

# 28. No duplicar estilos

Evitar:

```scss
.usuario-button {}
.cliente-button {}
.documento-button {}
.reporte-button {}
```

si visualmente cumplen la misma función.

Preferir:

```scss
.btn
.btn-primary
.btn-secondary
.btn-danger
```

De la misma manera:

```scss
.form-control
.form-label
.form-group
.table
.card
.badge
```

---

# 29. Arquitectura recomendada del Layout

El layout principal debe funcionar conceptualmente así:

```text
┌───────────────────────────────────────────────────┐
│ HEADER                                             │
├───────────────┬───────────────────────────────────┤
│               │                                   │
│    SIDEBAR    │             CONTENT               │
│               │                                   │
│               │                                   │
│               │                                   │
├───────────────┴───────────────────────────────────┤
│ FOOTER                                             │
└───────────────────────────────────────────────────┘
```

Cuando el Sidebar se colapsa:

```text
┌───────────────────────────────────────────────────┐
│ HEADER                                             │
├─────┬─────────────────────────────────────────────┤
│     │                                             │
│  S  │                  CONTENT                    │
│  I  │                                             │
│  D  │                                             │
│  E  │                                             │
│     │                                             │
├─────┴─────────────────────────────────────────────┤
│ FOOTER                                             │
└───────────────────────────────────────────────────┘
```

---

# 30. Regla para trabajar pantalla por pantalla

Antes de modificar una pantalla:

### Paso 1

Analizar:

* HTML
* TypeScript
* SCSS
* Componentes utilizados
* Servicios relacionados

### Paso 2

Determinar qué corresponde a:

```text
Layout
Componentes reutilizables
Contenido específico
```

### Paso 3

Aplicar el sistema visual global.

### Paso 4

Mejorar la estructura visual.

### Paso 5

Verificar responsive.

### Paso 6

Verificar que la funcionalidad no haya cambiado.

---

# 31. Prohibiciones

NO hacer:

```text
❌ Cambiar APIs
❌ Cambiar endpoints
❌ Cambiar servicios
❌ Cambiar modelos
❌ Cambiar rutas
❌ Cambiar lógica de negocio
❌ Eliminar funcionalidades
❌ Cambiar permisos
❌ Cambiar autenticación
❌ Cambiar consultas
❌ Cambiar comportamiento CRUD
```

Tampoco:

```text
❌ Crear diseños completamente diferentes entre módulos
❌ Usar tamaños arbitrarios
❌ Usar colores diferentes para cada pantalla
❌ Utilizar botones gigantes
❌ Utilizar formularios desordenados
❌ Sobrecargar la interfaz
❌ Utilizar demasiadas sombras
❌ Utilizar demasiados colores
```

---

# 32. Resultado esperado

El resultado final debe parecer una única aplicación diseñada bajo un **Design System profesional**.

Debe existir consistencia entre:

```text
Header
Sidebar
Footer
Page Header
Buttons
Inputs
Labels
Forms
Tables
Cards
Modals
Badges
Alerts
Pagination
Loading
Empty States
```

La aplicación debe sentirse:

> **Moderna + Profesional + Compacta + Limpia + Consistente + Responsive**

---

# 33. Prioridad de trabajo

Aplicar las mejoras en este orden:

```text
1. Layout global
2. Header
3. Sidebar
4. Footer
5. Sistema de espaciado
6. Tipografía
7. Botones
8. Inputs
9. Formularios
10. Tablas
11. Cards
12. Modales
13. Badges
14. Alertas
15. Estados loading/empty/error
16. Responsive
17. Accesibilidad
18. Refinamiento visual
```

No intentar rediseñar toda la aplicación de una sola vez.

---

# 34. Criterio final de aceptación

Antes de considerar terminado un cambio, verificar:

* [ ] La funcionalidad existente continúa funcionando.
* [ ] No se modificaron APIs.
* [ ] No se modificaron endpoints.
* [ ] No se modificaron servicios innecesariamente.
* [ ] No se modificaron modelos.
* [ ] El Sidebar puede abrirse y cerrarse.
* [ ] El menú funciona correctamente en desktop.
* [ ] El menú funciona correctamente en mobile.
* [ ] Header consistente.
* [ ] Footer consistente.
* [ ] Botones uniformes.
* [ ] Inputs uniformes.
* [ ] Labels uniformes.
* [ ] Formularios organizados.
* [ ] Tablas compactas.
* [ ] Acciones de tablas consistentes.
* [ ] Modales consistentes.
* [ ] Espaciado uniforme.
* [ ] Tipografía uniforme.
* [ ] Responsive correctamente.
* [ ] No existe overflow horizontal innecesario.
* [ ] Estados loading/empty/error están contemplados.
* [ ] La interfaz mantiene una identidad visual única.

---

# 35. Instrucción final para la IA / desarrollador

Antes de modificar código:

1. Analiza la estructura existente.
2. Identifica componentes reutilizables.
3. Identifica duplicación visual.
4. Identifica estilos inconsistentes.
5. Identifica oportunidades de componentización.
6. No cambies la funcionalidad.
7. No cambies la lógica de negocio.
8. No cambies APIs ni endpoints.
9. Implementa primero el Layout global.
10. Después implementa el Design System.
11. Después adapta los componentes existentes.
12. Finalmente adapta cada pantalla.

Cuando sea necesario crear componentes nuevos, priorizar componentes reutilizables.

El objetivo NO es simplemente "hacer que se vea bonito".

El objetivo es construir una **experiencia UX/UI coherente y un sistema frontend escalable para toda la aplicación Angular**, manteniendo intacta la funcionalidad existente.

**Principio fundamental:**

> **Primero preservar la funcionalidad. Después mejorar la experiencia. Finalmente perfeccionar la estética.**


---

# 36. MEJORA V2 — Layout fijo y scroll independiente

## Regla estructural obligatoria

La aplicación debe comportarse como una aplicación administrativa de escritorio dentro del navegador:

```text
┌───────────────────────────────────────────────────────────┐
│ HEADER FIJO                                               │
├────────────────┬──────────────────────────────────────────┤
│ SIDEBAR FIJO   │ MAIN CONTENT                            │
│                │                                          │
│ Sidebar Header │ Contenido con scroll vertical propio    │
│ ────────────── │                                          │
│ Menú           │ Tablas / formularios / dashboards       │
│ SCROLL PROPIO  │                                          │
│ ────────────── │                                          │
│ Sidebar Footer │                                          │
├────────────────┴──────────────────────────────────────────┤
│ FOOTER FIJO                                               │
└───────────────────────────────────────────────────────────┘
```

El `body` NO debe desplazarse. Solo podrán tener scroll vertical:

- `main-content`;
- `sidebar-menu`;
- `modal-body` cuando el contenido sea largo.

### SCSS base

```scss
html,
body,
app-root {
  width: 100%;
  height: 100%;
  margin: 0;
}

html,
body {
  overflow: hidden;
}

.app-shell {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  flex: 0 0 56px;
  min-height: 56px;
  z-index: 1030;
}

.app-body {
  flex: 1 1 auto;
  display: flex;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.app-main {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

.app-footer {
  flex: 0 0 34px;
  min-height: 34px;
}
```

Los tamaños pueden ajustarse al diseño existente, pero no cambiar el comportamiento.

---

# 37. MainSidebar reorganizado

Dividir visualmente `MainSidebar` en:

```text
SIDEBAR
├── sidebar-header       ← estático
├── sidebar-menu         ← scroll independiente
└── sidebar-footer       ← estático
```

HTML conceptual:

```html
<aside class="app-sidebar">
  <div class="sidebar-header">
    <!-- logo / nombre -->
  </div>

  <nav class="sidebar-menu">
    <!-- rutas y submenús existentes -->
  </nav>

  <div class="sidebar-footer">
    <!-- usuario / acciones -->
  </div>
</aside>
```

SCSS:

```scss
.app-sidebar {
  width: 260px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width .2s ease;
}

.sidebar-header,
.sidebar-footer {
  flex: 0 0 auto;
}

.sidebar-menu {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
}
```

Aunque existan 50 opciones, únicamente se desplaza el menú. Logo y usuario permanecen visibles.

Mantener intactos:

- rutas;
- permisos;
- submenús;
- eventos;
- navegación;
- estado activo.

### Sidebar colapsado

En modo colapsado:

- mantener iconos;
- ocultar textos;
- mostrar tooltip;
- mantener opción activa;
- reducir ancho;
- conservar navegación.

### Mobile

Convertir el sidebar en drawer/offcanvas con:

- botón hamburguesa;
- overlay;
- botón cerrar;
- cierre al seleccionar una opción cuando corresponda.

---

# 38. Header y Footer estáticos

El Header debe permanecer visible aunque el componente actual sea muy largo.

Contenido recomendado:

```text
☰ | Título / Breadcrumb       Empresa | SRI: PRUEBAS | 🔔 | 👤
```

El Footer también debe permanecer visible y ser compacto:

```text
© 2026 Aplicación                         v1.0.0
```

No utilizar `position: fixed` indiscriminadamente si el layout Flex puede resolverlo.

---

# 39. Main Content

`app-main` será el contenedor principal desplazable.

Cada pantalla:

```text
PAGE
├── PAGE HEADER
├── TOOLBAR / FILTERS
└── PAGE CONTENT
```

Si una tabla, formulario, detalle o dashboard es muy largo, el scroll ocurre únicamente dentro de `app-main`.

No permitir que el contenido largo desplace:

- Header;
- Sidebar;
- Footer.

---

# 40. CRUD prioritariamente mediante modales

Para operaciones administrativas pequeñas y medianas utilizar modal:

```text
+ Nuevo        → modal
👁 Ver          → modal de información
✏ Editar       → modal
🗑 Eliminar     → modal de confirmación
⚙ Cambiar      → modal cuando sea una operación corta
```

No convertir en modal:

- Dashboard;
- bandejas principales;
- detalle completo de comprobante SRI;
- reportes grandes;
- pantallas con múltiples tabs complejos;
- configuraciones demasiado extensas.

---

# 41. Estructura estándar de modal

```text
┌──────────────────────────────────────────┐
│ ICONO  TÍTULO                       X    │
├──────────────────────────────────────────┤
│                                          │
│ MODAL BODY                               │
│                                          │
│ scroll si es necesario                   │
│                                          │
├──────────────────────────────────────────┤
│                    Cancelar   Guardar    │
└──────────────────────────────────────────┘
```

Ejemplo Bootstrap:

```html
<div class="modal-header">
  <h5 class="modal-title d-flex align-items-center gap-2">
    <i class="bi bi-building"></i>
    Información de empresa
  </h5>

  <button
    type="button"
    class="btn-close"
    aria-label="Cerrar">
  </button>
</div>

<div class="modal-body">
  <div class="row g-3">

    <div class="col-12 col-md-6">
      <label class="form-label">RUC</label>
      <input
        type="text"
        class="form-control form-control-sm">
    </div>

    <div class="col-12 col-md-6">
      <label class="form-label">Razón social</label>
      <input
        type="text"
        class="form-control form-control-sm">
    </div>

  </div>
</div>

<div class="modal-footer">
  <button class="btn btn-outline-secondary btn-sm">
    <i class="bi bi-x-lg me-1"></i>
    Cancelar
  </button>

  <button class="btn btn-primary btn-sm">
    <i class="bi bi-floppy me-1"></i>
    Guardar
  </button>
</div>
```

---

# 42. Modales largos con Header/Footer visibles

Cuando un modal contenga un formulario largo:

```scss
.modal-content {
  max-height: calc(100vh - 3rem);
  overflow: hidden;
}

.modal-body {
  min-height: 0;
  overflow-y: auto;
}
```

Resultado:

```text
Modal Header → visible
Modal Body   → scroll
Modal Footer → visible
```

El botón Guardar nunca debe quedar perdido al final de un formulario largo.

Tamaños recomendados:

```text
Confirmación       → modal-sm
Formulario corto   → modal normal
CRUD medio         → modal-lg
Información amplia → modal-xl
```

No utilizar `modal-xl` por defecto.

---

# 43. Inputs Bootstrap obligatorios

Usar como patrón:

```html
<label class="form-label">Nombre</label>
<input class="form-control form-control-sm">
```

Select:

```html
<select class="form-select form-select-sm">
</select>
```

Textarea:

```html
<textarea
  class="form-control form-control-sm"
  rows="3">
</textarea>
```

Búsqueda:

```html
<div class="input-group input-group-sm">
  <span class="input-group-text">
    <i class="bi bi-search"></i>
  </span>
  <input
    type="text"
    class="form-control"
    placeholder="Buscar...">
</div>
```

Mantener Reactive Forms, validadores, `formControlName`, eventos y lógica existentes.

---

# 44. Validación visual Bootstrap

No cambiar reglas funcionales.

Usar:

```html
<input
  class="form-control form-control-sm"
  [class.is-invalid]="control.invalid && control.touched">

<div class="invalid-feedback">
  El campo es obligatorio.
</div>
```

Aplicar también:

- `form-check`;
- `form-switch`;
- `form-select`;
- `input-group`.

---

# 45. Formularios mediante Bootstrap Grid

Patrón:

```html
<div class="row g-3">
  <div class="col-12 col-md-6">
    ...
  </div>

  <div class="col-12 col-md-6">
    ...
  </div>

  <div class="col-12">
    ...
  </div>
</div>
```

Desktop:

```text
Campo 1             Campo 2
Campo 3             Campo 4
Campo largo
```

Mobile:

```text
Campo 1
Campo 2
Campo 3
Campo 4
Campo largo
```

Agrupar formularios largos por secciones funcionales.

---

# 46. Información de solo lectura en modal

Para `Ver información`, no presentar todo como inputs `disabled`.

Preferir:

```text
RUC
0460028810001

Razón social
EMPRESA PÚBLICA ...
```

Crear opcionalmente un componente visual:

```text
InfoFieldComponent
```

Entradas:

```text
label
value
icon
copyable
```

Los inputs deben reservarse principalmente para edición.

---

# 47. Bootstrap Icons como sistema principal

Utilizar preferentemente **Bootstrap Icons**.

No mezclar indiscriminadamente Material Icons, Font Awesome, emoji y Bootstrap Icons.

Mapa recomendado:

```text
Dashboard              bi-speedometer2
Documentos             bi-files
Factura                bi-receipt
Liquidación            bi-file-earmark-text
Nota crédito           bi-file-earmark-minus
Nota débito            bi-file-earmark-plus
Retención              bi-percent
Guía remisión          bi-truck

Empresas               bi-building
Establecimientos       bi-shop
Puntos emisión         bi-printer
Secuenciales           bi-list-ol
Certificados           bi-patch-check
Usuarios               bi-people
Roles                  bi-shield-lock
Permisos               bi-key

Configuración          bi-gear
Auditoría              bi-clock-history
Errores                bi-exclamation-triangle
Correo                 bi-envelope
Monitoreo              bi-activity

Nuevo                   bi-plus-lg
Ver                     bi-eye
Editar                  bi-pencil
Eliminar                bi-trash
Guardar                 bi-floppy
Cancelar                bi-x-lg
Buscar                  bi-search
Filtrar                 bi-funnel
Actualizar              bi-arrow-clockwise
Descargar               bi-download
Reprocesar              bi-arrow-repeat
XML                     bi-filetype-xml
PDF / RIDE              bi-file-earmark-pdf
Menú                    bi-list
```

Todo botón únicamente con icono debe tener `title`, tooltip o `aria-label`.

---

# 48. Botones Bootstrap compactos

Nuevo:

```html
<button class="btn btn-primary btn-sm">
  <i class="bi bi-plus-lg me-1"></i>
  Nuevo
</button>
```

Cancelar:

```html
<button class="btn btn-outline-secondary btn-sm">
  <i class="bi bi-x-lg me-1"></i>
  Cancelar
</button>
```

Acciones en tabla:

```html
<button
  class="btn btn-sm btn-light"
  title="Ver"
  aria-label="Ver">
  <i class="bi bi-eye"></i>
</button>
```

Evitar botones grandes dentro de tablas.

---

# 49. Tablas Bootstrap

Patrón:

```html
<div class="table-responsive">
  <table class="table table-sm table-hover align-middle">
    ...
  </table>
</div>
```

Requisitos:

- compactas;
- header diferenciado;
- hover;
- badges;
- acciones icon-only;
- loading;
- empty state;
- paginación uniforme.

---

# 50. Patrón visual CRUD

```text
PAGE HEADER
│
├── [+ NUEVO]
│
FILTER TOOLBAR
│
DATA TABLE
│
├── 👁 VER
├── ✏ EDITAR
└── 🗑 ELIMINAR
```

Cuando el tamaño del formulario lo permita:

```text
NUEVO    → Modal
VER      → Modal info
EDITAR   → Modal
ELIMINAR → ConfirmModal
```

---

# 51. ConfirmModal reutilizable

Crear un único componente:

```text
ConfirmModalComponent
```

Tipos:

```text
info
warning
danger
```

Ejemplo:

```text
⚠ Eliminar registro

Esta acción no puede deshacerse.

[Cancelar] [Eliminar]
```

No duplicar un modal de confirmación por cada módulo.

---

# 52. Loading dentro de modal

Durante una operación:

```text
[Guardar]
    ↓
[spinner Guardando...]
```

Deshabilitar temporalmente el botón para evitar doble submit cuando la lógica existente permita identificar el estado de carga.

---

# 53. Design Tokens

Centralizar:

```scss
:root {
  --app-header-height: 56px;
  --app-footer-height: 34px;

  --app-sidebar-width: 260px;
  --app-sidebar-collapsed-width: 72px;

  --app-radius-sm: 4px;
  --app-radius-md: 8px;
  --app-radius-lg: 12px;

  --app-space-1: 4px;
  --app-space-2: 8px;
  --app-space-3: 12px;
  --app-space-4: 16px;
  --app-space-5: 24px;

  --app-font-xs: 11px;
  --app-font-sm: 12px;
  --app-font-md: 14px;
  --app-font-lg: 18px;
  --app-font-xl: 22px;
}
```

---

# 54. Densidad visual

Preferir:

```text
Inputs       34–38px
Botones      32–38px
Body         13–14px
Labels       12–13px
Tablas       12–13px
```

Compacto no significa difícil de utilizar.

---

# 55. Responsive

## Desktop

```text
Header fijo
Sidebar fijo
Main scroll
Footer fijo
```

## Tablet

```text
Sidebar colapsable
Main scroll
Modales adaptados
```

## Mobile

```text
Header fijo
Sidebar offcanvas
Main scroll
Footer compacto
Formularios una columna
Modales adaptados
```

Evitar overflow horizontal general.

---

# 56. Scroll permitido

Únicamente cuando sea necesario:

```text
✓ Main Content
✓ Sidebar Menu
✓ Modal Body
✓ table-responsive horizontal
```

Evitar múltiples scroll verticales anidados en cards/paneles dentro de `main`.

---

# 57. Componentes visuales recomendados

```text
layout/
├── main-layout/
├── app-header/
├── app-sidebar/
└── app-footer/

shared/components/
├── page-header/
├── info-field/
├── confirm-modal/
├── form-modal/
├── status-badge/
├── loading/
├── empty-state/
└── error-state/

shared/styles/
├── _variables.scss
├── _layout.scss
├── _forms.scss
├── _tables.scss
├── _buttons.scss
└── _modals.scss
```

No crear componentes si no aportan reutilización real.

---

# 58. No duplicar formularios de modal

Si Crear y Editar comparten los mismos campos:

```text
NO:
EmpresaCreateModal
EmpresaEditModal

PREFERIR:
EmpresaFormModal
```

con modo:

```text
CREATE
EDIT
```

sin alterar los servicios actuales.

---

# 59. Orden de implementación V2

```text
01 MainLayout
02 Header estático
03 Footer estático
04 Sidebar estático
05 Sidebar menu scroll
06 Sidebar collapse
07 Sidebar mobile
08 Main Content scroll
09 Bootstrap Icons
10 Design Tokens
11 Inputs Bootstrap
12 Botones
13 Formularios
14 Modales
15 Info Modals
16 Confirm Modals
17 Tablas
18 Badges / estados
19 Responsive
20 Accesibilidad
21 Refinamiento
```

Primero corregir el layout global. Después adaptar los módulos.

---

# 60. Primera fase obligatoria

Modificar inicialmente solo:

```text
MainLayout
Header
Sidebar
Footer
styles globales
```

Objetivo:

```text
HEADER FIJO
SIDEBAR FIJO
FOOTER FIJO
MAIN SCROLL
SIDEBAR MENU SCROLL
```

No comenzar migrando todos los CRUD a modal antes de estabilizar el layout.

---

# 61. Segunda fase

Unificar:

```text
Bootstrap Icons
Inputs
Labels
Buttons
Tables
Badges
Spacing
Typography
```

---

# 62. Tercera fase

Migrar progresivamente:

```text
Nuevo
Editar
Ver
Confirmar
```

a modales donde corresponda.

---

# 63. Cuarta fase

Adaptar módulo por módulo:

```text
Dashboard
Documentos
Empresas
Establecimientos
Puntos emisión
Certificados
Usuarios
Roles
Configuración
Auditoría
```

---

# 64. Checklist del Layout

- [ ] `body` no tiene scroll.
- [ ] aplicación ocupa `100vh`.
- [ ] Header siempre visible.
- [ ] Footer siempre visible.
- [ ] Sidebar siempre visible en desktop.
- [ ] Main tiene scroll vertical independiente.
- [ ] Sidebar menu tiene scroll independiente.
- [ ] Sidebar header permanece visible.
- [ ] Sidebar footer permanece visible.
- [ ] No existe overflow horizontal global.
- [ ] Funciona con contenido corto.
- [ ] Funciona con contenido muy largo.

---

# 65. Checklist Sidebar

- [ ] Expandido.
- [ ] Colapsado.
- [ ] Iconos.
- [ ] Tooltips.
- [ ] Ruta activa.
- [ ] Submenús.
- [ ] Scroll en menú largo.
- [ ] Header interno estático.
- [ ] Footer interno estático.
- [ ] Offcanvas mobile.
- [ ] Overlay mobile.
- [ ] Rutas originales intactas.

---

# 66. Checklist Modales

- [ ] Header uniforme.
- [ ] Icono.
- [ ] Título.
- [ ] Cerrar.
- [ ] Body organizado.
- [ ] Bootstrap Grid.
- [ ] Inputs Bootstrap.
- [ ] Footer.
- [ ] Cancelar.
- [ ] Guardar.
- [ ] Loading.
- [ ] Validaciones.
- [ ] Responsive.
- [ ] Scroll interno en formularios largos.
- [ ] Header/footer visibles en modal largo.

---

# 67. Checklist Formularios

- [ ] `form-label`.
- [ ] `form-control`.
- [ ] `form-control-sm`.
- [ ] `form-select form-select-sm`.
- [ ] `row g-3`.
- [ ] Responsive.
- [ ] Errores próximos al campo.
- [ ] No overflow.
- [ ] Densidad uniforme.
- [ ] Reactive Forms y validadores intactos.

---

# 68. Checklist Iconografía

- [ ] Bootstrap Icons como sistema principal.
- [ ] Tamaño consistente.
- [ ] Alineación consistente.
- [ ] Espaciado uniforme.
- [ ] Tooltip para icon-only.
- [ ] `aria-label`.
- [ ] No emoji para acciones del sistema.

---

# 69. Instrucción específica para MainSidebar

```text
Analiza MainSidebar antes de modificarlo.

Reestructura únicamente su presentación.

Debe quedar dividido en:
- sidebar-header;
- sidebar-menu;
- sidebar-footer.

El Sidebar debe ocupar el 100% del alto disponible dentro de app-body.

sidebar-header y sidebar-footer deben permanecer visibles.

sidebar-menu debe utilizar:
overflow-y: auto;
overflow-x: hidden;
min-height: 0;
flex: 1;

Mantén:
- rutas;
- permisos;
- submenús;
- eventos;
- estado activo;
- lógica existente.

Agrega Bootstrap Icons de forma consistente.

En modo colapsado:
- mostrar iconos;
- ocultar texto;
- mostrar tooltip.

En mobile:
- convertir a drawer/offcanvas;
- agregar overlay;
- conservar navegación.
```

---

# 70. Instrucción específica para CRUD

```text
Analiza primero el CRUD actual.

No cambies:
- servicio;
- endpoint;
- payload;
- validaciones;
- eventos;
- métodos;
- permisos.

Mantén la tabla como pantalla principal.

Mover a modal cuando sea apropiado:
- crear;
- editar;
- ver;
- confirmar eliminación.

Usar:
- Bootstrap modal;
- Bootstrap Grid;
- form-label;
- form-control form-control-sm;
- form-select form-select-sm;
- btn btn-sm;
- Bootstrap Icons.

Si el modal es largo:
- limitar max-height;
- hacer modal-body scrollable;
- mantener modal-header y modal-footer visibles.

La información de solo lectura debe priorizar info-fields sobre inputs disabled.
```

---

# 71. Instrucción maestra para agente de programación

```text
Analiza primero el componente actual y sus dependencias.

Tu trabajo es exclusivamente UX/UI.

NO cambies:
- lógica de negocio;
- endpoints;
- servicios HTTP;
- modelos;
- interfaces;
- rutas;
- permisos;
- autenticación;
- comportamiento CRUD.

Implementa la GUIA_MAESTRA_UX_UI_ANGULAR_V2.md.

Prioriza:
1. layout 100vh;
2. Header estático;
3. Footer estático;
4. Sidebar estático;
5. scroll independiente de Sidebar Menu;
6. scroll independiente de Main Content;
7. Bootstrap 5;
8. Bootstrap Icons;
9. form-control/form-control-sm;
10. Bootstrap Grid;
11. CRUD apropiado en modales;
12. modal-body con scroll para contenido largo;
13. responsive;
14. accesibilidad.

Antes de crear componentes nuevos revisa si existe uno reutilizable.

Al finalizar:
1. informa archivos modificados;
2. informa componentes creados;
3. confirma que la lógica funcional no cambió;
4. ejecuta build/tests disponibles;
5. informa problemas encontrados.
```

---

# 72. Criterio final V2

Antes de considerar finalizada la mejora:

- [ ] funcionalidad existente intacta;
- [ ] Header estático;
- [ ] Footer estático;
- [ ] Sidebar estático;
- [ ] menú Sidebar desplazable;
- [ ] Main Content desplazable;
- [ ] formularios largos no rompen el layout;
- [ ] CRUD simples/medios utilizan modales;
- [ ] modal largo conserva Header/Footer;
- [ ] inputs utilizan Bootstrap;
- [ ] iconografía consistente;
- [ ] tablas compactas;
- [ ] responsive;
- [ ] accesibilidad básica;
- [ ] sin overflow global innecesario.

---

# 73. Principio final V2

> **Primero preservar la funcionalidad. Después estabilizar el Layout. Luego unificar Bootstrap e iconografía. Después trasladar los CRUD apropiados a modales. Finalmente perfeccionar responsive, accesibilidad y estética.**

El comportamiento final esperado es:

```text
VENTANA 100vh
│
├── HEADER ESTÁTICO
│
├── APP BODY
│   ├── SIDEBAR ESTÁTICO
│   │   ├── HEADER
│   │   ├── MENU → SCROLL
│   │   └── FOOTER
│   │
│   └── MAIN CONTENT → SCROLL
│
└── FOOTER ESTÁTICO
```
