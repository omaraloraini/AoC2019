import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class Day24 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    private static Path INPUT_PATH = Path.of("input24.txt");

    static class Part1 {
        static long answer() throws IOException {
            String s = Files.readString(INPUT_PATH);
            BooleanGrid grid = readGrid(s);

            HashSet<BooleanGrid> grids = new HashSet<>();
            while (!grids.contains(grid)) {
                grids.add(grid);
                grid = simulate(grid);
            }

            return bioDiversity(grid);
        }

        static int bioDiversity(BooleanGrid grid) {
            int d = 0;
            int k = 1;

            for (int i = 0; i < GRID_SIZE; i++)
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (grid.get(i, j)) d += k;
                    k = k << 1;
                }

            return d;
        }

        static BooleanGrid simulate(BooleanGrid grid) {
            BiFunction<Integer, Integer, Integer> adjacentBugs = (i, j) ->
                    (i > 0 && grid.get(i - 1, j) ? 1 : 0) +
                            (j > 0 && grid.get(i, j - 1) ? 1 : 0) +
                            (i < GRID_SIZE - 1 && grid.get(i + 1, j) ? 1 : 0) +
                            (j < GRID_SIZE - 1 && grid.get(i, j + 1) ? 1 : 0);

            BooleanGrid next = new BooleanGrid();

            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (grid.get(i , j)) /* Bug */{
                        next.set(i, j, adjacentBugs.apply(i, j) == 1);
                    } else {
                        int bugs = adjacentBugs.apply(i, j);
                        next.set(i, j, bugs == 1 || bugs == 2);
                    }
                }
            }

            return next;
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            String s = Files.readString(INPUT_PATH);
            BooleanGrid grid = readGrid(s);
            Simulation simulation = new Simulation(grid);

            for (int i = 0; i < 200; i++) {
                simulation.simulate();
            }

            return simulation.countBugs();
        }

        static class Simulation {
            private BooleanGrid outerGrid;

            Simulation(BooleanGrid grid) {
                outerGrid = grid;
            }

            boolean shouldExpandOut() {
                return willInfest(outerGrid.countBugs(BooleanGrid.topRow)) ||
                        willInfest(outerGrid.countBugs(BooleanGrid.rightColumn)) ||
                        willInfest(outerGrid.countBugs(BooleanGrid.bottomRow)) ||
                        willInfest(outerGrid.countBugs(BooleanGrid.leftColumn));
            }

            void simulate() {
                expandIfNecessary();
                ArrayList<Pair<Tuple<BooleanGrid, Integer, Integer>, Boolean>> ops = new ArrayList<>();

                for (BooleanGrid prev = null, curr = outerGrid; curr != null; prev = curr, curr = curr.getNext()) {
                    for (int i = 0; i < GRID_SIZE; i++) {
                        for (int j = 0; j < GRID_SIZE; j++) {
                            int bugs = countAdjacentBugs(prev, curr, curr.getNext(), i, j);
                            if (curr.get(i, j)) { /* Bug */
                                ops.add(Pair.of(Tuple.of(curr, i, j), bugs == 1));
                            } else {
                                ops.add(Pair.of(Tuple.of(curr, i, j), bugs == 1 || bugs == 2));
                            }
                        }
                    }
                }

                for (Pair<Tuple<BooleanGrid, Integer, Integer>, Boolean> op : ops) {
                    Tuple<BooleanGrid, Integer, Integer> tuple = op.first();
                    tuple.first().set(tuple.second(), tuple.third(), op.second());
                }
            }

            int countBugs() {
                int count = 0;
                BooleanGrid grid = this.outerGrid;
                while (true) {
                    count += grid.countBugs();
                    if (!grid.hasNext()) break;
                    grid = grid.getNext();
                }
                return count;
            }

            BooleanGrid innerMost() {
                BooleanGrid grid = outerGrid;
                while (grid.hasNext()) {
                    grid = grid.getNext();
                }
                return grid;
            }

            boolean shouldExpandIn() {
                BooleanGrid grid = innerMost();
                return grid.get(midTop.first(), midTop.second()) || grid.get(midRight.first(), midRight.second()) || grid.get(midBottom.first(), midBottom.second()) || grid.get(midLeft.first(), midLeft.second());
            }

            void expandIfNecessary() {
                if (shouldExpandOut()) {
                    BooleanGrid o = new BooleanGrid();
                    o.setNext(outerGrid);
                    outerGrid = o;
                }

                if (shouldExpandIn()) {
                    BooleanGrid grid = innerMost();
                    grid.setNext(new BooleanGrid());
                }
            }

            int countAdjacentBugs(BooleanGrid prev, BooleanGrid curr, BooleanGrid next, int i, int j) {
                if (i == 2 && j == 2) return 0;

                int count = (i > 0 && curr.get(i - 1, j) ? 1 : 0) +
                        (j > 0 && curr.get(i, j - 1) ? 1 : 0) +
                        (i < GRID_SIZE - 1 && curr.get(i + 1, j) ? 1 : 0) +
                        (j < GRID_SIZE - 1 && curr.get(i, j + 1) ? 1 : 0);

                if (prev != null && i == 0) count += prev.get(midTop.first(), midTop.second()) ? 1 : 0;
                if (prev != null && j == GRID_SIZE - 1) count += prev.get(midRight.first(), midRight.second()) ? 1 : 0;
                if (prev != null && i == GRID_SIZE - 1) count += prev.get(midBottom.first(), midBottom.second()) ? 1 : 0;
                if (prev != null && j == 0) count += prev.get(midLeft.first(), midLeft.second()) ? 1 : 0;

                if (next != null && i == midTop.first() && j == midTop.second())
                    for (int k = 0; k < GRID_SIZE; k++) count += next.get(0, k) ? 1 : 0;

                if (next != null && i == midRight.first() && j == midRight.second())
                    for (int k = 0; k < GRID_SIZE; k++) count += next.get(k, GRID_SIZE - 1) ? 1 : 0;

                if (next != null && i == midBottom.first() && j == midBottom.second())
                    for (int k = 0; k < GRID_SIZE; k++) count += next.get(GRID_SIZE - 1, k) ? 1 : 0;

                if (next != null && i == midLeft.first() && j == midLeft.second())
                    for (int k = 0; k < GRID_SIZE; k++) count += next.get(k, 0) ? 1 : 0;

                return count;
            }

            boolean willInfest(int bugs) {
                return bugs == 1 || bugs == 2;
            }

            final static Pair<Integer, Integer> midTop = Pair.of(1, 2);
            final static Pair<Integer, Integer> midRight = Pair.of(2, 3);
            final static Pair<Integer, Integer> midBottom = Pair.of(3, 2);
            final static Pair<Integer, Integer> midLeft = Pair.of(2, 1);
        }
    }


    static BooleanGrid readGrid(String s) {
        BooleanGrid grid = new BooleanGrid();
        String[] rows = s.split("\n");
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows[i].length(); j++) {
                if (rows[i].charAt(j) == '#') grid.set(i, j, true);
                else grid.set(i, j, false);
            }
        }

        return grid;
    }

    static final int GRID_SIZE = 5;
    static class BooleanGrid {
        private BooleanGrid next;
        final boolean[] array;

        BooleanGrid() {
            array = new boolean[GRID_SIZE * GRID_SIZE];
        }

        boolean get(int i, int j) {
            return array[i * GRID_SIZE + j];
        }

        void set(int i, int j, boolean value) {
            array[i * GRID_SIZE + j] =  value;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (get(i, j)) builder.append('#');
                    else builder.append('.');
                }
                builder.append('\n');
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BooleanGrid that = (BooleanGrid) o;
            return Arrays.equals(array, that.array);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }

        public boolean hasNext() {
            return next != null;
        }

        public BooleanGrid getNext() {
            return next;
        }

        public void setNext(BooleanGrid next) {
            this.next = next;
        }

        static final BiPredicate<Integer, Integer> topRow = (i, j) -> i == 0;
        final static BiPredicate<Integer, Integer> bottomRow = (i, j) -> i == GRID_SIZE - 1;
        static final BiPredicate<Integer, Integer> leftColumn = (i, j) -> j == 0;
        static final BiPredicate<Integer, Integer> rightColumn = (i, j) -> j == GRID_SIZE - 1;

        int countBugs() {
            int count = 0;
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    count += get(i, j) ? 1 : 0;
                }
            }
            return count;
        }

        int countBugs(BiPredicate<Integer, Integer> predicate) {
            int count = 0;
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    if (predicate.test(i, j)) count += get(i, j) ? 1 : 0;
                }
            }
            return count;
        }
    }
}
