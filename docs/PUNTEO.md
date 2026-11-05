# TPO Uada – Punteo para el grupo

Valentín Umbides Álvarez · Actualizado: 4 de noviembre de 2026

Tenemos el TP resuelto de punta a punta: código Java que funciona con cualquier mapa en `.txt` (incluidos los de la cátedra) y el informe completo ([INFORME.md](INFORME.md)). La entrega y defensa es el **11/11** y la nota es individual: todos tenemos que poder explicar cualquier parte.

## Qué pide el TP, en una frase

Uada sale del Aula 348, tiene que pasar por todos los lugares obligatorios y volver al 348 **en el menor tiempo**, sin quedarse sin batería, sin pisar zonas prohibidas, sin repetir lugares y sin entrar a un lugar cuando está cerrado. Hay que devolver las **3 mejores rutas** usando **backtracking con branch & bound** y, si no hay solución, la ruta que llegó más cerca.

## Punteo de lo que hicimos

1. **Interpretamos el enunciado** y dejamos por escrito cada supuesto:
    - Caminar gasta 1 punto de batería por minuto. Saltar tarda la mitad y gasta 2,2 veces más. Patas para arriba tarda el doble y gasta 55% menos.
    - Las aulas recargan (suma de dígitos)/5, con tope de 100.
    - La batería debe quedar > 0 después de cada movimiento (al llegar al final, ≥ 0).
    - Una ventana cerrada se mira en el **momento de llegada**. Si está cerrado, Uada **espera** en la puerta y entra 1 s después del cierre: gasta tiempo, no batería (confirmado por la profe).
    - Si dos rutas empatan en tiempo, gana la de más batería final y después la que pasa por menos lugares (pedido por la profe).
    - El "aula 633" del enunciado es un error de tipeo por el aula 348 (confirmado por la profe).
2. **Armamos un mapa de ejemplo de UADE** (13 lugares) donde la batería sí importa: recorrer todo caminando gasta más de 100.
3. **Programamos el backtracking en Java** (`MisionUada.java`): prueba todas las rutas posibles, guarda las 3 mejores y deshace cada paso al volver.
4. **Agregamos 7 podas** para no explorar ramas inútiles (sección de podas más abajo).
5. **Objetivo secundario:** si no hay solución, devuelve la ruta que visitó más obligatorios y quedó más cerca del 348.
6. **Log del árbol:** cada decisión queda escrita en `logs/log_<mapa>.txt` con su nivel, su número de nodo y el motivo de cada poda.
7. **Lector de mapas desde `.txt`** (`CargadorMapa.java`): acepta el formato de la cátedra, grafos dirigidos o no, y mapas con o sin coordenadas.
8. **Probamos con los dos archivos de prueba de la cátedra** (uno con solución y otro sin solución), y dan lo esperado:
    - `mapa_grande.txt` → 3 rutas distintas de 411 s (sin poder esperar daba 417,5 s; se puede ver con la opción `sinespera`).
    - `mapa_grande_sin_solucion.txt` → ruta truncada que visita los 4 obligatorios.
9. **Verificamos que las podas no rompen nada:** corrimos con y sin poda y siempre salió el mismo top-3.
    - Además hay 60 tests automáticos (`mvn test`), y uno re-simula cada ruta devuelta para comprobar todas las reglas.
10. **Medimos la complejidad práctica:** mapas aleatorios de 5 a 18 lugares, con tiempos y nodos graficados.
11. **Escribimos el informe** con todos los entregables: estrategia, pseudocódigo, complejidad, log, gráfico, análisis comparativo, escenarios reales y conclusiones.

## Cómo funciona el backtracking

Es como recorrer un laberinto con una tiza: avanzás por un pasillo, y si te trabás, volvés a la última bifurcación, borrás la marca y probás el pasillo siguiente. Al final probaste todos los caminos, pero de a uno y sin copiar nada.

En nuestro código, cada "paso" es una **decisión doble**: a qué lugar vecino ir y con qué modo (saltando, caminando o patas para arriba). La función `backtrack(actual, tiempo, batería)` hace siempre lo mismo:

1. Para cada vecino y cada modo permitido en esa conexión, calcula el tiempo y la batería nuevos.
2. Si la opción viola una regla (batería, ventana, lugar repetido o prohibido), la **poda** y pasa a la siguiente.
3. Si es válida, **HACE**: marca el lugar como visitado, suma la recarga y agrega el paso al camino.
4. Si llegó de vuelta al 348 con todos los obligatorios, es una **solución**: la compara con el top-3.
5. Si no, se **llama a sí misma** desde el lugar nuevo (baja un nivel en el árbol).
6. Al volver de la recursión, **DESHACE**: desmarca el lugar y saca el paso, y prueba la opción siguiente.

