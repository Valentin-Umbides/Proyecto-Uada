package uada;

import java.util.ArrayList;
import java.util.List;

/** Secuencia de movimientos (solución completa o ruta truncada). */
public class Ruta {
    final List<Movimiento> pasos;
    final double tiempoTotal;

    public Ruta(List<Movimiento> pasos) {
        this.pasos = new ArrayList<>(pasos);                 // copia: O(n)
        this.tiempoTotal = pasos.isEmpty() ? 0 : pasos.get(pasos.size() - 1).tiempoAcumulado;
    }

    /** Secuencia de estaciones, ej: "A348 > PASILLO3 > ... > A348". */
    public String secuencia() {
        if (pasos.isEmpty()) return "(sin movimientos)";
        StringBuilder sb = new StringBuilder(pasos.get(0).origen.getCodigo());
        for (Movimiento m : pasos) sb.append(" > ").append(m.destino.getCodigo());
        return sb.toString();
    }

    /** Secuencia con el modo de cada tramo, ej: "A348 -S-> PASILLO3 -C-> A201 ...". */
    public String secuenciaConModos() {
        if (pasos.isEmpty()) return "(sin movimientos)";
        StringBuilder sb = new StringBuilder(pasos.get(0).origen.getCodigo());
        for (Movimiento m : pasos)
            sb.append(" -").append(m.modo.name().charAt(0)).append("-> ").append(m.destino.getCodigo());
        return sb.toString();
    }

    /** Batería con la que termina la ruta (100 si no tiene movimientos). */
    public double bateriaFinal() {
        return pasos.isEmpty() ? MisionUada.BATERIA_INICIAL : pasos.get(pasos.size() - 1).bateriaRestante;
    }

    public Estacion ultima() {
        return pasos.isEmpty() ? null : pasos.get(pasos.size() - 1).destino;
    }

    public String detalle() {
        StringBuilder sb = new StringBuilder();
        int w = 9;
        for (Movimiento m : pasos) w = Math.max(w, Math.max(m.origen.getCodigo().length(), m.destino.getCodigo().length()));
        String f1 = "  %-3s %-" + w + "s %-" + w + "s %-18s %10s %9s %9s %12s%n";
        String f2 = "  %-3d %-" + w + "s %-" + w + "s %-18s %10.2f %9s %9s %10.1f s%n";
        sb.append(String.format(f1,
                "#", "Origen", "Destino", "Movimiento", "Batería", "Recarga", "Espera", "T. acumulado"));
        int i = 1;
        for (Movimiento m : pasos) {
            sb.append(String.format(f2,
                    i++, m.origen.getCodigo(), m.destino.getCodigo(), m.modo.getNombre(),
                    m.bateriaRestante, m.recarga > 0 ? String.format("+%.1f", m.recarga) : "-",
                    m.espera > 0 ? String.format("%.1f s", m.espera) : "-",
                    m.tiempoAcumulado));
        }
        sb.append(String.format("  Tiempo total: %.1f s (%s)%n", tiempoTotal, formatear(tiempoTotal)));
        return sb.toString();
    }

    static String formatear(double seg) {
        long s = Math.round(seg);
        return String.format("%d min %02d s", s / 60, s % 60);
    }
}
