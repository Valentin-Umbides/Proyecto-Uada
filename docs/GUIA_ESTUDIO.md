# Guía de estudio – Defensa TPO "La misión de Uada"

Defensa: **11/11**. La nota es individual y el profe puede pedir que **modifiquemos el código en vivo** para que corra de otra manera. Esta guía tiene tres partes:

1. Qué hay que saber explicar sin mirar (el esqueleto del backtracking).
2. **Dónde tocar** el código según el tipo de cambio que pidan.
3. Un **banco de 18 modificaciones ya probadas**, con el código exacto y el resultado que tiene que dar.

> ⚠ Programación dinámica, greedy, Dijkstra, DyC y complejidad están en el material de la materia. El detalle de backtracking y branch & bound **no** está desarrollado en las diapositivas (solo se menciona en la de DFS); lo que dice esta guía sobre eso sale del enunciado y del código.

---

## 0. Plan de estudio (7/11 → 11/11)

| Día | Qué hacer | Tiempo |
|---|---|---|
| Hoy | Leer `PUNTEO.md` + la presentación con el guion (tecla G). Leer la sección 1 de esta guía con `MisionUada.java` abierto al lado. | 1 h 30 |
| Mañana | Dibujar en papel el árbol de `chico` (sección 1.3) sin mirar. Hacer las modificaciones 1 a 6 del banco **escribiéndolas vos**, sin copiar. | 2 h |
| Pasado | Modificaciones 7 a 18. Para cada una, antes de correr, **predecir** qué va a cambiar. | 2 h |
| 10/11 | Autoevaluación (sección 5) en voz alta. Ensayo de la presentación. Repasar las "trampas" (mods 9, 11, 14). | 1 h 30 |

Regla para practicar: **trabajá en una rama** (`git checkout -b practica`) y al terminar `git checkout -- . && git checkout main`. Después de correr `Main`, siempre `git checkout -- logs`.

Compilar y correr (Mac, desde la carpeta del proyecto):

```bash
javac -encoding UTF-8 -d out src/main/java/uada/*.java
java -cp out uada.Main chico                      # mapa chico (6 lugares), ideal para ver cambios
java -cp out uada.Main mapas/mapa_grande.txt      # mapa de los profes
java -cp out uada.Main mapas/mapa_grande.txt sincota   # sin branch & bound
```

**Tip clave:** en `mapa_grande` la batería casi nunca es el límite (el resultado 411 s no cambia aunque toques la batería). Si el profe pide un cambio de batería o de recarga, **mostralo con `chico`**, donde sí se nota.

---

## 1. Lo que tenés que poder explicar sin mirar

### 1.1 Los archivos, en una línea cada uno

| Archivo | Qué hace |
|---|---|
| `Main.java` | Lee los argumentos (`sincota`, `sinespera`, `dirigido`, `nodirigido`), carga el mapa, crea `MisionUada`, imprime y guarda el log. |
| `CargadorMapa.java` | Lee el `.txt` (LUGAR, ARISTA, VENTANA, INICIO…) y arma el `Mapa`. Errores con número de línea. |
| `Mapa.java` / `Conexion.java` | Grafo con listas de adyacencia; cada conexión tiene segundos y modos permitidos. |
| `Estacion.java` | Un lugar: aula/obligatorio/prohibido, `recarga()` (suma de dígitos / 5), `cerradaEn(t)`, `primerInstanteAbierto`. |
| `Ventana.java` | Un intervalo de cierre `[desde, hasta]` (cerrado inclusive, línea 19). |
| `Modo.java` | Los 3 modos con factor de tiempo y de batería. **El orden del enum es el orden en que se prueban.** |
| `MisionUada.java` | El backtracking + branch & bound, top-3 y objetivo secundario. **Es el 90 % de la defensa.** |
| `Ruta.java` / `Movimiento.java` | Una solución (lista de pasos) y un paso. |

### 1.2 El esqueleto de `backtrack` (MisionUada.java, línea 133)

Estado: `(actual, tiempo, bateria, nivel)` + `visitado[]` + `camino` + `obligatoriasVisitadas`.