Ejemplo real del log del mapa chico (`java -cp out uada.Main chico`). La sangría es la profundidad en el árbol:

```
[#1] RAÍZ: A348 | t=0.0s | bat=100.00 | obligatorias 0/2
  [#2] A348 -> PASILLO3 (Saltando) t=300.0s bat=78.00            <- decide saltar al pasillo
    [#3] PASILLO3 -> A348  ✘ PODA: vuelta anticipada             <- no puede volver, faltan obligatorios
    [#4] PASILLO3 -> A613 (Saltando) t=750.0s bat=47.00 (+2.0 recarga)
      [#7] A613 -> STARBUCKS (Saltando)  ✘ PODA: batería (quedaría -8.00)
      [#8] A613 -> STARBUCKS (Caminando) t=2250.0s bat=22.00   <- caminando sí le alcanza
        [#11] STARBUCKS -> PATIO (Saltando) t=2400.0s bat=11.00
          [#12] PATIO -> A348 (Caminando)  ✘ PODA: batería
          [#13] PATIO -> A348 (Patas para arriba) t=4200.0s  ★ SOLUCIÓN (1º del top-3)
        [#16] STARBUCKS -> PATIO (Caminando) t=2550.0s bat=17.00  <- volvió atrás y probó otro modo
          [#17] PATIO -> A348 (Caminando) t=3450.0s  ★ SOLUCIÓN, mejor que la anterior
```

Acá se ve lo central del TP: **saltar es más rápido pero gasta más batería**. El algoritmo descubre solo en qué tramos conviene saltar y en cuáles caminar, porque prueba todas las combinaciones.

**Las 3 mejores:** el top-3 es una lista ordenada por tiempo, y las 3 rutas tienen que pasar por **lugares distintos**. Si aparece una ruta con los mismos lugares que otra que ya está en el top-3 (solo cambia el modo de algún tramo), se queda la más rápida de las dos. En el nodo #17 de arriba pasa eso: misma secuencia que la #13 pero más rápida, así que la reemplaza.

**Objetivo secundario:** mientras explora, el programa recuerda la ruta parcial "más avanzada": la que visitó más obligatorios y, a igualdad, terminó más cerca en línea recta del 348. Si al final no hubo ninguna solución, devuelve esa.

## Las podas, explicadas

Podar es cortar una rama del árbol antes de recorrerla. Hay dos tipos: las **reglas del enunciado** (si se rompen, la ruta es inválida) y las **cotas** del branch & bound (la ruta podría ser válida, pero ya sabemos que no puede entrar al top-3).

| # | Poda | Tipo | Cuándo corta |
|---|---|---|---|
| 1 | Zona prohibida | Regla | El lugar está marcado como prohibido |
| 2 | Ya visitada | Regla | El lugar ya se pisó (se pasa una sola vez) |
| 3 | Vuelta anticipada | Regla | Quiere volver al 348 y faltan obligatorios |
| 4 | Batería | Regla | Después del movimiento quedaría en 0 o menos |
| 5 | Ventana temporal | Regla | Llega justo cuando el lugar está cerrado |
| 6 | Batería insuficiente / sin camino | Cota | Aun recargando en todas las aulas que faltan, no alcanza para terminar, o no hay camino posible |
| 7 | Cota de tiempo | Cota | Tiempo actual + lo mínimo que falta ≥ la 3ª mejor ruta encontrada |

**¿Cómo sabe "lo mínimo que falta"?** Sin usar ningún otro algoritmo. A cada obligatorio pendiente todavía hay que **entrar** una vez, y al final hay que entrar al 348. Al principio, el programa recorre las aristas y guarda, para cada lugar, su **conexión de entrada más barata** (con el modo más rápido). Lo que falta cuesta como mínimo la suma de esas entradas.

Ejemplo con el mapa base: la entrada más rápida a BIBLIO es de 240 s y la del 348 es de 150 s. Si Uada lleva 4400 s, le falta solo BIBLIO y la 3ª mejor ruta tarda 4695 s, la cota es 4400 + 240 + 150 = 4790 ≥ 4695: esa rama se poda sin recorrerla.

