# Guion de Demo — 5 minutos

Guion para presentar los flujos principales de **Mejengueros** (reserva de canchas de fútbol en Costa Rica) en exactamente 5 minutos. Cubre las dos personas: **jugador (mejenguero)** y **dueño de complejo**.

---

## Antes de empezar (checklist de 2 minutos, fuera del reloj)

- [ ] App instalada y abierta en el dispositivo/emulador; **sesión ya iniciada** como usuario con rol dueño (así podés mostrar ambas vistas).
- [ ] Backend y base de datos con **datos sembrados** (varias canchas publicadas con coordenadas, reseñas y una reserva pasada para el prompt de reseña).
- [ ] **Buena conexión**: el mapa de ubicación carga tiles de OpenFreeMap por red. Probalo una vez antes.
- [ ] Arrancá parado en la pestaña **Buscar** con el catálogo ya cargado.
- [ ] Tené en mente **una cancha con reseñas** (para el detalle) y **una notificación de reseña pendiente**.
- [ ] Silenciá notificaciones del sistema y subí el brillo.

> Regla de oro: no tipear si podés evitarlo. Filtros y taps > escribir. Si algo tarda, seguí hablando.

---

## Reloj de la demo

| Tiempo | Bloque | Persona |
|--------|--------|---------|
| 0:00 – 0:30 | Intro y contexto | — |
| 0:30 – 2:00 | Buscar → Detalle de cancha → Reservar | Jugador |
| 2:00 – 2:45 | Mis reservas + Notificación + Reseña | Jugador |
| 2:45 – 4:30 | Vista dueño: Mi complejo → Crear complejo → Agregar cancha | Dueño |
| 4:30 – 5:00 | Reservas/Reseñas del dueño + Cierre | Dueño |

---

## 0:00 – 0:30 · Intro

**Decís:**
> "Mejengueros conecta a jugadores que buscan dónde jugar con dueños de complejos que quieren llenar sus canchas. Es una sola app con dos experiencias: la del **mejenguero** que reserva, y la del **dueño** que administra. Está construida en **Kotlin Multiplatform**, así que el mismo código corre en Android, iOS y escritorio. Veamos el flujo completo."

**Hacés:** Mostrás la pantalla **Buscar** con el catálogo cargado.

---

## 0:30 – 2:00 · Jugador: Buscar → Detalle → Reservar

**Decís:**
> "Como jugador entro a **Buscar**. Tengo el catálogo de canchas con scroll infinito y puedo filtrar por **provincia** y **cantón**."

**Hacés:**
1. Scrolleás el catálogo (mostrá que sigue cargando — scroll infinito).
2. Abrís el filtro **Provincia** y elegís una; luego **Cantón**. La lista se acota.
3. Tocás una cancha **con reseñas** para abrir el **Detalle de cancha**.

**Decís (en el detalle — este es el momento estrella):**
> "Acá está todo lo que el jugador necesita para decidir: foto, calificación, **disponibilidad de horarios**, servicios, las **reseñas** de otros jugadores… y la **ubicación real en el mapa**, centrada en la cancha. Antes esto era un placeholder; ahora es un mapa de verdad."

**Hacés:** Bajás hasta la sección **Ubicación** y mostrás el mapa con el pin sobre la cancha. Seguís bajando a **Reseñas**.

**Decís:**
> "Me convence. Reservo."

**Hacés:**
4. Tocás **Reservar cancha**.
5. Elegís **día** y **horario** disponible.
6. Confirmás la reserva.

---

## 2:00 – 2:45 · Jugador: Mis reservas + Notificación + Reseña

**Decís:**
> "La reserva ya aparece en **Mis reservas**, separada entre próximas y finalizadas."

**Hacés:** Vas a la pestaña **Mis reservas** y mostrás la reserva recién hecha en *próximas*.

**Decís:**
> "Y cuando termino de jugar, la app me invita a dejar una reseña. Mirá **Notificaciones**."

**Hacés:**
1. Vas a **Notificaciones** (mostrá el contador de no leídas).
2. Abrís la notificación de **reseña pendiente** (sobre una reserva ya finalizada).
3. Dejás una **reseña**: calificación en estrellas + comentario, y confirmás.

**Decís:**
> "Esa reseña es la que otros jugadores van a ver en el detalle. El ciclo se cierra solo."

---

## 2:45 – 4:30 · Dueño: Mi complejo → Crear complejo → Agregar cancha

**Decís:**
> "Ahora cambio a la **vista de dueño** — es el mismo usuario, otra cara de la app."

**Hacés:** Tocás el botón de **cambiar a vista dueño**. La navegación cambia a **Mi complejo · Reservas · Reseñas**.

**Decís:**
> "Desde **Mi complejo** administro todo. Voy a dar de alta un complejo nuevo."

**Hacés:**
1. Entrás a **Mi complejo** y empezás **Crear complejo**.
2. Completás nombre y dirección.
3. **Elegís la ubicación en el mapa** (picker interactivo): movés el mapa / tocás para poner el punto.

**Decís (segundo momento clave — conectá con lo del jugador):**
> "Fijate: esta ubicación que el dueño elige acá en el **mapa interactivo** es exactamente la que el jugador ve después en el detalle. Misma coordenada, punta a punta."

**Hacés:**
4. Seleccionás **provincia**, **cantón** y **servicios**.
5. Guardás el complejo.
6. **Agregás una cancha**: nombre, foto y disponibilidad de horarios.

---

## 4:30 – 5:00 · Dueño: Reservas/Reseñas + Cierre

**Decís:**
> "Como dueño veo las **reservas** que me entran y las **reseñas** que dejan los jugadores sobre mis canchas."

**Hacés:** Mostrás rápidamente **Reservas** y **Reseñas** del dueño.

**Decís (cierre):**
> "Eso es Mejengueros de punta a punta: el jugador encuentra, decide con el mapa y reserva; el dueño publica, administra y recibe feedback. Todo en tiempo real y en un solo código base para Android, iOS y escritorio. Gracias."

---

## Plan B (si algo falla)

- **El mapa no carga:** decí "esto normalmente carga el mapa de la ubicación" y seguí; no te trabes esperando tiles.
- **La red se cae:** tené screenshots de respaldo del detalle con mapa y del picker de ubicación.
- **Te pasás de tiempo:** los bloques sacrificables son *Mis reservas* (2:00) y *Reservas/Reseñas del dueño* (4:30). Los flujos que **no** podés saltear: Detalle con mapa y Crear complejo con picker.
- **Orden alternativo:** si el público es de dueños, arrancá por la vista dueño; si es de jugadores, quedate más en Buscar/Detalle.
