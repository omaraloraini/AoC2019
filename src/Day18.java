import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class Day18 {
    static final Path INPUT_PATH = Path.of(".", "input18.txt");

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static class Part1 {
        static long answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            Map<Character, Set<Pair<Character, Integer>>> graph = computeGraph(parseGrid(input));
            return shortestPath(graph, List.of('@'));
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            char[][] grid = parseGrid(input);

            int height = grid.length;
            int width = grid[0].length;

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (grid[i][j] == '@') {
                        grid[i - 1][j - 1] = '1';
                        grid[i - 1][j] = '#';
                        grid[i - 1][j + 1] = '2';

                        grid[i][j - 1] = '#';
                        grid[i][j] = '#';
                        grid[i][j + 1] = '#';

                        grid[i + 1][j - 1] = '3';
                        grid[i + 1][j] = '#';
                        grid[i + 1][j + 1] = '4';
                    }
                }
            }

            Map<Character, Set<Pair<Character, Integer>>> graph = computeGraph(grid);
            return shortestPath(graph, List.of('1', '2', '3', '4'));
        }
    }

    static int shortestPath(Map<Character, Set<Pair<Character, Integer>>> graph, List<Character> robots) {
        int keyCount = (int) graph.keySet().stream().filter(Character::isLowerCase).count();

        PriorityQueue<Tuple<Set<Character>, List<Character>, Integer>> queue = new PriorityQueue<>(Comparator.comparingInt(Tuple::third));
        HashSet<Pair<Set<Character>, Set<Character>>> visited = new HashSet<>();
        queue.add(Tuple.of(Collections.emptySet(), robots, 0));

        for (;;) {
            Tuple<Set<Character>, List<Character>, Integer> tuple = queue.poll();

            if (tuple == null) throw new Error("Could not collect all keys");

            Set<Character> keysSeen = tuple.first();
            List<Character> robotPositions = tuple.second();
            Pair<Set<Character>, Set<Character>> state = Pair.of(keysSeen, new HashSet<>(robotPositions));

            if (visited.contains(state)) continue;

            if (keysSeen.size() == keyCount) {
                return tuple.third();
            }

            robotPositions.stream().distinct().forEach(position -> {
                for (Pair<Character, Integer> pair : keyClosure(graph, keysSeen, position)) {
                    char c = pair.first();
                    HashSet<Character> set = new LinkedHashSet<>(keysSeen);
                    set.add(c);

                    ArrayList<Character> newPositions = new ArrayList<>(robotPositions);
                    newPositions.set(robotPositions.indexOf(position), c);

                    int distance = pair.second() + tuple.third();
                    queue.add(Tuple.of(set, newPositions, distance));
                }
            });

            visited.add(state);
        }
    }

    static Set<Pair<Character, Integer>> keyClosure(Map<Character, Set<Pair<Character, Integer>>> map,
                                                    Set<Character> keys,
                                                    char key) {

        Queue<Pair<Character, Integer>> queue = new LinkedList<>(map.get(key));
        HashSet<Pair<Character, Integer>> closure = new HashSet<>();
        HashSet<Character> visited = new HashSet<>();
        visited.add(key);

        while (!queue.isEmpty()) {
            Pair<Character, Integer> pair = queue.poll();
            char c = pair.first();
            int l = pair.second();

            if (visited.contains(c)) continue;

            visited.add(c);

            if (c == '@' || Character.isDigit(c) || keys.contains(c) || Character.isUpperCase(c) && keys.contains(Character.toLowerCase(c))) {
                for (Pair<Character, Integer> p : map.get(c)) {
                    queue.add(Pair.of(p.first(), p.second() + l));
                }
            }
            else if (Character.isLowerCase(c)) {
                closure.add(pair);
            }
        }

        return closure;
    }

    static char[][] parseGrid(String input) {
        return Arrays.stream(input.split("\n")).map(String::toCharArray).toArray(char[][]::new);
    }

    static Map<Character, Set<Pair<Character, Integer>>> computeGraph(char[][] grid) {
        HashMap<Character, Set<Pair<Character, Integer>>> map = new HashMap<>();

        int height = grid.length;
        int width = grid[0].length;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                char c = grid[i][j];
                if (c == '.' || c == '#') continue;
                map.put(c, adjacent(grid, i, j, height, width));
            }
        }

        return map;
    }

    /* For a given position (i, j) compute all reachable keys or doors */
    static Set<Pair<Character, Integer>> adjacent(char[][] rows, int i, int j, int height, int width) {
        Predicate<Pair<Integer, Integer>> validPosition = p -> {
            int y = p.first();
            int x = p.second();
            return y >= 0 && y < height && x >= 0 && x < width;
        };

        Predicate<Pair<Integer, Integer>> isWall = p -> rows[p.first()][p.second()] == '#';
        Predicate<Pair<Integer, Integer>> validPositionAndNotWall = validPosition.and(Predicate.not(isWall));

        Queue<Tuple<Integer, Integer, Integer>> queue = new LinkedList<>();


        HashSet<Pair<Character, Integer>> result = new HashSet<>();

        queue.add(Tuple.of(i + 1, j, 1));
        queue.add(Tuple.of(i - 1, j, 1));
        queue.add(Tuple.of(i, j + 1, 1));
        queue.add(Tuple.of(i, j - 1, 1));

        HashSet<Pair<Integer, Integer>> visited = new HashSet<>();
        visited.add(Pair.of(i, j));

        while (!queue.isEmpty()) {
            Tuple<Integer, Integer, Integer> tuple = queue.poll();
            Pair<Integer, Integer> position = tuple.firstPair();
            int length = tuple.third();

            int y = position.first();
            int x = position.second();

            if (visited.contains(position) || isWall.test(Pair.of(y, x))) continue;
            if (rows[y][x] != '.') {
                result.add(Pair.of(rows[y][x], length));
                continue;
            }

            var up = Tuple.of(y - 1, x, length + 1);
            var right = Tuple.of(y, x + 1, length + 1);
            var down = Tuple.of(y + 1, x, length + 1);
            var left = Tuple.of(y, x - 1, length + 1);

            if (validPositionAndNotWall.test(up.firstPair())) queue.offer(up);
            if (validPositionAndNotWall.test(right.firstPair())) queue.offer(right);
            if (validPositionAndNotWall.test(down.firstPair())) queue.offer(down);
            if (validPositionAndNotWall.test(left.firstPair())) queue.offer(left);

            visited.add(position);
        }

        return result;
    }
}