**¿Por qué la cota no hace perder soluciones?** La estimación es optimista: supone el mejor modo y la entrada más barata para cada lugar, así que **nunca sobreestima** lo que falta. Si ni siquiera en el mejor caso imaginable se mejora la 3ª ruta, en la realidad tampoco. Lo comprobamos: con y sin cota el top-3 es idéntico. La cota ahorra unas 2,3 veces los nodos en el mapa base y unas 140 veces en `mapa_grande.txt`.

**Detalle importante (fase 2):** las cotas sirven para el objetivo principal, pero pueden borrar la información del secundario. En `mapa_grande_sin_solucion.txt` no hay ninguna arista que vuelva al 348, así que la cota cortaba todo desde el primer paso y no quedaba ruta truncada. Por eso, si la primera pasada termina sin soluciones, el programa **explora de nuevo solo con las reglas 1 a 5** para encontrar la ruta que llegó más cerca.

**Solo backtracking con branch & bound (indicación de la profe):** antes usábamos Floyd para calcular la cota y lo sacamos. Ahora la cota se calcula directo sobre el grafo. ⚠ El detalle de branch & bound no está en el material de la materia (ahí el backtracking solo aparece en la clase de búsqueda en profundidad), así que conviene saber explicarlo.

## Cómo correrlo con el mapa de los profes

No hay que tocar código: se escribe el mapa en un `.txt` y se pasa como argumento.

1. Compilar una vez: `mvn package` (o sin Maven: `javac -encoding UTF-8 -d out src/main/java/uada/*.java` y usar `-cp out`)
2. Copiar `mapas/mapa_grande.txt` y reemplazar los datos con los del mapa nuevo:

```
INICIO;Aula 348
LUGAR;nombre;aula(0 si no);x;y;obligatorio(0/1);prohibido(0/1)
ARISTA;origen;destino;segundos;CSP
VENTANA;lugar;desde;hasta
```

3. Correr: `java -cp target/classes uada.Main mapas/mapa_profes.txt`
4. Ver el resultado en consola y el árbol en `logs/log_mapa_profes.txt`.

Cosas a tener en cuenta:

- **Mapa dirigido (confirmado):** el mapa de los profes viene con direcciones, como los de prueba, y el programa ya lo lee así por defecto: cada `ARISTA` va solo de origen a destino. No hay que agregar nada al archivo ni al comando.
- **Errores de tipeo:** el programa dice la línea exacta (por ejemplo, `Línea 3: modo desconocido 'Z' (usar C, S o P)`).
- **Sin coordenadas:** si el mapa no trae x/y, funciona igual. Para la ruta truncada solo cuenta cuántos obligatorios visitó (sin coordenadas no hay distancia recta).
- **Tamaño:** hasta unos 16 lugares responde en menos de un cuarto de segundo; con 18 tarda unos 4 s. Los archivos de la profe (20 lugares) tardan menos de 1,5 s. Un mapa de 25 o más lugares muy conectados podría tardar mucho.
- **Si piden un cambio en vivo:** casi todo está en `MisionUada.backtrack()`. Ahí se agrega una regla nueva como una poda más (`if (...) { podar(...); continue; }`).

## Complejidad y otras técnicas, en simple

**Teórica: O((3g)ⁿ), exponencial.** *n* es la cantidad de lugares y *g* la cantidad máxima de vecinos de un lugar. Sale del **método de sustracción** de la clase 1b:

- Cada llamada hace hasta **a = 3g** llamadas recursivas (g vecinos × 3 modos).
- Cada llamada reduce el problema en **b = 1** lugar, porque no se repiten lugares.
- Como a > 1, el resultado es T(n) ∈ O(aⁿ) = O((3g)ⁿ).

Calcular las entradas más baratas cuesta O(n + m) una sola vez (m = cantidad de aristas), que es despreciable. Las podas no cambian el peor caso: un mapa sin solución se recorre entero.

**Práctica:** medimos con mapas aleatorios de 5 a 18 lugares (5 mapas por tamaño). Cada lugar agregado multiplica los nodos por ~2,5 con poda y por ~4,9 sin poda. En escala logarítmica las curvas son rectas, que es la firma de una exponencial: coincide con la teórica. El gráfico está en [`grafico_complejidad.png`](../grafico_complejidad.png).

**¿Por qué no otras técnicas?** El TP pide proponer otra y explicar por qué se descarta. Cada una falla por algo concreto:

