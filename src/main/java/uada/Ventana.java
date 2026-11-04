package uada;

/** Intervalo [desde, hasta] (en segundos) durante el cual una estación está cerrada. */
public class Ventana {
    private final double desde;
    private final double hasta;

    public Ventana(double desde, double hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public boolean contiene(double t) {
        return t >= desde && t <= hasta;
    }

    @Override
    public String toString() {
        return "[" + (long) desde + "s, " + (long) hasta + "s]";
    }
}