```
backtrack(actual, tiempo, bateria):
    actualizarMejorParcial(actual)                       // objetivo secundario (l. 287)
    para cada conexión actual -> sig:                    // l. 137
        podas sin modo: prohibida / vuelta anticipada / ya visitada   (l. 141-148)
        para cada modo de la conexión:                   // l. 151
            t2 = tiempo + modo.tiempo ; b2 = bateria - modo.consumo
            poda batería                                 // l. 157
            si sig está cerrada: esperar o podar         // l. 162-178
            recarga (tope 100, no en el cierre)          // l. 179
            podas por cota (branch & bound)              // l. 182-214
            HACER: camino.add, visitado=true, ...        // l. 216
            si sig == origen  -> registrarSolucion       // top-3
            si no             -> backtrack(sig, t2, b3)  // l. 239
            DESHACER: visitado=false, camino.remove      // l. 241-246
```

Las 3 cosas que siempre preguntan:
- **¿Dónde está el "deshacer"?** Líneas 241-246: se revierte exactamente lo que se hizo en "hacer" (visitado, obligatoriasVisitadas, recargasPendientes, camino).
- **¿Por qué la cota no pierde soluciones?** Porque suma, para cada obligatorio pendiente y para el regreso a 348, la conexión de entrada **más barata** del mapa (`calcularEntradasMinimas`, l. 305). Eso nunca sobreestima lo que falta → si aun así supera a la 3ª mejor, ninguna extensión puede entrar al top-3.
- **¿Por qué `>` y no `>=` en la cota?** Para no perder rutas que empatan en tiempo y ganan por batería.

### 1.3 El árbol para dibujar (mapa `chico`)

Con cota: 297 nodos analizados, mejor ruta `A348 > PATIO > STARBUCKS > A613 > A348` en **3000 s**. Sin cota: 465 nodos, mismo top-3. Esos dos números demuestran que la poda funciona y no cambia el resultado.

Mapa grande: **411 s** (con espera), 128.587 nodos con cota vs 12.601.408 sin cota. Con `sinespera`: 417,5 s. Mapa sin solución: fase 2, ruta truncada que termina en Aula 613.

### 1.4 Complejidad

`T(n) = 3g · T(n−1) + O(n²)` → recurrencia por sustracción con `a = 3g > 1` → **O((3g)ⁿ)** (diapositiva "1b - Eficiencia Algoritmos Recursivos"). Espacio O(n + m). La cota no cambia el peor caso; mejora el caso práctico (×2,4 nodos por lugar con cota vs ×4,8 sin cota, `complejidad_practica.csv`).

---

## 2. Dónde tocar según lo que pidan

| Si piden… | Archivo : línea | Ojo con… |
|---|---|---|
| Otra cantidad de rutas (top-5, solo la mejor) | `MisionUada` : 36 `TOP` | Nada: la cota usa `mejores.get(TOP - 1)`, se adapta sola. |
| Otra batería inicial / tope | `MisionUada` : 34-35 | En `mapa_grande` no cambia nada; mostrarlo con `chico`. |
| Cambiar velocidad/consumo de un modo | `Modo` : 11-13 | La cota se recalcula sola desde los modos. |
| Prohibir un modo en todo el mapa | `MisionUada` : después de l. 152 | — |
| Una **regla nueva** sobre un tramo ("no se puede saltar hacia un aula") | `MisionUada` : después de l. 152 | Usar `podar(...)` + `continue` para que figure en el log y en las estadísticas. |
| Cambiar la regla de batería (reserva mínima, etc.) | `MisionUada` : 157 | — |
| Cambiar la recarga | `Estacion` : 33 | La cota de batería usa `recargasPendientes`, que sale de `recarga()`: se adapta sola. |
| Cambiar cómo se espera / el margen de 1 s | `MisionUada` : 38 y 162-178, `Ventana` : 19 | **Trampa**, ver mod 9. |
| Ventanas abiertas/cerradas en los bordes | `Ventana` : 19 | — |
| No permitir esperar | `Main` : 23 (o argumento `sinespera`) | — |
| Cambiar el desempate | `MisionUada` : 277 `comparar` | Si el **primer** criterio deja de ser el tiempo, la cota de tiempo deja de ser válida (mod 14). |
| Top-3 con la misma secuencia de lugares | `MisionUada` : 259 | Es lo que antes se veía "repetido" (mod 11). |
| Límite de pasos / profundidad | `MisionUada` : después de l. 152, usando `nivel` | — |
| Otro objetivo secundario | `MisionUada` : 287 `actualizarMejorParcial` | — |
| Otro origen | El `.txt`: `INICIO;Aula 501` | No hace falta tocar código. |
| Grafo no dirigido | Argumento `nodirigido` | No hace falta tocar código. |
| Un modo nuevo | `Modo` (enum) + `CargadorMapa` : 117-119 (letra) | Actualizar el mensaje "usar C, S o P". |