- **Greedy:** elige siempre el vecino más cercano y nunca vuelve atrás. Puede quedarse sin batería o chocar con una ventana cerrada, y da una sola ruta en vez de 3. Es rápido, pero no garantiza el óptimo.
- **Dijkstra y Floyd:** dan caminos mínimos entre dos puntos, no un circuito que pase por todos los obligatorios sin repetir lugares. Por eso no sirven, y por indicación de la profe tampoco se usan como ayuda.
- **Programación Dinámica (como el viajante de la guía):** funcionaría si el estado guardara también el tiempo y la batería, porque las ventanas dependen del horario. Pero esos valores son decimales y la tabla se vuelve gigante.
- **Divide y Conquista:** no se puede partir el mapa en partes independientes. La batería, el horario y los lugares ya visitados de una parte afectan a la otra.

**Respuesta corta si nos preguntan:** usamos solo backtracking con branch & bound, como pidió la cátedra. La cota es una estimación optimista que se calcula sobre el propio grafo, sin otros algoritmos.

## Preguntas probables en la defensa

| Pregunta | Respuesta corta |
|---|---|
| ¿Dónde está el "volver atrás"? | En `backtrack()`, después de la llamada recursiva: `visitado[sig] = false` y `camino.removeLast()` |
| ¿Qué es un nodo del árbol? | Cada opción evaluada (vecino + modo). Los que pasan las podas generan una llamada recursiva |
| ¿Cómo garantizan que es el óptimo? | Se exploran todas las ramas salvo las podadas, y las podas solo cortan lo inválido o lo que no puede mejorar. Verificado con y sin cota |
| ¿Cómo eligen las 3 mejores? | Ordenadas por tiempo total; si empatan, más batería y después menos lugares (`MisionUada.comparar`). Con secuencias de lugares distintas; si dos rutas pasan por los mismos lugares, queda la mejor |
| ¿Qué pasa si llega a un lugar cerrado? | Espera en la puerta y entra 1 s después del cierre (`MARGEN_ESPERA`); suma tiempo, no batería. Si el cierre no termina nunca, se poda |
| ¿Por qué se prueba primero saltar? | Es el modo más rápido: llena antes el top-3 y la cota empieza a podar más temprano |
| ¿Qué pasa si no hay solución? | Devuelve la ruta truncada con más obligatorios y más cerca del 348 (con fase 2 si hace falta). Nunca devuelve vacío |
| ¿Cómo se calcula la recarga? | (suma de dígitos del aula)/5 al llegar. Ej.: 613 → (6+1+3)/5 = 2 |
| ¿Cómo se calcula la complejidad? | Sustracción: a = 3g, b = 1, a > 1 → O((3g)ⁿ) |
| ¿Qué es el branch & bound acá? | La cota: tiempo actual + la suma de las entradas más baratas de lo que falta. Si eso ya no mejora la 3ª ruta, se poda |
| Agreguen una regla nueva (ej.: no saltar en el 6º piso) | Una poda más en `backtrack()` antes de "HACER" |
| Cambien a 5 mejores rutas | Cambiar la constante `TOP = 3` en `MisionUada` |
| ¿Qué pasa con un mapa no dirigido? | `GRAFO;NO_DIRIGIDO`: cada arista se agrega en las dos listas de adyacencia |

## Decisiones y próximos pasos

Confirmado con la cátedra:

- [x] El mapa es dirigido: viene con direcciones.
- [x] "Aula 633" = aula 348: era un error de tipeo.
- [x] La batería tiene tope de 100.
- [x] Uada puede esperar a que abra un lugar: gasta tiempo, no batería.
- [x] Empates del top-3: más batería y después menos lugares.
- [x] Técnica: solo backtracking con branch & bound (Floyd ya se sacó).
- [x] Las 3 mejores rutas pasan por lugares distintos.

Para el grupo:

- [ ] Que cada uno clone el repo, compile y corra `chico`, `mapa_grande.txt` y `mapa_grande_sin_solucion.txt`
- [ ] Leer juntos `MisionUada.backtrack()` línea por línea
- [ ] Revisar el informe y ajustar lo que no nos convenza (mapa, supuestos, redacción)
- [ ] Repartir quién explica cada parte: estrategia y podas, código, complejidad, comparativo y escenarios reales
- [ ] Practicar un cambio en vivo (por ejemplo, una regla nueva o `TOP = 5`)
- [ ] Exportar el informe a PDF y mandarlo por el mail de UADE antes del 11/11
