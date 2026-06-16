# Issue #31: Implementar el landing page del equipo The Monsters

## Título

Implementar el landing page del equipo The Monsters.


## Nota de priorización MVP semana 10

Landing del equipo queda post-MVP por directriz de semana 10; conservar trazabilidad sin bloquear el demo funcional.

## Prioridad

Media

## Objetivo

Definir e implementar una página pública que presente al equipo The Monsters, sus integrantes, roles y relación con el proyecto Mejengueros, de forma clara y ordenada.

## Trazabilidad

- `FR21` La aplicación debe contar con un landing page del equipo The Monsters.

## Relación con issues coordinados

- `#31` forma parte de la épica `#7 Landing page del producto y el equipo`.
- `#30` presenta el producto Mejengueros.
- `#31` presenta al equipo responsable del proyecto.
- `#31` no sustituye documentación formal del curso ni reportes académicos; sólo ofrece una presentación pública del equipo.

## Historia de usuario

Como visitante del sitio,
quiero ver una landing page del equipo The Monsters,
para conocer quiénes desarrollan el proyecto y cuáles son sus roles dentro del trabajo.

## Alcance

- Presentar el nombre del equipo The Monsters.
- Mostrar los integrantes del equipo.
- Mostrar el rol o responsabilidad principal de cada integrante.
- Explicar brevemente la relación del equipo con el proyecto Mejengueros.
- Mantener una experiencia pública, clara y responsiva.
- Permitir navegación básica con el resto de las páginas públicas del MVP.

## Secciones mínimas esperadas

- Hero principal con nombre del equipo.
- Listado de integrantes.
- Rol principal de cada integrante según la organización del proyecto.
- Relación del equipo con el producto Mejengueros.
- Navegación pública coherente con el resto del sitio.

## Fuera de alcance

- Gestionar perfiles editables de integrantes desde panel administrativo.
- Sincronizar datos automáticamente desde GitHub u otras plataformas.
- Crear portafolios individuales complejos por integrante.
- Agregar chat, contacto avanzado o formularios complejos.
- Convertir esta página en repositorio documental académico completo.

## Reglas de negocio

1. La landing page del equipo debe ser pública y accesible sin autenticación.
2. Debe mostrar información suficiente para identificar a los integrantes y sus roles.
3. La información presentada debe estar alineada con la organización real del equipo.
4. La experiencia debe ser clara tanto en escritorio como en móvil.
5. La página debe mantener coherencia visual con la landing page del producto.

## Flujo principal

1. Un visitante accede a la sección pública del equipo.
2. El sistema muestra la landing page de The Monsters.
3. El visitante visualiza el nombre del equipo, sus integrantes y sus roles.
4. El visitante entiende quiénes participan en el desarrollo del proyecto.
5. El visitante puede navegar hacia otras secciones públicas definidas por el MVP.

## Casos alternos/validaciones

- Si se accede desde un dispositivo móvil, la distribución de integrantes debe seguir siendo legible.
- Si algún integrante sólo tiene un rol principal resumido, la página debe seguir siendo clara y consistente.
- Si el contenido del equipo cambia, la estructura debe permitir actualizar la información sin rediseñar toda la página.

## Datos de entrada

- Nombre del equipo.
- Lista de integrantes.
- Rol o responsabilidad principal de cada integrante.
- Descripción breve de la participación del equipo en el proyecto.
- Recursos visuales o lineamientos de estilo definidos por el equipo.

## Fuente de verdad del equipo

- Documento `documentación/Pro_4_Themounster.md` como referencia inicial de integrantes y roles.

## Datos de salida

- Landing page pública del equipo The Monsters.
- Información visible de integrantes y roles.
- Presentación clara de la identidad del equipo.
- Navegación pública coherente con el resto del sitio.

## Dependencias

- `#7` como épica contenedora.
- Confirmación de integrantes y roles del equipo.
- Disponibilidad del cliente o base web donde se publicará la página.

## Criterios de aceptación

1. Dado un visitante no autenticado, cuando accede a la página del equipo, entonces puede visualizar la landing page de The Monsters.
2. Dado el contenido del equipo, cuando el visitante revisa la página, entonces puede identificar a los integrantes y sus roles principales.
3. Dado un visitante en móvil, cuando visualiza la landing page, entonces la información sigue siendo legible y ordenada.
4. Dada la relación con el proyecto, cuando se revisa la página, entonces queda claro que The Monsters es el equipo responsable de Mejengueros.
5. Dado el alcance de la historia, cuando se valida la implementación, entonces la página funciona como presentación pública del equipo y no como sistema de gestión de perfiles.

## Definition of Done

- Existe una landing page pública del equipo The Monsters.
- La página muestra integrantes y roles de forma clara.
- La presentación del equipo está alineada con el contexto del proyecto Mejengueros.
- La UI es responsiva y consistente con el resto de las páginas públicas.
- La historia se mantiene en alcance MVP, sin agregar capacidades administrativas innecesarias.
- La implementación queda lista para revisión y demostración.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/30
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#30
Current issue: TheMonstersP4/mejengueros-app#31