**Checklist mental antes de tocar cualquier cosa** (decilo en voz alta en la defensa, suma mucho):
1. ¿Esto cambia una **restricción** (poda) o el **criterio de optimalidad** (`comparar`)?
2. Si es una restricción nueva: la cota sigue siendo válida (puede quedar más floja, nunca incorrecta).
3. Si cambia el criterio: ¿la cota sigue midiendo lo mismo que el criterio? Si no, desactivarla o reescribirla.
4. Si toco "hacer", toco también "deshacer".
5. Pruebo con `chico` y comparo con `sincota`: tienen que dar el mismo top-3.

---

## 3. Banco de modificaciones probadas

Todas se probaron sobre el código del repo (commit `88a11bf`). "Base" = chico 3000 / 3151 / 3450 s (297 nodos); grande 411 / 411 / 411 s (128.587 nodos).

### Mod 1 – Mostrar las 5 mejores rutas
```java
public static final int TOP = 5;                     // MisionUada l. 36
```
Resultado: chico 3000 / 3151 / 3450 / 3450 / 3900 s (396 nodos). Grande: cuatro de 411 s y una de 431 s (175.587 nodos).
**Para explicar:** más rutas en el top → la 5ª es peor que la 3ª → la cota poda menos → más nodos.

### Mod 2 – Solo la mejor ruta
```java
public static final int TOP = 1;
```
Resultado: chico 3000 s (218 nodos); grande 411 s (107.782 nodos). Menos nodos porque la cota compara contra la mejor, no contra la 3ª.

### Mod 3 – Regla nueva: "no se puede llegar saltando a un aula"
Después de `nodosAnalizados++;` (l. 152):
```java
if (modo == Modo.SALTANDO && sig.esAula()) {
    podar(nivel, actual, sig, modo, "regla nueva", "no se puede saltar hacia un aula");
    continue;
}
```
Resultado: chico 3151 / 3450 / 3600 s (240 nodos); grande **456** / 462,5 / 462,5 s (48.246 nodos). En las estadísticas aparece la poda "regla nueva".
**Para explicar:** es una restricción nueva, la cota sigue siendo válida (todavía cuenta el salto como entrada más barata, así que queda optimista; solo más floja).

### Mod 4 – Deshabilitar "patas para arriba"
Después de l. 152:
```java
if (modo == Modo.PATAS_ARRIBA) continue;
```
Resultado: mismo top-3 en ambos mapas, pero grande baja a 41.278 nodos (chico 260). Patas arriba nunca formaba parte de las mejores rutas; se ahorra explorarlo.

### Mod 5 – Batería inicial 60
```java
public static final double BATERIA_INICIAL = 60.0;   // l. 34
```
Resultado: chico 4800 / 4801 / 5100 s (cambia el ganador: hay que ir más lento para no quedarse sin batería). Grande: igual (411 s).

### Mod 6 – Saltar consume más (4 en lugar de 2,2)
```java
SALTANDO("Saltando", 0.5, 4.0),                      // Modo l. 11
```
Resultado: chico 3750 / 3750 / 3900 s; grande igual.

### Mod 7 – Recarga doble (suma de dígitos / 2,5)
```java
return suma / 2.5;                                   // Estacion l. 33
```
Resultado: mismo top-3 (chico 310 nodos: la poda "batería insuficiente" corta menos). Buen ejemplo de que más batería no siempre cambia el óptimo.

### Mod 8 – Llegar a 348 con al menos 10 de reserva
```java
if (esCierre ? b2 < 10 : b2 <= 0) {                  // l. 157
```
Resultado: chico 3750 / 3750 / 3750 s (la mejor base terminaba con 1,0). Grande igual.

