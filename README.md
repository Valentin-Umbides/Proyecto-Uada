# TPO Programación III – La misión de Uada (Backtracking)

## Compilar
    javac -encoding UTF-8 -d out src/uada/*.java

## Ejecutar con un mapa en archivo (el que den los profes)
    java -cp out uada.Main mapas/mapa_grande.txt
    java -cp out uada.Main mapas/mapa_grande.txt nodirigido   # fuerza grafo no dirigido
    java -cp out uada.Main mapas/mapa_grande.txt sincota      # sin poda por cota (para verificar)

Genera `logs/log_<mapa>.txt` con el árbol de exploración, los nodos analizados y las podas.
Si en la consola de Windows se ven mal los acentos: `java -Dstdout.encoding=UTF-8 -cp out uada.Main ...`

## Formato del mapa (.txt)
    INICIO;Aula 348
    GRAFO;DIRIGIDO            (opcional; por defecto DIRIGIDO; o NO_DIRIGIDO)
    LUGAR;nombre;aula(0 si no);x;y;obligatorio(0/1);prohibido(0/1)
    ARISTA;origen;destino;segundos;CSP     (C caminando, S saltando, P patas para arriba)
    VENTANA;lugar;desde;hasta              (cerrado en [desde, hasta] segundos)
Las líneas con # son comentarios.

## Mapas de ejemplo cargados en código
    java -cp out uada.Main base        # mapa UADE de 13 lugares (el del informe)
    java -cp out uada.Main chico       # 6 lugares: log corto para mostrar en la defensa
    java -cp out uada.Main imposible   # sin solución -> objetivo secundario

## Complejidad práctica
    java -cp out uada.Benchmark        # genera complejidad_practica.csv (tarda varios minutos)
    python3 graficar.py                # genera grafico_complejidad.png

## Clases
- `MisionUada`   : el backtracking (podas, branch & bound, top-3, objetivo secundario, log)
- `CargadorMapa` : lee el mapa desde .txt
- `Mapa`, `Estacion`, `Conexion`, `Ventana`, `Modo` : modelo del grafo
- `Movimiento`, `Ruta` : pasos y rutas resultantes
- `Escenarios`   : mapas de ejemplo y generador de mapas aleatorios
- `Benchmark`    : mediciones de tiempo para distintos n

## Logs
Todos los logs se guardan en `logs/` (cada ejecución pisa el log anterior del mismo mapa).
En el repo están el del mapa chico (para mostrar en la defensa) y el de `mapa_grande.txt`.
Los demás pesan cientos de MB, así que no se suben: se regeneran corriendo el programa.

## Documentación
- [`docs/INFORME.md`](docs/INFORME.md): informe del TP (estrategia, pseudocódigo, complejidad, resultados, comparativo, conclusiones).
- [`docs/PUNTEO.md`](docs/PUNTEO.md): resumen y explicación para el grupo, con preguntas probables de la defensa.
