# Plan de implementación por iteraciones

Se parte del código que ya está en `main`: `src/uada`, hecho por Valentin. Esa implementación ya
da los valores correctos de la sección 9 de [estrategia.md](estrategia.md). Las iteraciones
agregan tests, ordenan el proyecto y completan los entregables, sin reescribir lo que funciona.

## Reglas de cada iteración

1. Una rama por iteración, con el nombre `features/<nombre>` y creada desde `main` actualizado.
2. Los tests se escriben primero (TDD). Si un test destapa un error, se corrige en la misma rama.
3. Antes de hacer push:
   - `mvn test` en verde (todos los tests, no solo los nuevos);
   - el programa corrido sobre los 3 mapas de referencia, con los resultados de la sección 9 de la estrategia.
4. Un pull request contra `main` por iteración. La siguiente iteración arranca cuando el PR anterior está mergeado.
5. Los commits no llevan coautoría.

Comando de Maven: si `mvn` no está en el PATH, usar el que está en
`%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\…\bin\mvn` o el Maven que trae IntelliJ.

## Iteraciones

| # | Rama | Qué entrega | Depende de |
| --- | --- | --- | --- |
| 0 | `features/plan-iteraciones` | `docs/estrategia.md` y este plan | — |
| 1 | `features/maven-y-tests-modelo` | Proyecto Maven y tests del modelo y del lector de mapas | — |
| 2 | `features/tests-backtracking` | Tests del algoritmo: mapas chicos, mapas reales y "con cota = sin cota" | 1 |
| 2b | `features/desempate-top3` | Desempate del top-3: más batería y después menos lugares (pedido por la profe) | 2 |
| 2c | `features/espera-en-ventanas` | Uada puede esperar a que abra un lugar: gasta tiempo, no batería (confirmado por la profe) | 2b |
| 3 | `features/tests-log-y-reporte` | Tests del log, del reporte y de los argumentos de `Main` | 2c |
| 4 | `features/benchmark-reproducible` | Tests del generador, benchmark configurable, CSV y gráfico regenerados | 1 |
| 5 | `features/informe` | Informe completo con todos los entregables | 2c, 3 y 4 |

Las iteraciones 2 y 4 son independientes entre sí, así que se pueden trabajar en paralelo
después de la 1.

---

### Iteración 1 — `features/maven-y-tests-modelo`

**Archivos**
- Mover `src/uada/*.java` → `src/main/java/uada/` con `git mv`, que conserva el historial.
- Crear `pom.xml`: `groupId` `uada`, `artifactId` `mision-uada`, Java 21, UTF-8, `junit-jupiter` 5.10.0 (scope test) y `maven-surefire-plugin` 3.2.5.
- `.gitignore`: agregar `target/`.
- `README.md`: actualizar la sección Compilar con `mvn test` / `mvn package` y el `javac` con la nueva ruta.
- Crear los tests en `src/test/java/uada/`: `ModoTest`, `EstacionTest`, `MapaTest` y `CargadorMapaTest`.

**Casos de prueba** (deltas de 1e-9)
- `ModoTest`
  - Una conexión de 55 s tarda 27,5 saltando, 55 caminando y 110 patas para arriba.
  - Consumo: 120 s caminando = 2,0; 60 s saltando = 2,2; 60 s patas para arriba = 0,45.
  - El orden de prueba es SALTANDO, CAMINANDO, PATAS_ARRIBA.
- `EstacionTest`
  - Recarga: 613 → 2,0; 348 → 3,0; 501 → 1,2; no aula → 0.
  - Ventana [100, 130]: cerrada en 100, 115 y 130; abierta en 99,5 y 130,5. Si hay dos ventanas, alcanza con que una esté cerrada.
  - Distancia recta de (0,0) a (90,−40) = √9700.