### Mod 9 – TRAMPA: entrar en el mismo segundo que termina el cierre
Si solo cambiás `MARGEN_ESPERA = 0.0;` **el programa se cuelga**. Por qué: la ventana es cerrada `[desde, hasta]`, en `t = hasta` sigue cerrada, `primerInstanteAbierto` vuelve a calcular `t = hasta + 0` → bucle infinito.
Arreglo correcto (las dos cosas juntas):
```java
public static final double MARGEN_ESPERA = 0.0;      // MisionUada l. 38
return t >= desde && t < hasta;                       // Ventana l. 19: abierta en 'hasta'
```
Resultado: chico 3000 / **3150** / 3450 s; grande **410** s (1 s menos que la base, justo el margen).
**Para explicar:** el margen de 1 s existe porque el intervalo es inclusivo (lo dice el comentario del archivo de los profes).

### Mod 10 – Ventanas exclusivas en los dos bordes
```java
return t > desde && t < hasta;                        // Ventana l. 19
```
Resultado: grande **370 s** (95.515 nodos); chico igual. Coincide con la verificación por fuerza bruta.

### Mod 11 – Sin espera por defecto
```java
boolean permitirEspera = false;                       // Main l. 23
```
Resultado: chico 3000 / 3450 / 3450 s; grande **417,5 s** (= correr con `sinespera`). Las esperas pasan a podas por "ventana temporal".

### Mod 12 – Que se cuente un mismo recorrido con distintos modos como rutas distintas
```java
if (mejores.get(i).secuenciaConModos().equals(r.secuenciaConModos())) {   // l. 259
```
Resultado: grande muestra **tres veces la misma secuencia de lugares** con 411 s (cambian modos). Es exactamente el problema que se corrigió: por eso se compara por secuencia de lugares.

### Mod 13 – Límite de 4 tramos
Después de l. 152:
```java
if (!esCierre && nivel >= 4) { podar(nivel, actual, sig, modo, "máx. tramos", ""); continue; }
```
Resultado: chico 3000 / 3151 / 4200 s; grande **sin solución** → entra la fase 2 y muestra ruta truncada. Bueno para mostrar el objetivo secundario en vivo.

### Mod 14 – TRAMPA: "las mejores rutas son las que llegan con más batería"
Cambiar el orden en `comparar` (l. 277):
```java
if (Math.abs(a.bateriaFinal() - b.bateriaFinal()) > EPS) return Double.compare(b.bateriaFinal(), a.bateriaFinal());
if (Math.abs(a.tiempoTotal - b.tiempoTotal) > EPS) return Double.compare(a.tiempoTotal, b.tiempoTotal);
```
Con cota, chico da una ruta de 5100 s como "mejor". **Está mal:** corriendo con `sincota` la mejor real es `A348 > A613 > STARBUCKS > PATIO > A348`, 6000 s, batería final 56,25.
**Por qué:** la cota poda por **tiempo** ("ya supera en tiempo a la 3ª"), pero ahora el ranking es por **batería**: una ruta lenta puede ser la mejor. La cota dejó de ser una cota del criterio.
Arreglo mínimo: desactivar la poda por cota (`boolean cotas = false;` l. 182) o correr con `sincota`. Arreglo fino: una cota de batería (batería actual + recargas pendientes − consumo mínimo pendiente) contra la batería de la 3ª.
**Esta es la pregunta de nivel 10:** mostrar que entendés que el branch & bound depende del criterio.

### Mod 15 – Desempate: menos lugares antes que más batería
```java
if (a.pasos.size() != b.pasos.size()) return Integer.compare(a.pasos.size(), b.pasos.size());
return Double.compare(b.bateriaFinal(), a.bateriaFinal());
```
Resultado: igual en ambos mapas (los empates de 411 s tienen la misma cantidad de pasos). La cota sigue válida porque el primer criterio sigue siendo el tiempo.

### Mod 16 – Esperar consume batería (0,5 por minuto)
Dentro del bloque de espera, después de `t2 = entrada;` (l. 177):
```java
b2 -= espera / 60.0 * 0.5;
if (b2 <= 0) { podar(nivel, actual, sig, modo, "batería", "se agotó esperando"); continue; }
```
Resultado: chico #2 pasa de 3151 a **3301 s** (esperó 151 s y tuvo que cambiar un modo). Grande igual.

