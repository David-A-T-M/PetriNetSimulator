import numpy as np

def analyze_petri_net(m0, pre, post, action_mask):
    """
    Genera el espacio de estados y calcula el máximo de hilos activos (Algoritmo 4.1).
    """
    incidence = post - pre
    visited_states = []
    queue = [m0.tolist()]

    max_active_threads = 0

    print("Iniciando análisis de alcanzabilidad...")

    while queue:
        current_m = queue.pop(0)

        if current_m in visited_states:
            continue

        visited_states.append(current_m)

        # Algoritmo 4.1: Suma de marcas en plazas de acción (Ecuación 5)
        current_active = sum(np.array(current_m) * action_mask)
        if current_active > max_active_threads:
            max_active_threads = current_active

        # Intentar disparar cada transición (Ecuación de estado)
        for t in range(incidence.shape[1]):
            # Una transición está sensibilizada si el marcado >= pesos de arcos de entrada
            if all(np.array(current_m) >= pre[:, t]):
                # M_j+1 = M_j + I * sigma (Ecuación 3 del PDF)
                next_m = (np.array(current_m) + incidence[:, t]).tolist()

                if next_m not in visited_states:
                    queue.append(next_m)

    return visited_states, max_active_threads

# --- CONFIGURACIÓN DE TU RED (9 PLAZAS) ---
# Orden: [P1, P2, P3, P4, P5, P6, P7, P8, P9]
m0 = np.array([2, 0, 0, 2, 0, 0, 1, 1, 0])

# Matriz Pre (Arcos de entrada a transiciones T0..T5)
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
])

# Matriz Post (Arcos de salida de transiciones T0..T5)
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
])

# Máscara de Plazas de Acción (PA): P2, P3, P5, P6 (Ecuación 5)
# Excluye P1, P4 (Idle), P7 (Huecos), P8 (Mutex), P9 (Productos)
mask = np.array([0, 1, 1, 0, 1, 1, 0, 0, 0])

# Ejecución
estados, max_hilos = analyze_petri_net(m0, pre, post, mask)

print(f"\nTotal de estados encontrados: {len(estados)}")
print(f"Cantidad máxima de hilos activos simultáneos (Algoritmo 4.1): {max_hilos}")