package uada;

import java.io.PrintWriter;
import java.util.*;

/**
 * Backtracking que busca las 3 rutas de menor tiempo para la misión de Uada.
 *
 * Estado del árbol de exploración: (estación actual, visitados, tiempo acumulado, batería).
 * Cada nivel del árbol elige el próximo movimiento: (conexión vecina, modo permitido).
 *
 * Podas (funciones de factibilidad):
 *   1. Zona prohibida                -> no se entra nunca.
 *   2. Estación ya visitada          -> se pasa una única vez por cada lugar.
 *   3. Volver a A348 sin completar   -> no se puede cerrar el circuito.
 *   4. Batería                       -> la batería no puede llegar a 0 antes del destino final.
 *   5. Ventana temporal              -> la estación está cerrada en el tiempo de llegada.
 *
 * Branch & Bound (cotas optimistas, calculadas solo con el grafo, sin otros algoritmos):
 *   Todavía falta ENTRAR a cada obligatorio pendiente y, al final, ENTRAR a A348. Cada entrada usa
 *   una conexión distinta, así que lo que falta cuesta como mínimo la suma, para cada uno de esos
 *   lugares, de su conexión de entrada más barata (con el modo más rápido / de menor consumo).
 *   6. Sin camino                    -> algún lugar que falta no tiene ninguna conexión de entrada.
 *   7. Batería insuficiente          -> aun recargando en todas las aulas pendientes, la batería no
 *                                       alcanza para ese consumo mínimo.
 *   8. Cota de tiempo                -> tiempo + ese tiempo mínimo no mejora la 3ra mejor ruta.
 */
public class MisionUada {

    public static final double BATERIA_INICIAL = 100.0;
    public static final double BATERIA_MAXIMA = 100.0;
    public static final int TOP = 3;

    private final Mapa mapa;
    private final Estacion origen;
    private final int totalObligatorias;
    private final boolean usarCota;
    private boolean faseSecundaria = false;   // true = 2da pasada solo para el objetivo secundario
    private long nodosFase1 = -1;

    // ---- estado del backtracking ----
    private final boolean[] visitado;
    private final Deque<Movimiento> camino = new ArrayDeque<>();
    private int obligatoriasVisitadas = 0;

    // ---- resultados ----
    private final List<Ruta> mejores = new ArrayList<>();      // top-3 ordenado por tiempo
    private Ruta mejorParcial = new Ruta(Collections.emptyList());
    private int mejorParcialObligatorias = -1;
    private double mejorParcialDistancia = Double.MAX_VALUE;

    // ---- cotas: conexión de entrada más barata de cada lugar (tiempo y consumo) ----
    private final double[] minEntradaT;
    private final double[] minEntradaB;
    private double recargasPendientes;   // suma de recargas de las aulas aún no visitadas

    // ---- trazabilidad ----
    private long nodosAnalizados = 0;   // nodos del árbol generados (candidatos evaluados)
    private long nodosExpandidos = 0;   // llamadas recursivas (estados válidos)
    private long solucionesEncontradas = 0;
    private final Map<String, Long> podas = new LinkedHashMap<>();
    private final PrintWriter log;      // null = sin log (para mediciones)
    private long lineasLog = 0;
    private final long maxLineasLog;

    public MisionUada(Mapa mapa, String codigoOrigen, boolean usarCota, PrintWriter log, long maxLineasLog) {
        this.mapa = mapa;
        this.origen = mapa.get(codigoOrigen);
        this.usarCota = usarCota;
        this.log = log;
        this.maxLineasLog = maxLineasLog;
        this.visitado = new boolean[mapa.size()];
        int k = 0;
        for (Estacion e : mapa.getEstaciones()) if (e.isObligatoria()) k++;
        this.totalObligatorias = k;
        this.minEntradaT = new double[mapa.size()];
        this.minEntradaB = new double[mapa.size()];
        calcularEntradasMinimas();
        for (String p : new String[]{"zona prohibida", "ya visitada", "vuelta anticipada",
                "batería", "ventana temporal", "sin camino", "batería insuficiente", "cota"}) podas.put(p, 0L);
    }

    // =====================================================================
    //  BACKTRACKING
    // =====================================================================

