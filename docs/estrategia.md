# Estrategia de solución — "La misión de Uada"

Describe la solución implementada en `uada.MisionUada`. Sirve de base para las secciones de
estrategia, pseudocódigo y complejidad del informe.

## 1. El problema como backtracking

Todas las rutas posibles forman un árbol (6a Backtracking). La raíz es Uada en el Aula 348, con
t = 0 y batería 100. Cada arista del árbol es una decisión **(próximo lugar, modo de
movimiento)**. La solución es una lista ordenada de movimientos (X1, …, Xk), como en N reinas.

| Elemento | En este problema |
| --- | --- |
| Estado | lugar actual, tiempo acumulado, batería, `visitado[]`, obligatorios visitados, camino |
| Candidatos | cada conexión saliente del lugar actual × cada modo permitido (S, C, P, en ese orden) |
| Factibilidad (poda) | ver sección 5 |
| Estado solución | volver al Aula 348 con todos los obligatorios visitados y batería ≥ 0 |
| Objetivo | las 3 rutas de menor tiempo total (top-3) |
| Vuelta atrás | al volver de la recursión se desmarca el lugar, se restan los obligatorios y se quita el último movimiento |

Los modos se prueban empezando por el más rápido (S), así el top-3 se llena antes y la poda por
cota corta más ramas. No se puede usar memoización: el mismo lugar con otro tiempo, otra batería
u otro conjunto de visitados es un estado distinto, y las ventanas dependen del tiempo exacto de
llegada.

## 2. Decisiones tomadas ante ambigüedades de la consigna

| # | Ambigüedad | Decisión implementada |
| --- | --- | --- |
| D1 | Consumo al saltar o ir patas para arriba | Relativo a caminar el mismo tramo: `consumo = segundosMapa / 60 × factor` (C = 1; S = 2,2; P = 0,45) — `Modo.consumo` |
| D2 | "Aula 633" en la consigna | Errata: el destino final es el Aula 348 |
| D3 | Batería en 0 | En el camino tiene que quedar > 0. Al llegar al 348 final se acepta ≥ 0 |
| D4 | Llegar y recargar | Se controla la batería al llegar y recién después se recarga, con tope de 100. El 348 no recarga |
| D5 | Ventanas | Se evalúan con el tiempo de llegada. El intervalo es cerrado (`desde ≤ t ≤ hasta` está cerrado). No se puede esperar |
| D6 | Aristas dirigidas | Por defecto dirigidas. `GRAFO;NO_DIRIGIDO` en el archivo, o el argumento `nodirigido`, las vuelve bidireccionales |
| D7 | "3 mejores secuencias" | Distinta secuencia de lugares; para la misma secuencia se queda la asignación de modos más rápida. Los empates de tiempo se ordenan por orden de hallazgo, que es determinista |
| D8 | Ruta truncada | Más obligatorios visitados y, si empatan, menor distancia recta (x, y) desde el último lugar al Aula 348 |
| D9 | Aula 348 | Es el único lugar que se visita dos veces (inicio y fin) |

## 3. Reglas numéricas

| Modo | Tiempo del tramo | Batería consumida |
| --- | --- | --- |
| Saltando (S) | `s / 2` | `s / 60 × 2,2` |
| Caminando (C) | `s` | `s / 60` |
| Patas para arriba (P) | `s × 2` | `s / 60 × 0,45` |

Recarga en un aula = suma de los dígitos del número / 5 (`Estacion.recarga`). Por ejemplo:
613 → 2,0; 348 → 3,0; 501 → 1,2.

## 4. Pseudocódigo

