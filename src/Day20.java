import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Day20 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input20.txt");

    static class Part1 {
        static long answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            return shortestPath(input);
        }

        static int shortestPath(String input) {
            char[][] grid = Arrays.stream(input.split("\n")).map(String::toCharArray).toArray(char[][]::new);

            HashMap<String, List<Pair<Integer, Integer>>> portalsMap = findAllPortals(grid);

            Pair<Integer, Integer> start = portalsMap.get("AA").get(0);
            Pair<Integer, Integer> goal = portalsMap.get("ZZ").get(0);

            HashSet<Pair<Integer, Integer>> visited = new HashSet<>();

            Queue<Tuple<Integer, Integer, Integer>> queue = new LinkedList<>();
            queue.offer(Tuple.of(start.first(), start.second(), 0));

            while (!queue.isEmpty()) {
                Tuple<Integer, Integer, Integer> tuple = queue.poll();
                Pair<Integer, Integer> pair = tuple.firstPair();
                if (pair.equals(goal)) {
                    return tuple.third();
                }

                if (visited.contains(pair)) continue;

                visited.add(pair);
                for (Pair<Integer, Integer> move : moves(grid, portalsMap, pair.first(), pair.second())) {
                    queue.offer(Tuple.of(move.first(), move.second(), tuple.third() + 1));
                }
            }

            throw new Error("No path");
        }

        static List<Pair<Integer, Integer>> moves(char[][] grid, Map<String, List<Pair<Integer, Integer>>> portalsMap, int i, int j) {
            ArrayList<Pair<Integer, Integer>> list = new ArrayList<>();
            if (grid[i - 1][j] != '#') {
                if (grid[i - 1][j] == '.') list.add(Pair.of(i - 1, j));
                else list.addAll(portalsMap.get(readPortalName(grid, i, j, Direction.UP)));
            }

            if (grid[i + 1][j] != '#') {
                if (grid[i + 1][j] == '.') list.add(Pair.of(i + 1, j));
                else list.addAll(portalsMap.get(readPortalName(grid, i, j, Direction.DOWN)));
            }

            if (grid[i][j + 1] != '#') {
                if (grid[i][j + 1] == '.') list.add(Pair.of(i, j + 1));
                else list.addAll(portalsMap.get(readPortalName(grid, i, j, Direction.RIGHT)));
            }

            if (grid[i][j - 1] != '#') {
                if (grid[i][j - 1] == '.') list.add(Pair.of(i, j - 1));
                else list.addAll(portalsMap.get(readPortalName(grid, i, j, Direction.LEFT)));
            }

            return list;
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            return shortestPath(input);
        }

        static class TraversalState {
            final int i;
            final int j;
            final int level;
            final int length;

            List<TraversalState> history;

            TraversalState(int i, int j, int level, int length) {
                this.i = i;
                this.j = j;
                this.level = level;
                this.length = length;
            }

            TraversalState move(Direction direction) {
                switch (direction) {
                    case UP: return new TraversalState(i - 1, j, level, length + 1);
                    case DOWN: return new TraversalState(i + 1, j, level, length + 1);
                    case RIGHT: return new TraversalState(i, j + 1, level, length + 1);
                    case LEFT: return new TraversalState(i, j - 1, level, length + 1);
                }

                throw new Error();
            }

            Optional<TraversalState> recurse(char[][] grid, Map<String, List<Pair<Integer, Integer>>> portalsMap, Direction direction) {
                boolean outer = isOuterPortal(grid);
                for (Pair<Integer, Integer> pair : portalsMap.get(readPortalName(grid, i, j, direction))) {
                    if (pair.first() == i && pair.second() == j) continue;
                    if (level == 0 && outer) continue;
                    return Optional.of(new TraversalState(pair.first(), pair.second(), outer ? level - 1 : level + 1, length + 1));
                }
                return Optional.empty();
            }

            List<TraversalState> allMoves(char[][] grid, Map<String, List<Pair<Integer, Integer>>> portalsMap) {
                ArrayList<TraversalState> list = new ArrayList<>();

                if (grid[i - 1][j] != '#') {
                    if (grid[i - 1][j] == '.') list.add(this.move(Direction.UP));
                    else recurse(grid, portalsMap, Direction.UP).ifPresent(list::add);
                }

                if (grid[i + 1][j] != '#') {
                    if (grid[i + 1][j] == '.') list.add(this.move(Direction.DOWN));
                    else recurse(grid, portalsMap, Direction.DOWN).ifPresent(list::add);
                }

                if (grid[i][j + 1] != '#') {
                    if (grid[i][j + 1] == '.') list.add(this.move(Direction.RIGHT));
                    else recurse(grid, portalsMap, Direction.RIGHT).ifPresent(list::add);
                }

                if (grid[i][j - 1] != '#') {
                    if (grid[i][j - 1] == '.') list.add(this.move(Direction.LEFT));
                    else recurse(grid, portalsMap, Direction.LEFT).ifPresent(list::add);
                }

                return list;
            }

            boolean isOuterPortal(char[][] grid) {
                int height = grid.length;
                int width = grid[0].length;

                return i == 2 || i + 3 == height || j == 2 || j + 3 == width;
            }

        }

        static int shortestPath(String input) {
            char[][] grid = Arrays.stream(input.split("\n")).map(String::toCharArray).toArray(char[][]::new);

            HashMap<String, List<Pair<Integer, Integer>>> portalsMap = findAllPortals(grid);

            Pair<Integer, Integer> start = portalsMap.get("AA").get(0);
            Pair<Integer, Integer> zz = portalsMap.get("ZZ").get(0);
            Tuple<Integer, Integer, Integer> goal = Tuple.of(zz.first(), zz.second(), 0);

            HashSet<Tuple<Integer, Integer, Integer>> visited = new HashSet<>(); /* (i, j, level) */

            Queue<TraversalState> queue = new PriorityQueue<>(Comparator.comparingInt(t -> t.level));
            queue.offer(new TraversalState(start.first(), start.second(), 0, 0));

            while (!queue.isEmpty()) {
                TraversalState traversal = queue.poll();
                Tuple<Integer, Integer, Integer> state = Tuple.of(traversal.i, traversal.j, traversal.level);
                if (state.equals(goal)) {
                    return traversal.length;
                }

                if (visited.contains(state)) continue;

                visited.add(state);
                queue.addAll(traversal.allMoves(grid, portalsMap));
            }

            throw new Error("No path");
        }
    }

    /* grid[i][j] is at '.' before the portal */
    static String readPortalName(char[][] grid, int i, int j, Direction direction) {
        switch (direction) {
            case UP: return String.valueOf(grid[i - 2][j]) + grid[i - 1][j];
            case RIGHT: return String.valueOf(grid[i][j + 1]) + grid[i][j + 2];
            case DOWN: return String.valueOf(grid[i + 1][j]) + grid[i + 2][j];
            case LEFT: return String.valueOf(grid[i][j - 2]) + grid[i][j - 1];
        }

        throw new Error("Should not get here");
    }

    static HashMap<String, List<Pair<Integer, Integer>>> findAllPortals(char[][] grid) {
        HashMap<String, List<Pair<Integer, Integer>>> pairs = new HashMap<>();

        for (int i = 0; i < grid.length - 1; i++) {
            for (int j = 0; j < grid[i].length - 1; j++) {

                char a = grid[i][j];
                if (Character.isLetter(a)) {
                    char b = grid[i][j + 1];
                    if (Character.isLetter(b)) {
                        String key = String.valueOf(a) + b;
                        Pair<Integer, Integer> value = j > 0 && grid[i][j - 1] == '.' ? Pair.of(i, j - 1) : Pair.of(i, j + 2);
                        List<Pair<Integer, Integer>> list = pairs.putIfAbsent(key, new ArrayList<>(List.of(value)));
                        if (list != null) {
                            list.add(value);
                        }
                    }

                    b = grid[i + 1][j];
                    if (Character.isLetter(grid[i + 1][j])) {
                        String key = String.valueOf(a) + b;
                        Pair<Integer, Integer> value = i > 0 && grid[i - 1][j] == '.' ? Pair.of(i - 1, j) : Pair.of(i + 2, j);
                        List<Pair<Integer, Integer>> list = pairs.putIfAbsent(key, new ArrayList<>(List.of(value)));
                        if (list != null) {
                            list.add(value);
                        }
                    }
                }
            }
        }

        return pairs;
    }

    enum Direction {UP, RIGHT, DOWN, LEFT }
}