    public void resolver() {
        nodosAnalizados = 0;
        nodosExpandidos = 0;
        escribir(0, "=== FASE 1: búsqueda de las 3 mejores rutas (con todas las podas) ===");
        explorar();
        if (mejores.isEmpty() && usarCota) {
            // Sin solución: las podas por cota son válidas para el objetivo principal, pero pueden cortar
            // ramas que sirven para el objetivo secundario (ej.: si no hay camino de regreso al origen,
            // la cota corta todo desde el primer paso). Se repite la exploración solo con las
            // restricciones reales del enunciado para hallar la ruta truncada más avanzada.
            faseSecundaria = true;
            nodosFase1 = nodosAnalizados;
            escribir(0, "");
            escribir(0, "=== FASE 2: no hubo solución. Se explora sin cotas para el objetivo secundario ===");
            explorar();
        }
    }

    private void explorar() {
        visitado[origen.getId()] = true;
        recargasPendientes = 0;
        for (Estacion e : mapa.getEstaciones())
            if (e != origen && !e.isProhibida()) recargasPendientes += e.recarga();
        nodosAnalizados++;   // raíz
        escribir(0, String.format("[#%d] RAÍZ: %s | t=0.0s | bat=%.2f | obligatorias 0/%d",
                nodosAnalizados, origen.getCodigo(), BATERIA_INICIAL, totalObligatorias));
        backtrack(origen, 0.0, BATERIA_INICIAL, 1);
        visitado[origen.getId()] = false;
    }

    private void backtrack(Estacion actual, double tiempo, double bateria, int nivel) {
        nodosExpandidos++;
        actualizarMejorParcial(actual);

        for (Conexion c : mapa.adyacentes(actual)) {
            Estacion sig = c.otroExtremo(actual);

            // --- podas que no dependen del modo ---
            if (sig.isProhibida()) { podar(nivel, actual, sig, null, "zona prohibida", ""); continue; }
            boolean esCierre = (sig == origen);
            if (esCierre && obligatoriasVisitadas < totalObligatorias) {
                podar(nivel, actual, sig, null, "vuelta anticipada",
                        "faltan " + (totalObligatorias - obligatoriasVisitadas) + " obligatorias");
                continue;
            }
            if (!esCierre && visitado[sig.getId()]) { podar(nivel, actual, sig, null, "ya visitada", ""); continue; }

            // --- se prueba cada modo permitido en la conexión ---
            for (Modo modo : c.getModos()) {
                nodosAnalizados++;
                double t2 = tiempo + modo.tiempo(c.getSegundos());
                double b2 = bateria - modo.consumo(c.getSegundos());

                // Batería: debe quedar > 0, salvo al llegar al destino final (>= 0)
                if (esCierre ? b2 < 0 : b2 <= 0) {
                    podar(nivel, actual, sig, modo, "batería", String.format("quedaría %.2f", b2));
                    continue;
                }
                if (sig.cerradaEn(t2)) {
                    podar(nivel, actual, sig, modo, "ventana temporal",
                            String.format("llega en t=%.1fs y está cerrada en %s", t2, cierresTexto(sig)));
                    continue;
                }
                double recarga = esCierre ? 0 : Math.min(sig.recarga(), BATERIA_MAXIMA - b2);
                double b3 = b2 + recarga;

                boolean cotas = usarCota && !faseSecundaria;
                if (cotas && !esCierre) {
                    // lo mínimo que falta: entrar a cada obligatorio pendiente y, al final, a A348
                    double faltaT = minEntradaT[origen.getId()];
                    double faltaB = minEntradaB[origen.getId()];
                    for (Estacion o : mapa.getEstaciones()) {
                        if (o.isObligatoria() && !visitado[o.getId()] && o != sig) {
                            faltaT += minEntradaT[o.getId()];
                            faltaB += minEntradaB[o.getId()];
                        }
                    }
                    if (Double.isInfinite(faltaT)) {
                        podar(nivel, actual, sig, modo, "sin camino",
                                "a un lugar que falta (obligatorio pendiente o " + origen + ") no entra ninguna conexión");
                        continue;
                    }
                    double bateriaPosible = b3 + recargasPendientes - sig.recarga();
                    if (bateriaPosible < faltaB) {
                        podar(nivel, actual, sig, modo, "batería insuficiente",
                                String.format("necesita >= %.2f y como mucho tendría %.2f", faltaB, bateriaPosible));
                        continue;
                    }
                    if (mejores.size() == TOP && t2 + faltaT >= mejores.get(TOP - 1).tiempoTotal) {
                        podar(nivel, actual, sig, modo, "cota", String.format("cota %.1fs >= 3ra mejor %.1fs",
                                t2 + faltaT, mejores.get(TOP - 1).tiempoTotal));
                        continue;
                    }
                } else if (cotas && mejores.size() == TOP && t2 >= mejores.get(TOP - 1).tiempoTotal) {
                    podar(nivel, actual, sig, modo, "cota", String.format("%.1fs >= 3ra mejor %.1fs",
                            t2, mejores.get(TOP - 1).tiempoTotal));
                    continue;
                }

                // --- hacer ---
                camino.addLast(new Movimiento(actual, sig, modo, b3, recarga, t2));

                if (esCierre) {
                    solucionesEncontradas++;
                    int pos = registrarSolucion(new Ruta(new ArrayList<>(camino)));
                    escribir(nivel, String.format("[#%d] %s -> %s (%s) t=%.1fs bat=%.2f  ★ SOLUCIÓN %s",
                            nodosAnalizados, actual, sig, modo.getNombre(), t2, b3,
                            pos > 0 ? "-> entra al top-3 en posición " + pos : "(no mejora el top-3)"));
                } else {
                    visitado[sig.getId()] = true;
                    recargasPendientes -= sig.recarga();
                    if (sig.isObligatoria()) obligatoriasVisitadas++;
                    escribir(nivel, String.format("[#%d] %s -> %s (%s) t=%.1fs bat=%.2f%s | oblig %d/%d",
                            nodosAnalizados, actual, sig, modo.getNombre(), t2, b3,
                            recarga > 0 ? String.format(" (+%.1f recarga)", recarga) : "",
                            obligatoriasVisitadas, totalObligatorias));

                    backtrack(sig, t2, b3, nivel + 1);

                    // --- deshacer ---
                    if (sig.isObligatoria()) obligatoriasVisitadas--;
                    recargasPendientes += sig.recarga();
                    visitado[sig.getId()] = false;
                }
                camino.removeLast();
            }
        }
    }