```
función resolver():
    explorar()                                    // fase 1: todas las podas
    si top3 está vacío y se usa cota:
        explorar() sin podas por cota             // fase 2: solo para el objetivo secundario

función explorar():
    visitado[origen] ← verdadero
    backtrack(origen, t = 0, bat = 100, nivel = 1)

función backtrack(actual, t, bat, nivel):
    expandidos++
    actualizarMejorParcial(actual)                // candidata a ruta truncada (D8)
    para cada conexión c que sale de actual:
        sig ← otro extremo de c
        si sig es prohibido:                          podar(PROHIBIDA);  continuar
        esCierre ← (sig = origen)
        si esCierre y faltan obligatorios:            podar(VUELTA_ANTICIPADA); continuar
        si no esCierre y visitado[sig]:               podar(YA_VISITADA);  continuar
        para cada modo m permitido en c (S, C, P):
            analizados++
            t2 ← t + tiempo(c, m);   b2 ← bat − consumo(c, m)
            si (esCierre y b2 < 0) o (no esCierre y b2 ≤ 0):   podar(BATERIA);  continuar
            si sig está cerrado en t2:                       podar(VENTANA);  continuar
            b3 ← b2 + (esCierre ? 0 : min(recarga(sig), 100 − b2))
            si hay que aplicar cotas:                        // fase 1
                si no esCierre:
                    faltaT, faltaB ← suma de la entrada más barata de cada obligatorio pendiente + la del origen
                    si faltaT = ∞:                           podar(SIN_CAMINO);  continuar
                    si b3 + recargasPendientes < faltaB:     podar(BATERIA_INSUFICIENTE); continuar
                    si top3 lleno y t2 + faltaT ≥ tercera:   podar(COTA);  continuar
                sino si top3 lleno y t2 ≥ tercera:           podar(COTA);  continuar
            camino.agregar(movimiento(actual, sig, m, b3, t2))
            si esCierre:
                registrarSolucion(camino)                    // top-3 sin repetir secuencias (D7)
            sino:
                marcar sig;  backtrack(sig, t2, b3, nivel + 1);  desmarcar sig   // vuelta atrás
            camino.quitarUltimo()
```

## 5. Podas

1. **Factibilidad** (las restricciones del enunciado): zona prohibida, lugar ya visitado, vuelta
   anticipada al 348, batería y ventana temporal.
2. **Branch & bound** (cotas optimistas que se calculan solo a partir del grafo): todavía falta
   *entrar* a cada obligatorio pendiente y, al final, al 348. Cada entrada usa una conexión
   distinta, así que como mínimo cuesta la suma de la conexión de entrada más barata de cada uno
   (`minEntradaT` / `minEntradaB`).
   - *Sin camino*: a algún lugar que falta no entra ninguna conexión.
   - *Batería insuficiente*: aun recargando en todas las aulas pendientes, no alcanza para el
     consumo mínimo.
   - *Cota*: el tiempo actual más el tiempo mínimo que falta no mejora a la tercera mejor ruta.
3. Las cotas nunca sobreestiman lo que falta, así que no descartan soluciones. El benchmark
   verifica que con cota y sin cota se obtiene el mismo top-3.

## 6. Contingencia (sin solución)

Si la fase 1 termina sin rutas, la fase 2 explora sin cotas: las cotas son válidas para el
objetivo principal, pero podrían cortar ramas útiles para la ruta truncada. Se devuelve la mejor
parcial según D8, junto con la distancia recta al 348 y los obligatorios visitados.

## 7. Log de exploración

Una línea por nodo analizado, con sangría según el nivel:

```
[#12] Pasillo P3 -> Escalera (Saltando) t=40.0s bat=97.80 | oblig 0/4
[#13] Pasillo P3 -> Zona obras  ✘ PODA: zona prohibida
[#86] Aula 402 -> Aula 348 (Caminando) t=417.5s bat=…  ★ SOLUCIÓN -> entra al top-3 en posición 1
```

Al final se escriben las estadísticas: nodos analizados, nodos expandidos, soluciones encontradas
y podas por motivo. El log se corta a los 2.000.000 de líneas.

## 8. Complejidad temporal teórica

Sea `n` la cantidad de lugares y `d` el grado de salida máximo. Cada nivel prueba como máximo
`3d` candidatos, y la profundidad máxima es `n − 1`, porque cada lugar se visita una sola vez.
El costo por candidato es O(n), porque la cota recorre los obligatorios pendientes.

```
T(n) = 3d · T(n − 1) + O(d · n)
```

Por el método de sustracción (1b), con a = 3d > 1, b = 1 y k = 1: **T(n) ∈ O((3d)^n)**. En un
grafo completo queda O(n! · 3^n). Es exponencial, igual que N reinas (O(n^n)) y la suma de
subconjuntos (O(2^n)). La poda reduce los nodos en la práctica, pero no cambia el peor caso.
Espacio: O(n), por la pila de recursión, el camino y `visitado[]`.

## 9. Valores de referencia (cálculo a mano, confirmados con el programa)

| Mapa | Resultado |
| --- | --- |
| `mapa_grande.txt` (dirigido) | 3 rutas de **417,5 s**, con secuencias distintas. Todas llegan al Molinete en 247,5 s |
| `mapa_grande_sin_solucion.txt` (dirigido) | **Sin solución**: no entra ninguna arista al 348. Truncada con 4/4 obligatorios, termina en el **Aula 613**, a √9700 ≈ **98,49** |
| `mapa_grande_sin_solucion.txt nodirigido` | 3 rutas de **285,0 s** |