- `MapaTest`
  - Rechaza con `IllegalArgumentException`: lugar repetido, conexión de un lugar consigo mismo, segundos ≤ 0, conexión sin modos, conexión duplicada y estación inexistente.
  - En un grafo dirigido, `a→b` no agrega `b→a`, y `a→b` y `b→a` pueden coexistir. En uno no dirigido se agrega en los dos sentidos.
- `CargadorMapaTest`
  - `mapa_grande.txt`: dirigido; 20 lugares; 32 conexiones; obligatorios exactamente {Aula 613, Molinete Lima 3, Starbucks, Auditorio}; prohibida solo `Zona obras`; 3 lugares con ventana; origen `Aula 348`; la única salida del Aula 402 va al Aula 348, con modos {C, P}; usa coordenadas.
  - `mapa_grande_sin_solucion.txt`: dirigido; 31 conexiones; ninguna conexión entra al Aula 348.
  - El mismo archivo con `nodirigido` forzado: 31 conexiones; el Aula 348 tiene 3 adyacentes (Pasillo P3, Aula 501, Aula 402).
  - Un archivo temporal con BOM, CRLF, líneas vacías, comentarios y espacios alrededor de `;` se lee bien.
  - La línea `GRAFO;NO_DIRIGIDO` hace el grafo no dirigido.
  - El formato alternativo `LUGAR;codigo;nombre;aula;tipo;x;y` funciona.
  - Errores: un lugar inexistente, un modo `X`, un número inválido o una instrucción desconocida lanzan `IllegalArgumentException` con un mensaje que empieza con `Línea N:`. Un archivo sin `INICIO` y sin Aula 348 da `Falta la línea INICIO`.

**Verificación**
- `mvn test` en verde.
- `mvn -q package`, después `java -cp target/classes uada.Main mapas/mapa_grande.txt` → 417,5 s.

### Iteración 2 — `features/tests-backtracking`

**Archivos**: `src/test/java/uada/MisionUadaTest.java`, `src/test/java/uada/ValidadorRuta.java`
y mapas chicos en `src/test/resources/mapas/`.

**ValidadorRuta**: re-simula una ruta usando solo `Modo` y `Estacion`, y falla si:
- no empieza y termina en el origen;
- le falta un obligatorio;
- pasa por un prohibido;
- repite un lugar;
- usa una conexión o un modo inexistente;
- llega a un lugar cerrado;
- la batería queda ≤ 0 en el camino o < 0 al final;
- el tiempo o la batería de cada `Movimiento` no coinciden con la re-simulación.

**Mapas chicos y resultados esperados** (Aula 348 en (0,0) en todos)

| Archivo | Contenido | Esperado |
| --- | --- | --- |
| `mini_ida_vuelta.txt` | A obligatorio (10,0); 348→A y A→348 de 10 s CSP | 1 ruta, 10,0 s, saltando los dos tramos, batería ≈ 99,2667 |
| `mini_ventana.txt` | ídem + `VENTANA;A;4;6` | 15,0 s; el primer tramo es caminando; poda por ventana ≥ 1 |
| `mini_prohibido.txt` | X prohibido; 348→X→A de 1 s; 348→B→A y A→348 de 20 s C | 348 > B > A > 348, 60,0 s; poda zona prohibida ≥ 1 |
| `mini_dos_obligatorios.txt` | A y B obligatorios; 348→A, A→348, A→B, B→348 de 10 s CSP | 1 ruta 348 > A > B > 348, 15,0 s; poda vuelta anticipada ≥ 1 |
| `mini_bateria.txt` | A obligatorio en (30,40); 348→A y A→348 de 3600 s C | sin solución; truncada termina en A, distancia 50,0, 1/1 obligatorios |
| `mini_bateria_justa.txt` | ídem con 3000 s | tiene solución; batería final 0,0 |