    /**
     * Inserta la ruta en el top-3 ordenado por tiempo total. Devuelve la posición 1..3, o 0 si no entra.
     * Las 3 rutas tienen que pasar por lugares distintos: si ya hay una ruta con la misma secuencia de
     * lugares (solo cambia el modo de algún tramo), se queda la más rápida de las dos.
     */
    private int registrarSolucion(Ruta r) {
        String sec = r.secuencia();
        for (int i = 0; i < mejores.size(); i++) {
            if (mejores.get(i).secuencia().equals(sec)) {
                if (r.tiempoTotal >= mejores.get(i).tiempoTotal) return 0;  // misma secuencia, no mejora
                mejores.remove(i);                                          // misma secuencia, más rápida
                break;
            }
        }
        int pos = 0;
        while (pos < mejores.size() && mejores.get(pos).tiempoTotal <= r.tiempoTotal) pos++;
        if (pos >= TOP) return 0;
        mejores.add(pos, r);
        if (mejores.size() > TOP) mejores.remove(TOP);
        return pos + 1;
    }

    /**
     * Objetivo secundario: la ruta truncada "más avanzada".
     * Criterio: 1) más lugares obligatorios visitados, 2) menor distancia recta al destino (A348).
     */
    private void actualizarMejorParcial(Estacion actual) {
        if (camino.isEmpty()) return;
        // Distancia recta con coordenadas; si el mapa no las trae, solo cuenta la cantidad de obligatorios.
        double d = mapa.isUsarCoordenadas() ? actual.distanciaRecta(origen) : 0;
        if (obligatoriasVisitadas > mejorParcialObligatorias
                || (obligatoriasVisitadas == mejorParcialObligatorias && d < mejorParcialDistancia)) {
            mejorParcialObligatorias = obligatoriasVisitadas;
            mejorParcialDistancia = d;
            mejorParcial = new Ruta(new ArrayList<>(camino));
        }
    }

    // =====================================================================
    //  COTAS: para cada lugar, la conexión de entrada más barata (sin zonas prohibidas).
    //  Es optimista (supone el mejor modo y que cada lugar se alcanza por su entrada más
    //  barata), por eso nunca sobreestima lo que falta y la poda no descarta soluciones.
    // =====================================================================

    private void calcularEntradasMinimas() {
        Arrays.fill(minEntradaT, Double.POSITIVE_INFINITY);
        Arrays.fill(minEntradaB, Double.POSITIVE_INFINITY);
        for (Estacion e : mapa.getEstaciones()) {
            if (e.isProhibida()) continue;
            for (Conexion c : mapa.adyacentes(e)) {          // conexiones que salen de e
                Estacion destino = c.otroExtremo(e);
                if (destino.isProhibida()) continue;
                for (Modo m : c.getModos()) {
                    int d = destino.getId();
                    minEntradaT[d] = Math.min(minEntradaT[d], m.tiempo(c.getSegundos()));
                    minEntradaB[d] = Math.min(minEntradaB[d], m.consumo(c.getSegundos()));
                }
            }
        }
    }

