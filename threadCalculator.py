import numpy as np
import csv

def analyze_petri_net_full(m0, pre, post, global_mask, segments):
    """
    Analiza el espacio de estados, genera un CSV y calcula hilos por segmento.
    """
    incidence = post - pre
    visited_states = []
    queue = [m0.tolist()]

    max_global_active = 0
    # Diccionario para guardar el máximo de cada segmento
    max_per_segment = {name: 0 for name in segments.keys()}

    print("Generando espacio de estados...")

    while queue:
        current_m = queue.pop(0)
        if current_m in visited_states:
            continue

        visited_states.append(current_m)
        curr_np = np.array(current_m)

        # 1. Algoritmo 4.1: Máximo Global
        global_active = sum(curr_np * global_mask)
        max_global_active = max(max_global_active, global_active)

        # 2. Algoritmo 4.3: Máximo por Segmento
        for name, mask in segments.items():
            segment_active = sum(curr_np * mask)
            max_per_segment[name] = max(max_per_segment[name], segment_active)

        # 3. Disparar transiciones para encontrar nuevos estados
        for t in range(incidence.shape[1]):
            if all(curr_np >= pre[:, t]):
                next_m = (curr_np + incidence[:, t]).tolist()
                if next_m not in visited_states:
                    queue.append(next_m)

    # Guardar en CSV para validación visual
    with open('marcados_sistema.csv', 'w', newline='') as f:
        writer = csv.writer(f)
        header = [f"P{i}" for i in range(len(m0))] + ["SUMA_GLOBAL"]
        writer.writerow(header)
        for state in visited_states:
            writer.writerow(state + [sum(np.array(state) * global_mask)])

    return visited_states, max_global_active, max_per_segment

# --- CONFIGURACIÓN DE TU RED (Zero-Based) ---
# Orden: [P0, P1, P2, P3, P4, P5, P6, P7, P8]
# Según tus matrices: P0..P2 (Prod), P3..P5 (Cons), P6 (Huecos), P7 (Mutex), P8 (Items)
m0 = np.array([3, 0, 0, 3, 0, 0, 20, 1, 0])
# m0 = np.array([3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0])

# Matrices Pre y Post según tu última corrección
pre = np.array([
    [1, 0, 0, 0, 0, 0], # P1
    [0, 1, 0, 0, 0, 0], # P2
    [0, 0, 1, 0, 0, 0], # P3
    [0, 0, 0, 1, 0, 0], # P4
    [0, 0, 0, 0, 1, 0], # P5
    [0, 0, 0, 0, 0, 1], # P6
    [1, 0, 0, 0, 0, 0], # P7
    [1, 0, 0, 1, 0, 0], # P8
    [0, 0, 0, 1, 0, 0]  # P9
    # [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0],
    # [0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]
])

post = np.array([
    [0, 0, 1, 0, 0, 0], # P1
    [1, 0, 0, 0, 0, 0], # P2
    [0, 1, 0, 0, 0, 0], # P3
    [0, 0, 0, 0, 0, 1], # P4
    [0, 0, 0, 1, 0, 0], # P5
    [0, 0, 0, 0, 1, 0], # P6
    [0, 0, 0, 0, 1, 0], # P7
    [0, 1, 0, 0, 1, 0], # P8
    [0, 1, 0, 0, 0, 0]  # P9
    # [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    # [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
    # [0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0],
    # [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0],
    # [0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0],
    # [0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0]
])

# Máscara Global (Plazas de acción: P1, P2, P4, P5)
global_mask = np.array([0, 1, 1, 0, 1, 1, 0, 0, 0])
# global_mask = np.array([0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0])

# --- DEFINICIÓN DE SEGMENTOS (Algoritmo 4.2 y 4.3) ---
# Cada máscara activa solo las plazas de acción de SU segmento
segments = {
    "Prod_A (T0)":     np.array([0, 1, 0, 0, 0, 0, 0, 0, 0]), # P1
    "Prod_B (T1, T2)": np.array([0, 0, 1, 0, 0, 0, 0, 0, 0]), # P2
    "Cons_A (T3)":     np.array([0, 0, 0, 0, 1, 0, 0, 0, 0]), # P4
    "Cons_B (T4, T5)": np.array([0, 0, 0, 0, 0, 1, 0, 0, 0])  # P5
+
    
    # DATA PROC 3 seg
    #"M":     np.array([1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1]),#[0, 1, 0, 0, 0, 0, 0, 0, 0]),
    #"L":     np.array([1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1]),#[0, 1, 0, 0, 0, 0, 0, 0, 0]),
    #"H":     np.array([1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1])#[0, 1, 0, 0, 0, 0, 0, 0, 0])

    #DATA PROC 5 seg
    # "S":     np.array([1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]),
    # "E":     np.array([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]),
    # "M":     np.array([0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0]),
    # "L":     np.array([0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0]),
    # "H":     np.array([0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0])

}

estados, m_global, m_segmentos = analyze_petri_net_full(m0, pre, post, global_mask, segments)

print(f"Análisis completado. Estados totales: {len(estados)}")
print(f"Hilos Máximos Activos Simultáneos (4.1): {m_global}")
print("-" * 30)
print("Hilos por Segmento (4.3):")
total_necesarios = 0
for name, val in m_segmentos.items():
    print(f" - {name}: {val} hilos")
    total_necesarios += val
print("-" * 30)
print(f"Total de hilos necesarios para el sistema: {total_necesarios}")
print("Archivo 'marcados_sistema.csv' generado correctamente.")