### Mod 17 – Probar primero el modo más lento
Invertir el orden del enum en `Modo` (PATAS_ARRIBA, CAMINANDO, SALTANDO).
Resultado: **mismo top-3**, pero más nodos: chico 356, grande 205.707 (vs 128.587).
**Para explicar:** el orden no cambia el resultado (el backtracking es exhaustivo), cambia cuán rápido se llena el top-3 y por lo tanto cuánto poda la cota.

### Mod 18 – Modo nuevo "corriendo" (×0,75 tiempo, ×1,5 batería, letra R)
```java
// Modo.java
PATAS_ARRIBA("Patas para arriba", 2.0, 0.45),
CORRIENDO("Corriendo", 0.75, 1.5);
// CargadorMapa.java, en el switch de letras (l. 117-119)
case 'R' -> modos.add(Modo.CORRIENDO);
```
Y en el `.txt`, agregar `R` a las conexiones (`ARISTA;A;B;30;CSR`). Resultado con un mapa grande con R en las 13 conexiones CS: mismo 411 s, pero 661.942 nodos (más ramas por conexión → la `g` de O((3g)ⁿ) crece).

---

## 4. Cosas que no requieren tocar código (decilo antes de ponerte a programar)

- **Otro origen:** `INICIO;Aula 501` en el mapa.
- **Grafo no dirigido:** `java -cp out uada.Main mapa.txt nodirigido`.
- **Sin esperar:** argumento `sinespera`.
- **Sin branch & bound:** argumento `sincota` (sirve para verificar que la cota no cambia el resultado).
- **Otro mapa:** cualquier `.txt` con el formato; el código no tiene datos del mapa grande.

---

## 5. Autoevaluación (responder en voz alta, sin mirar)

<details><summary>1. ¿Qué es un nodo del árbol de búsqueda en este TP?</summary>
Una decisión (vecino, modo) desde el estado actual: lugar, tiempo, batería y visitados. Cada lugar tiene hasta 3g hijos.
</details>

<details><summary>2. ¿Por qué la vuelta a 348 se trata distinto?</summary>
Porque 348 es origen y destino: está marcado visitado desde el inicio, solo se permite volver cuando están todas las obligatorias (poda "vuelta anticipada"), al cerrar no recarga y la batería puede quedar en 0 (no negativa).
</details>

<details><summary>3. ¿Qué pasa si quito el deshacer de `visitado`?</summary>
Después de explorar una rama, el lugar queda marcado para siempre y las ramas hermanas no pueden pasar por ahí: se pierden soluciones.
</details>

<details><summary>4. ¿Por qué la cota usa la entrada más barata de cada lugar y no Floyd/Dijkstra?</summary>
Por consigna solo backtracking + B&B. Además la entrada mínima es O(n + m) de calcular una vez y sigue siendo optimista.
</details>

<details><summary>5. Si no hay solución, ¿por qué hace una segunda pasada?</summary>
Porque las podas por cota sirven para el objetivo principal, pero pueden cortar ramas útiles para el secundario (la ruta truncada más avanzada). La fase 2 explora solo con las restricciones reales.
</details>

<details><summary>6. ¿Qué cambia en la complejidad si agrego un modo?</summary>
El factor de ramificación pasa de 3g a 4g: O((4g)ⁿ).
</details>

<details><summary>7. ¿Cómo demostrás que la poda no pierde la solución óptima?</summary>
Corriendo con y sin `sincota`: mismo top-3 con muchos menos nodos (chico 297 vs 465; grande 128.587 vs 12.601.408).
</details>

<details><summary>8. ¿Por qué Uada entra 1 s después del cierre?</summary>
El intervalo de cierre es inclusivo [desde, hasta]; en "hasta" sigue cerrado, entonces entra en hasta + 1.
</details>

<details><summary>9. Si el profe pide "las mejores rutas son las de menos lugares", ¿qué tocás?</summary>
`comparar` poniendo pasos primero, y desactivar o adaptar la cota de tiempo porque ya no mide el criterio (igual que la mod 14).
</details>

<details><summary>10. ¿Dónde se guarda la batería que se va a poder recargar todavía y para qué?</summary>
En `recargasPendientes`; se usa en la poda "batería insuficiente". Se resta al visitar un aula y se suma al deshacer.
</details>
