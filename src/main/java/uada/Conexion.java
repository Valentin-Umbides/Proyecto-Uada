package uada;

import java.util.EnumSet;
import java.util.Set;

/** Arista no dirigida entre dos estaciones, con su tiempo (s) y modos permitidos. */
public class Conexion {
    private final Estacion a, b;
    private final int segundos;
    private final Set<Modo> modos;

    public Conexion(Estacion a, Estacion b, int segundos, Set<Modo> modos) {
        this.a = a;
        this.b = b;
        this.segundos = segundos;
        this.modos = EnumSet.copyOf(modos);
    }

    public Estacion otroExtremo(Estacion e) { return e == a ? b : a; }
    public int getSegundos() { return segundos; }
    public Set<Modo> getModos() { return modos; }
    public Estacion getA() { return a; }
    public Estacion getB() { return b; }
}
