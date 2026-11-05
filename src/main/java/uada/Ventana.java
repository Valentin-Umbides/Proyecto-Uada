package uada;

/**
 * Intervalo [desde, hasta] (en segundos) durante el cual una estación está cerrada.
 * hasta = Double.POSITIVE_INFINITY significa que no vuelve a abrir (no sirve esperar).
 */
public class Ventana {
    private final double desde;
    private final double hasta;

    public Ventana(double desde, double hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public double getHasta() { return hasta; }

    public boolean contiene(double t) {
        return t >= desde && t <= hasta;
    }

    @Override
    public String toString() {
        return "[" + (long) desde + "s, " + (Double.isInfinite(hasta) ? "sin reapertura" : (long) hasta + "s") + "]";
    }
}
