import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Aplica los algoritmos 4.1 y 4.3 del paper de Ventre a un *_logic.json (BFS del grafo de alcanzabilidad).
 * Uso: java docs/reference/ThreadCalc.java <logic.json> <plazas idle, separadas por coma> <plazas recurso, separadas por coma>
 * Segmentos = agentes del JSON. Plaza de accion = no idle ni recurso; se asigna al segmento que la produce
 * (si la producen segmentos distintos, al que la consume; si es join y fork a la vez, a JOIN/FORK).
 * Es una referencia de solo lectura: no forma parte del build de Gradle.
 */
public class ThreadCalc {
    static int[][] matrix(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        int start = json.indexOf('[', i);
        int depth = 0, end = start;
        for (int k = start; k < json.length(); k++) {
            char c = json.charAt(k);
            if (c == '[') depth++;
            if (c == ']') { depth--; if (depth == 0) { end = k; break; } }
        }
        String body = json.substring(start, end + 1);
        List<int[]> rows = new ArrayList<>();
        Matcher m = Pattern.compile("\\[([^\\[\\]]*)\\]").matcher(body.substring(1));
        while (m.find()) {
            String[] p = m.group(1).trim().split("\\s*,\\s*");
            int[] r = new int[p.length];
            for (int k = 0; k < p.length; k++) r[k] = Integer.parseInt(p[k]);
            rows.add(r);
        }
        return rows.toArray(new int[0][]);
    }

    public static void main(String[] a) throws Exception {
        String json = Files.readString(Path.of(a[0]));
        Set<Integer> excluded = new HashSet<>();
        for (String s : (a[1] + "," + a[2]).split(",")) if (!s.isBlank()) excluded.add(Integer.parseInt(s.trim()));
        int[][] pre = matrix(json, "pre"), post = matrix(json, "post");
        Matcher mm = Pattern.compile("\"initialMarking\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        mm.find();
        String[] mp = mm.group(1).trim().split("\\s*,\\s*");
        int[] m0 = new int[mp.length];
        for (int k = 0; k < mp.length; k++) m0[k] = Integer.parseInt(mp[k]);
        int P = pre.length, T = pre[0].length;

        // segmentos = agentes (namePrefix -> secuencia)
        Map<String, int[]> seg = new LinkedHashMap<>();
        Map<String, Integer> cfgCount = new LinkedHashMap<>();
        Matcher am = Pattern.compile("\"namePrefix\"\\s*:\\s*\"(\\w+)\",\\s*\"count\"\\s*:\\s*(\\d+),\\s*\"sequence\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        while (am.find()) {
            String[] p = am.group(3).trim().split("\\s*,\\s*");
            int[] s = new int[p.length];
            for (int k = 0; k < p.length; k++) s[k] = Integer.parseInt(p[k]);
            seg.put(am.group(1), s);
            cfgCount.put(am.group(1), Integer.parseInt(am.group(2)));
        }
        Map<Integer, String> segOfT = new HashMap<>();
        seg.forEach((n, s) -> { for (int t : s) segOfT.put(t, n); });

        // asignacion de plazas de accion a segmentos
        Map<String, List<Integer>> places = new LinkedHashMap<>();
        seg.keySet().forEach(n -> places.put(n, new ArrayList<>()));
        places.put("JOIN/FORK", new ArrayList<>());
        for (int p = 0; p < P; p++) {
            if (excluded.contains(p)) continue;
            Set<String> prod = new LinkedHashSet<>(), cons = new LinkedHashSet<>();
            for (int t = 0; t < T; t++) {
                if (post[p][t] > 0 && segOfT.containsKey(t)) prod.add(segOfT.get(t));
                if (pre[p][t] > 0 && segOfT.containsKey(t)) cons.add(segOfT.get(t));
            }
            if (prod.size() == 1) places.get(prod.iterator().next()).add(p);
            else if (cons.size() == 1) places.get(cons.iterator().next()).add(p);
            else places.get("JOIN/FORK").add(p);
        }

        // alcanzabilidad
        Set<List<Integer>> seen = new HashSet<>();
        Deque<int[]> q = new ArrayDeque<>();
        q.add(m0);
        Map<String, Integer> max = new LinkedHashMap<>();
        places.keySet().forEach(n -> max.put(n, 0));
        int maxGlobal = 0;
        Set<Integer> all = new TreeSet<>();
        places.values().forEach(all::addAll);
        while (!q.isEmpty()) {
            int[] m = q.poll();
            List<Integer> key = new ArrayList<>();
            for (int v : m) key.add(v);
            if (!seen.add(key)) continue;
            for (var e : places.entrySet()) {
                int s = 0;
                for (int p : e.getValue()) s += m[p];
                max.merge(e.getKey(), s, Math::max);
            }
            int g = 0;
            for (int p : all) g += m[p];
            maxGlobal = Math.max(maxGlobal, g);
            for (int t = 0; t < T; t++) {
                boolean en = true;
                for (int p = 0; p < P; p++) if (m[p] < pre[p][t]) { en = false; break; }
                if (!en) continue;
                int[] n = m.clone();
                for (int p = 0; p < P; p++) n[p] += post[p][t] - pre[p][t];
                q.add(n);
            }
        }
        System.out.println("Estados alcanzables: " + seen.size());
        System.out.println("Plazas de accion: " + all);
        System.out.println("4.1 max hilos activos simultaneos: " + maxGlobal);
        int total = 0;
        for (var e : places.entrySet()) {
            int cnt = cfgCount.getOrDefault(e.getKey(), -1);
            System.out.printf("  %-10s transiciones=%s plazas=%s  max=%d  (JSON count=%s)%n", e.getKey(),
                    seg.containsKey(e.getKey()) ? Arrays.toString(seg.get(e.getKey())) : "-", e.getValue(), max.get(e.getKey()), cnt < 0 ? "-" : cnt);
            total += max.get(e.getKey());
        }
        System.out.println("4.3 total hilos necesarios: " + total);
    }
}