    // =====================================================================
    //  LOG
    // =====================================================================

    private void podar(int nivel, Estacion a, Estacion b, Modo m, String motivo, String detalle) {
        podas.merge(motivo, 1L, Long::sum);
        if (m == null) nodosAnalizados++;   // la poda sin modo también es un nodo analizado
        escribir(nivel, String.format("[#%d] %s -> %s%s  ✘ PODA: %s%s", nodosAnalizados, a, b,
                m == null ? "" : " (" + m.getNombre() + ")", motivo,
                detalle.isEmpty() ? "" : " (" + detalle + ")"));
    }

    private static String cierresTexto(Estacion e) {
        StringBuilder sb = new StringBuilder();
        for (Ventana v : e.getCierres()) sb.append(sb.length() > 0 ? " y " : "").append(v);
        return sb.toString();
    }

    private void escribir(int nivel, String texto) {
        if (log == null) return;
        lineasLog++;
        if (lineasLog <= maxLineasLog) log.println("  ".repeat(nivel) + texto);
        else if (lineasLog == maxLineasLog + 1)
            log.println("... (log truncado: se alcanzó el máximo de " + maxLineasLog + " líneas) ...");
    }

    // =====================================================================
    //  RESULTADOS
    // =====================================================================

    public String reporte() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append(" ESTADÍSTICAS DE LA EXPLORACIÓN\n");
        sb.append("============================================================\n");
        sb.append(String.format(" Nodos analizados (generados) : %d%n", nodosAnalizados));
        if (nodosFase1 >= 0)
            sb.append(String.format("    (fase 1 con podas: %d | fase 2 objetivo secundario: %d)%n",
                    nodosFase1, nodosAnalizados - nodosFase1));
        sb.append(String.format(" Nodos expandidos (recursión) : %d%n", nodosExpandidos));
        sb.append(String.format(" Soluciones completas halladas: %d%n", solucionesEncontradas));
        sb.append(" Podas por motivo:\n");
        podas.forEach((k, v) -> sb.append(String.format("    - %-18s: %d%n", k, v)));
        sb.append("\n");
        if (!mejores.isEmpty()) {
            sb.append("============================================================\n");
            sb.append(" OBJETIVO PRINCIPAL: " + mejores.size() + " MEJORES RUTAS\n");
            sb.append("============================================================\n");
            for (int i = 0; i < mejores.size(); i++) {
                Ruta r = mejores.get(i);
                sb.append(String.format("%nRuta #%d  -  %s%n", i + 1, r.secuencia()));
                sb.append("  Modos: " + r.secuenciaConModos() + "\n");
                sb.append(r.detalle());
            }
        } else {
            sb.append("============================================================\n");
            sb.append(" NO EXISTE SOLUCIÓN QUE CUMPLA TODAS LAS RESTRICCIONES\n");
            sb.append(" OBJETIVO SECUNDARIO: ruta truncada más cercana al objetivo\n");
            sb.append("============================================================\n");
            if (mejorParcial.pasos.isEmpty()) {
                sb.append(" Uada no puede realizar ningún movimiento válido desde " + origen.getCodigo() + ".\n");
                return sb.toString();
            }
            sb.append(String.format(" Obligatorios visitados: %d/%d%n", mejorParcialObligatorias, totalObligatorias));
            Estacion u = mejorParcial.ultima();
            sb.append(String.format(" Punto máximo de avance: %s (%s)%n", u, u == null ? "-" : u.getNombre()));
            if (mapa.isUsarCoordenadas())
                sb.append(String.format(" Distancia recta a %s: %.1f m%n", origen.getCodigo(), mejorParcialDistancia));
            sb.append(" Ruta truncada: " + mejorParcial.secuencia() + "\n");
            sb.append(mejorParcial.detalle());
        }
        return sb.toString();
    }

    public List<Ruta> getMejores() { return mejores; }
    public Ruta getMejorParcial() { return mejorParcial; }
    public int getMejorParcialObligatorias() { return mejorParcialObligatorias; }
    public double getMejorParcialDistancia() { return mejorParcialDistancia; }
    public Map<String, Long> getPodas() { return Collections.unmodifiableMap(podas); }
    public long getNodosAnalizados() { return nodosAnalizados; }
    public long getNodosExpandidos() { return nodosExpandidos; }
}
