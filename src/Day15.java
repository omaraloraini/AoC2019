import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Day15 {

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input15.txt");

    static class Part1 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            RepairDroidController controller = new RepairDroidController(machine);
            return controller.shortestPathToOxygen();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            RepairDroidController controller = new RepairDroidController(machine);
            return controller.oxygenSpreadTime();
        }
    }

    enum Direction {
        NORTH(1), SOUTH(2), WEST(3), EAST(4);

        private final long constant;

        Direction(long constant) {

            this.constant = constant;
        }

        private  Direction inverse() {
            switch (this) {
                case NORTH:
                    return Direction.SOUTH;
                case SOUTH:
                    return Direction.NORTH;
                case WEST:
                    return Direction.EAST;
                case EAST:
                    return Direction.WEST;
                default:
                    throw new Error();
            }
        }

        static Direction mapConstant(long constant) {
            for (Direction direction : Direction.values()) {
                if (direction.constant == constant) return direction;
            }
            throw new IllegalArgumentException("Unknown direction: " + constant);
        }
    }

    enum ReplyStatus {
        WALL(0), EMPTY(1), OXYGEN(2);

        private final long constant;

        ReplyStatus(long constant) {

            this.constant = constant;
        }

        static ReplyStatus mapConstant(long constant) {
            for (ReplyStatus status : ReplyStatus.values()) {
                if (status.constant == constant) return status;
            }
            throw new IllegalArgumentException("Unknown reply: " + constant);
        }
    }

    static class RepairDroidController implements LongSupplier, LongConsumer {

        private final IntCodeMachine machine;
        private final ArrayList<Direction> stack;
        private HashMap<Pair<Integer, Integer>, ReplyStatus> grid;
        private int x;
        private int y;
        private Pair<Integer, Integer> oxygenPosition;


        public RepairDroidController(IntCodeMachine machine) {
            this.machine = machine;
            grid = new HashMap<>();
            x = 0;
            y = 0;
            grid.put(Pair.of(x, y), ReplyStatus.EMPTY);
            stack = new ArrayList<>();
            state = State.SEARCHING;
        }


        State state;
        enum State {
            SEARCHING, MOVING_BACK, OXYGEN_FOUND;
            static Direction attemptedDirection;
        }

        private void updatePosition(Direction direction) {
            switch (direction) {
                case NORTH:
                    y--;
                    break;
                case SOUTH:
                    y++;
                    break;
                case WEST:
                    x--;
                    break;
                case EAST:
                    x++;
                    break;
            }
        }

        private void attemptMove(Direction direction) {
            State.attemptedDirection = direction;
            updatePosition(direction);
        }

        private void commitMove() {
            stack.add(State.attemptedDirection);
        }

        private void rollBackMove() {
            Direction inverse = State.attemptedDirection.inverse();
            updatePosition(inverse);
        }

        private Direction moveBack() {
            state = State.MOVING_BACK;
            Direction direction = stack.remove(stack.size() - 1).inverse();
            updatePosition(direction);
            return direction;
        }

        private List<Direction> nonVisitedAdjacent() {
            ArrayList<Direction> directions = new ArrayList<>();

            if (!grid.containsKey(Pair.of(x, y - 1)))
                directions.add(Direction.NORTH);

            if (!grid.containsKey(Pair.of(x, y + 1))) {
                directions.add(Direction.SOUTH);
            }

            if (!grid.containsKey(Pair.of(x + 1, y))) {
                directions.add(Direction.EAST);
            }

            if (!grid.containsKey(Pair.of(x - 1, y))) {
                directions.add(Direction.WEST);
            }


            return directions;
        }

        @Override
        public void accept(long value) {
            if (state == State.MOVING_BACK) {
                state = State.SEARCHING;
                return;
            }

            ReplyStatus status = ReplyStatus.mapConstant(value);
            grid.put(Pair.of(x, y), status);

            switch (status) {
                case WALL:
                    rollBackMove();
                    break;
                case EMPTY:
                    commitMove();
                    break;
                case OXYGEN:
                    commitMove();
                    oxygenPosition = Pair.of(x, y);
                    state = State.OXYGEN_FOUND;
                    break;
            }
        }

        @Override
        public long getAsLong() {
            List<Direction> reachable = nonVisitedAdjacent();
            if (reachable.isEmpty()) {

                if (stack.isEmpty()) return 0; // HALT

                Direction direction = moveBack();
                return direction.constant;
            } else {
                Direction direction = reachable.get(0);
                attemptMove(direction);
                return direction.constant;
            }
        }

        public void exploreGrid() {
            machine.runSynchronously(this, this);
        }

        public int shortestPathToOxygen() {
            if (state != State.OXYGEN_FOUND) exploreGrid();
            return shortestPathToOxygen(0, Pair.of(0, 0), Collections.emptySet());
        }

        public int shortestPathToOxygen(int length, Pair<Integer, Integer> position, Set<Pair<Integer, Integer>> visited) {
            if (grid.get(position) == ReplyStatus.OXYGEN) return length;
            else {
                HashSet<Pair<Integer, Integer>> newVisited = new HashSet<>(visited);
                newVisited.add(position);
                return nonWalledAdjacent(position.first(), position.second()).stream()
                        .filter(Predicate.not(visited::contains))
                        .mapToInt(p -> shortestPathToOxygen(length + 1, p, newVisited))
                        .min()
                        .orElse(Integer.MAX_VALUE);
            }
        }

        private List<Pair<Integer, Integer>> nonWalledAdjacent(int x, int y) {
            ArrayList<Pair<Integer, Integer>> adjacent = new ArrayList<>();

            if (grid.get(Pair.of(x, y - 1)) != ReplyStatus.WALL)
                adjacent.add(Pair.of(x, y - 1));

            if (grid.get(Pair.of(x, y + 1)) != ReplyStatus.WALL)
                adjacent.add(Pair.of(x, y + 1));

            if (grid.get(Pair.of(x + 1, y)) != ReplyStatus.WALL)
                adjacent.add(Pair.of(x + 1, y));

            if (grid.get(Pair.of(x - 1, y)) != ReplyStatus.WALL)
                adjacent.add(Pair.of(x - 1, y));

            return adjacent;
        }

        public int oxygenSpreadTime() {
            if (state != State.OXYGEN_FOUND) exploreGrid();

            long empty = grid.values().stream().filter(replyStatus -> replyStatus != ReplyStatus.WALL).count();
            List<Pair<Integer, Integer>> list = List.of(oxygenPosition);
            HashSet<Pair<Integer, Integer>> visited = new HashSet<>();

            int time = 0;
            while (visited.size() < empty) {

                list = list.stream()
                        .filter(Predicate.not(visited::contains))
                        .peek(visited::add)
                        .flatMap(p -> nonWalledAdjacent(p.first(), p.second()).stream())
                        .collect(Collectors.toList());

                time++;
            }

            return time - 1;
        }
    }
}
