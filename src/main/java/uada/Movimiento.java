package uada;

/** Un paso de la ruta: origen -> destino con el modo usado y el estado luego del paso. */
public class Movimiento {
    final Estacion origen, destino;
    final Modo modo;
    final double bateriaRestante;   // luego de moverse y (si corresponde) recargar
    final double recarga;           // batería recargada al llegar
    final double espera;            // segundos esperando a que abra el destino (0 si estaba abierto)
    final double tiempoAcumulado;   // al entrar al destino (incluye la espera)

    public Movimiento(Estacion origen, Estacion destino, Modo modo,
                      double bateriaRestante, double recarga, double espera, double tiempoAcumulado) {
        this.origen = origen;
        this.destino = destino;
        this.modo = modo;
        this.bateriaRestante = bateriaRestante;
        this.recarga = recarga;
        this.espera = espera;
        this.tiempoAcumulado = tiempoAcumulado;
    }
}
