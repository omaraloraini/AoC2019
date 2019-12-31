import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Day17 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input17.txt");

    static class Part1 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            AsciiGrid grid = new AsciiGrid(readGrid(machine));
            return grid.intersections.stream().mapToInt(p -> p.first() * p.second()).sum();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH, memory -> memory[0] = 2);
            MovementFunctions movementFunctions = findSolution();

            Function<String, String> format = s -> {
                StringBuilder builder = new StringBuilder();
                builder.append(s.charAt(0));
                for (int i = 1; i < s.length(); i++) {
                    if (
                            !Character.isDigit(s.charAt(i)) ||
                            !Character.isDigit(s.charAt(i - 1))
                    ) {
                        builder.append(',');
                    }
                    builder.append(s.charAt(i));
                }

                builder.append('\n');
                return builder.toString();
            };

            String robotInstructions =
                    format.apply(movementFunctions.routine) +
                    format.apply(movementFunctions.a) +
                    format.apply(movementFunctions.b) +
                    format.apply(movementFunctions.c) +
                    "n\n";

            long[] out = new long[1];

            machine.runSynchronously(new LongSupplier() {
                int index = 0;

                @Override
                public long getAsLong() {

                    return robotInstructions.charAt(index++);
                }
            }, l -> out[0] = l);

            return out[0];
        }

        static MovementFunctions findSolution() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            AsciiGrid grid = new AsciiGrid(readGrid(machine));
            return grid.visitAll().stream()
                    .map(MovementInstruction::coalesce)
                    .flatMap(s -> routines(s).stream())
                    .findAny()
                    .orElseThrow();
        }

        static Set<String> repeatedSubstrings(String s) {
            char[] chars = s.toCharArray();
            Set<String> strings = new HashSet<>();

            for (int i = chars.length - 1; i >= 0; i--) {

                for (int j = i - 2; j >= 0; j--) {
                    int k = i;
                    int l = j;

                    while (k > j && l >= 0 && chars[l] == chars[k]) {
                        l--;
                        k--;
                    }

                    if (i - k > 1) {
                        strings.add(s.substring(k + 1, i + 1));
                    }
                }
            }

            return strings;
        }

        static List<MovementFunctions> routines(String s) {
            String[] array = repeatedSubstrings(s).stream()
                    .filter(a -> a.length() > 1 && a.length() <= 10)
                    .collect(Collectors.toSet())
                    .toArray(String[]::new);

            ArrayList<MovementFunctions> movementFunctions = new ArrayList<>();
            ArrayList<String> routine = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                for (int j = i + 1; j < array.length; j++) {
                    NEXT:
                    for (int k = j + 1; k < array.length; k++) {
                        String a = array[i];
                        String b = array[j];
                        String c = array[k];

                        int index = 0;

                        routine.clear();
                        while (index < s.length()) {
                            if (routine.size() > 20) continue NEXT;

                            if (isPrefix(a, s, index)) {
                                index += a.length();
                                routine.add("A");
                            } else if (isPrefix(b, s, index)) {
                                index += b.length();
                                routine.add("B");
                            } else if (isPrefix(c, s, index)) {
                                index += c.length();
                                routine.add("C");
                            } else {
                                continue NEXT;
                            }
                        }

                        // If we get here we have found a solution.
                        movementFunctions.add(new MovementFunctions(a, b, c, String.join("", routine)));
                    }
                }
            }

            return movementFunctions;
        }

        static boolean isPrefix(String a, String b, int offset) {
            if (a.length() > b.length() - offset) return false;
            for (int i = 0; i < a.length(); i++) {
                if (a.charAt(i) != b.charAt(offset + i)) return false;
            }

            return true;
        }
    }

    static class AsciiGrid {
        private final ASCII[][] grid;
        private final int width;
        private final int height;
        private final int scaffoldCount;
        private int initialX;
        private int initialY;
        private Direction initialDirection;

        AsciiGrid(ASCII[][] grid) {
            this.grid = grid;
            height = grid.length;
            width = grid[0].length;

            for (ASCII[] row : grid) {
                if (row.length != width) throw new IllegalArgumentException("Non rectangular grid");
            }

            int sc = 1;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {

                    ASCII ascii = grid[i][j];
                    if (ascii == ASCII.SCAFFOLD) sc++;
                    else if (ascii != ASCII.SPACE) {
                        initialY = i;
                        initialX = j;
                        switch (ascii) {
                            case ROBOT_UP:
                                initialDirection = Direction.UP;
                                break;
                            case ROBOT_RIGHT:
                                initialDirection = Direction.RIGHT;
                                break;
                            case ROBOT_DOWN:
                                initialDirection = Direction.DOWN;
                                break;
                            case ROBOT_LEFT:
                                initialDirection = Direction.LEFT;
                                break;
                            default:
                                throw new Error("Should not get here.");
                        }
                    }
                }
            }

            scaffoldCount = sc;


            Set<Pair<Integer, Integer>> pairs = new HashSet<>();

            for (int i = 1; i < grid.length - 1; i++) {
                for (int j = 1; j < grid[i].length - 1; j++) {
                    if (
                            grid[i][j] == ASCII.SCAFFOLD &&
                                    grid[i + 1][j] == ASCII.SCAFFOLD &&
                                    grid[i - 1][j] == ASCII.SCAFFOLD &&
                                    grid[i][j + 1] == ASCII.SCAFFOLD &&
                                    grid[i][j - 1] == ASCII.SCAFFOLD) {

                        pairs.add(Pair.of(i, j));
                    }
                }
            }

            intersections = pairs;
        }

        final Set<Pair<Integer, Integer>> intersections;

        boolean inRange(int i, int j) {
            return i >= 0 && i < height && j >= 0 && j < width;
        }

        boolean isScaffold(int i, int j) {
            return grid[i][j] == ASCII.SCAFFOLD;
        }

        @Override
        public String toString() {
            return Arrays.stream(grid)
                    .map(row -> Arrays.stream(row)
                            .map(ASCII::toString)
                            .collect(Collectors.joining("")))
                    .collect(Collectors.joining("\n"));
        }

        private static class Move {
            final Pair<Integer, Integer> position;
            final Direction direction;

            Move(Pair<Integer, Integer> position, Direction direction) {
                Objects.requireNonNull(position);
                Objects.requireNonNull(direction);
                this.position = position;
                this.direction = direction;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Move move = (Move) o;
                return position.equals(move.position) &&
                        direction == move.direction;
            }

            @Override
            public int hashCode() {
                return Objects.hash(position, direction);
            }
        }

        List<List<MovementInstruction>> visitAll() {
            return new Traversal().visitAll();
        }

        private class Traversal {
            private ArrayList<Move> pathStack = new ArrayList<>(scaffoldCount);
            private ArrayList<Pair<Integer, Move>> alternativePaths = new ArrayList<>();
            ArrayList<List<MovementInstruction>> result;

            Traversal() {
                pathStack.add(new Move(Pair.of(initialY, initialX), initialDirection));
            }

            List<List<MovementInstruction>> visitAll() {
                if (result != null) return result;

                result = new ArrayList<>();
                for(;;) {
                    if (pathStack.size() >= scaffoldCount + intersections.size()) {
                        result.add(foldMoves(pathStack));
                        pathStack.add(popAlternative());
                    }

                    Move move = currentMove();
                    leftForward(move)
                            .filter(m -> isNotVisited(m.position))
                            .ifPresent(m -> alternativePaths.add(Pair.of(pathStack.size(), m)));

                    rightForward(move)
                            .filter(m -> isNotVisited(m.position))
                            .ifPresent(m -> alternativePaths.add(Pair.of(pathStack.size(), m)));

                    Optional<Move> next = forward(move);
                    if (next.isPresent() && isNotVisited(next.get().position)) {
                        pathStack.add(next.get());
                    } else {
                        if (alternativePaths.size() == 0) break;
                        pathStack.add(popAlternative());
                    }
                }

                return result;
            }

            List<MovementInstruction> foldMoves(List<Move> moves) {
                ArrayList<MovementInstruction> instructions = new ArrayList<>();
                Direction direction = moves.get(0).direction;

                for (int i = 1; i < moves.size(); i++) {
                    Move move = moves.get(i);
                    if (move.direction != direction) {
                        if (direction.right() == move.direction) {
                            direction = direction.right();
                            instructions.add(MovementInstruction.TURN_RIGHT);
                        } else {
                            direction = direction.left();
                            instructions.add(MovementInstruction.TURN_LEFT);
                        }
                    }

                    instructions.add(MovementInstruction.MOVE_FORWARD);
                }

                return instructions;
            }

            private Move currentMove() { return pathStack.get(pathStack.size() - 1); }
            private Move popAlternative() {
                Pair<Integer, Move> last = alternativePaths.remove(alternativePaths.size() - 1);
                int index = last.first();
                if (pathStack.size() > index + 1) {
                    pathStack.subList(index + 1, pathStack.size()).clear();
                }

                return last.second();
            }

            private boolean isNotVisited(Pair<Integer, Integer> p) {
                return intersections.contains(p) ||
                        pathStack.stream().map(m -> m.position).noneMatch(p::equals);
            }

            private Optional<Move> forward(Move m) {
                int i = m.position.first();
                int j = m.position.second();
                switch (m.direction) {
                    case UP:
                        i--;
                        break;
                    case RIGHT:
                        j++;
                        break;
                    case DOWN:
                        i++;
                        break;
                    case LEFT:
                        j--;
                        break;
                }

                return inRange(i, j) && isScaffold(i, j) ? Optional.of(new Move(Pair.of(i, j), m.direction)) : Optional.empty();
            }


            private Optional<Move> leftForward(Move m) {
                return forward(new Move(m.position, m.direction.left()));
            }

            private Optional<Move> rightForward(Move m) {
                return forward(new Move(m.position, m.direction.right()));
            }
        }
    }

    static class MovementFunctions {
        final String a;
        final String b;
        final String c;
        final String routine;

        MovementFunctions(String a, String b, String c, String routine) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.routine = routine;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MovementFunctions that = (MovementFunctions) o;
            return Objects.equals(a, that.a) &&
                    Objects.equals(b, that.b) &&
                    Objects.equals(c, that.c) &&
                    Objects.equals(routine, that.routine);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c, routine);
        }

        @Override
        public String toString() {
            return "MovementFunctions{" +
                    "a='" + a + '\'' +
                    ", b='" + b + '\'' +
                    ", c='" + c + '\'' +
                    ", routine='" + routine + '\'' +
                    '}';
        }
    }

    enum MovementInstruction {
        TURN_LEFT, TURN_RIGHT, MOVE_FORWARD;

        static String coalesce(List<MovementInstruction> instructions) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < instructions.size(); i++) {
                MovementInstruction instruction = instructions.get(i);
                if (instruction == TURN_LEFT) {
                    buffer.append("L");
                } else if (instruction == TURN_RIGHT) {
                    buffer.append("R");
                } else {
                    int j = i + 1;
                    while (j < instructions.size() && instructions.get(j) == MOVE_FORWARD) j++;
                    buffer.append(j - i);
                    i = j - 1;
                }
            }

            return buffer.toString();
        }
    }

    static ASCII[][] readGrid(IntCodeMachine machine) {
        ArrayList<ArrayList<ASCII>> grid = new ArrayList<>();
        grid.add(new ArrayList<>());

        machine.runSynchronously(() -> {
            throw new IllegalStateException();
        }, ascii -> {
            char c = (char) ascii;
            if (c == '\n') {
                grid.add(new ArrayList<>());
            } else {
                ArrayList<ASCII> row = grid.get(grid.size() - 1);
                row.add(ASCII.fromSymbol(c));
            }
        });

        return grid.stream()
                .filter(Predicate.not(ArrayList::isEmpty))
                .map(row -> row.toArray(ASCII[]::new))
                .toArray(ASCII[][]::new);
    }

    enum Direction {
        UP(0), RIGHT(1), DOWN(2), LEFT(3);

        private final int constant;

        Direction(int constant) {
            this.constant = constant;
        }

        static Optional<Direction> fromConstant(int constant) {
            return Arrays.stream(values()).filter(direction -> direction.constant == constant)
                    .findFirst();
        }

        Direction right() {
            int i = Math.floorMod(this.constant + 1, 4);
            return fromConstant(i).orElseThrow();
        }

        Direction left() {
            int i = Math.floorMod(this.constant - 1, 4);
            return fromConstant(i).orElseThrow();
        }
    }

    enum ASCII {
        SCAFFOLD('#'), SPACE('.'), ROBOT_UP('^'), ROBOT_RIGHT('>'), ROBOT_DOWN('v'), ROBOT_LEFT('<'), ROBOT_X('X');

        private final char symbol;

        ASCII(char symbol) {
            this.symbol = symbol;
        }

        static ASCII fromSymbol(char symbol) {
            return Arrays.stream(ASCII.values()).filter(ascii -> ascii.symbol == symbol).findFirst().orElseThrow();
        }

        @Override
        public String toString() {
            return String.valueOf(symbol);
        }
    }
}
