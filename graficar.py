"""Grafica complejidad_practica.csv (salida de uada.Benchmark)."""
import csv
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

n, msc, nodc, mss, nods = [], [], [], [], []
with open("complejidad_practica.csv", encoding="utf-8") as f:
    for r in csv.DictReader(f):
        n.append(int(r["n"])); msc.append(float(r["ms_con_cota"])); nodc.append(int(r["nodos_con_cota"]))
        mss.append(float(r["ms_sin_cota"]) if r["ms_sin_cota"] else None)
        nods.append(int(r["nodos_sin_cota"]) if r["nodos_sin_cota"] else None)

AZUL, NARANJA, TINTA, GRIS = "#2a78d6", "#eb6834", "#2b2b2a", "#8a8a85"
fig, axes = plt.subplots(1, 2, figsize=(12, 4.8))
for ax, (yc, ys, titulo, ylab) in zip(axes, [
        (msc, mss, "Tiempo de ejecución", "milisegundos (escala log)"),
        (nodc, nods, "Nodos analizados del árbol", "nodos (escala log)")]):
    ns = [a for a, b in zip(n, ys) if b is not None]
    ax.plot(n, yc, "-o", color=AZUL, lw=2, ms=6, label="Backtracking con poda (cota + batería)")
    ax.plot(ns, [b for b in ys if b is not None], "-s", color=NARANJA, lw=2, ms=6,
            label="Backtracking sin cota")
    ax.set_yscale("log")
    ax.set_title(titulo, color=TINTA, fontsize=12, loc="left")
    ax.set_xlabel("n = cantidad de lugares del mapa", color=TINTA)
    ax.set_ylabel(ylab, color=TINTA)
    ax.grid(True, which="major", color="#e4e3df", lw=0.8)
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    for s in ("left", "bottom"):
        ax.spines[s].set_color(GRIS)
    ax.tick_params(colors=TINTA)
    ax.set_xticks(n)
axes[0].legend(frameon=False, fontsize=9, loc="upper left")
fig.suptitle("Complejidad práctica - promedio de 5 mapas aleatorios por tamaño", color=TINTA,
             fontsize=13, x=0.01, ha="left")
fig.tight_layout()
fig.savefig("grafico_complejidad.png", dpi=160)
print("ok")