**Mapas reales**
- `mapa_grande.txt`: 3 rutas de 417,5 s con secuencias distintas.
- `mapa_grande_sin_solucion.txt`: sin solución; truncada en el Aula 613 a 98,4886.
- `mapa_grande_sin_solucion.txt` no dirigido: 3 rutas de 285,0 s.
- Con cota y sin cota: mismos tiempos y secuencias del top-3 en `mapa_grande`, y menos nodos analizados con cota.

Si se necesitan getters (`getMejorParcial`, estadísticas), se agregan sin cambiar el algoritmo.

### Iteración 3 — `features/tests-log-y-reporte`

- La cantidad de líneas del log que empiezan con `[#` es igual a `getNodosAnalizados()`, mientras el log no se trunque.
- El log de `mini_ventana` contiene `esperó 2.0s` y `★ SOLUCIÓN`; con `sinespera`, contiene `PODA: ventana temporal`.
- Con `maxLineasLog = 5` aparece la línea `log truncado`.
- `reporte()` con solución contiene `Ruta #1`, `Tiempo total: 417.5 s` y la tabla con las columnas Origen, Destino, Movimiento, Batería y T. acumulado. Sin solución contiene `NO EXISTE SOLUCIÓN`, `Punto máximo de avance: Aula 613` y `Distancia recta`.
- `Main` con un archivo inexistente imprime `No se encontró el archivo` y no lanza excepción. Con una opción desconocida imprime `Opción ignorada`.
- Si se encuentran problemas de formato (decimales con coma según el locale, acentos), se corrigen en esta iteración.

### Iteración 4 — `features/benchmark-reproducible`

- `EscenariosTest`:
  - `aleatorio(n, 0.35, semilla)` con la misma semilla produce el mismo mapa (mismos lugares, conexiones y obligatorios);
  - siempre tiene `n` lugares, y el `N0` es un aula 348;
  - `uade()`, `uadeChico()` y `uadeImposible()` se resuelven (el imposible sin solución).
- `Benchmark` acepta argumentos opcionales `nMin nMax semillas`. Los valores por defecto son los actuales.
- Test `BenchmarkTest` con n de 5 a 8: con cota y sin cota dan el mismo top-3 en todas las semillas.
- Regenerar `complejidad_practica.csv` y `grafico_complejidad.png` con `python graficar.py`, y commitearlos.

### Iteración 5 — `features/informe`

Valentin ya subió `docs/INFORME.md` y `docs/PUNTEO.md`, que cubren todos los entregables. Esta
iteración **revisa y completa** ese informe; no escribe uno nuevo. Después se exporta a PDF para
el mail.

1. Consultar al grupo si el resultado principal tiene que ser `mapa_grande.txt` (el de la
   cátedra) en lugar del mapa propio de 13 lugares. Si es así, pasar la sección "Prueba con los
   archivos de la cátedra" al frente.
2. Log: agregar un extracto de `logs/log_mapa_grande.txt` con la poda por ventana en el Patio
   (t = 150) y en el Molinete (t = 200). Muestra por qué ir siempre lo más rápido no sirve.
3. Análisis comparativo: sumar el contraejemplo de Greedy en el mapa de la cátedra (con el
   movimiento más rápido llega al Patio a 150 s y al Molinete a 200 s, los dos cerrados).
4. Agregar una sección de verificación: los tests automáticos (cantidad y qué cubren), y que con
   cota y sin cota se obtiene el mismo top-3.
5. Actualizar las instrucciones de compilación a Maven.

**Verificación**: todos los números del informe tienen que coincidir con los que da el programa
y con la sección 9 de la estrategia.

## Cronograma (entrega 11/11)

| Día | Iteraciones |
| --- | --- |
| Mié 4/11 | 0 y 1 |
| Jue 5/11 | 2, y en paralelo 4 |
| Vie 6/11 | 3 |
| Sáb 7 – Lun 9/11 | 5 (informe) |
| Mar 10/11 | Ensayo de la defensa: cada integrante explica `backtrack()` y hace un cambio en vivo |
