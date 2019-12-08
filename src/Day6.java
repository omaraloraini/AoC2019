import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Day6 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day6.Part1.answer());
        System.out.println(Day6.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input6.txt");

    static Map<String, Set<String>> readTree() throws IOException {
        List<String> input = Files.readAllLines(INPUT_PATH);
        return input
                .stream()
                .map(s -> s.split("\\)"))
                .collect(Collectors.groupingBy(pair -> pair[0],
                        Collectors.mapping(pair -> pair[1],
                                Collectors.toSet())));
    }

    static class Part1 {
        static long answer() throws IOException {
            return lengthOfAllPaths(readTree(), "COM", 0);
        }

        private static long lengthOfAllPaths(Map<String, Set<String>> tree, String node, int stepsSoFar) {
            return stepsSoFar + tree.getOrDefault(node, Collections.emptySet())
                    .stream()
                    .mapToLong(n -> lengthOfAllPaths(tree, n, stepsSoFar + 1))
                    .sum();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            Map<String, Set<String>> tree = readTree();
            Map<String, String> inverted = invert(tree);
            return lengthToMostSpecificAncestor(inverted, "YOU", "SAN");
        }


        static <T> Map<T, T> invert(Map<T, Set<T>> tree) {
            HashMap<T, T> inverted = new HashMap<>();
            for (Map.Entry<T, Set<T>> entry : tree.entrySet()) {
                for (T key : entry.getValue()) {
                    inverted.putIfAbsent(key, entry.getKey());
                }
            }
            return inverted;
        }

        static int lengthToMostSpecificAncestor(Map<String, String> tree, String v1, String v2) {
            var v1Path = new ArrayList<String>();
            var v2Path = new ArrayList<String>();

            v1Path.add(tree.get(v1));
            v2Path.add(tree.get(v2));

            String v1Last = v1Path.get(v1Path.size() - 1);
            String v2Last = v2Path.get(v2Path.size() - 1);
            while (!v1Path.contains(v2Last) && !v2Path.contains(v1Last)) {
                v1Path.add(tree.get(v1Last));
                v2Path.add(tree.get(v2Last));

                v2Last = v2Path.get(v2Path.size() - 1);
                v1Last = v1Path.get(v1Path.size() - 1);
            }

            if (v1Path.contains(v2Last)) {
                return v1Path.indexOf(v2Last) + v2Path.size() - 1;
            } else {
                return v2Path.indexOf(v1Last) + v1Path.size() - 1;
            }
        }
    }
}
