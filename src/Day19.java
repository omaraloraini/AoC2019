import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class Day19 {
    static final Path INPUT_PATH = Path.of(".", "input19.txt");

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static class Part1 {
        static long answer() throws IOException {
            DroneSystem system = new DroneSystem();
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            return IntStream.range(0, 50)
                    .map(i -> IntStream.range(0, 50)
                            .map(j -> system.deploy(machine.fork(), i, j) ? 1 : 0)
                            .sum())
                    .sum();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            Map<Integer, Pair<Integer, Integer>> grid = buildGrid();

            /* assumes square is below y = 1500 */
            for (int y = 100; y < 1500; y++) {
                Pair<Integer, Integer> pair = grid.get(y);
                int x1 = pair.first();
                int x2 = pair.second();

                if (x2 - x1 + 1 < 100) continue;

                int yTop = y - 100 + 1;
                Pair<Integer, Integer> topPair = grid.get(yTop);
                if (topPair.second() - x1 + 1 < 100) continue;

                return x1 * 10000 + yTop;
            }

            throw new Error();
        }

        static Map<Integer, Pair<Integer, Integer>> buildGrid() throws IOException {
            DroneSystem system = new DroneSystem();
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            Supplier<IntCodeMachine> newRobot = machine::fork;
            HashMap<Integer, Pair<Integer, Integer>> map = new HashMap<>();

            int x1 = 0;
            while (!system.deploy(newRobot.get(), x1, 100)) x1++;

            int x2 = x1;
            while (system.deploy(newRobot.get(), x2, 100)) x2++;
            x2--;

            map.put(100, Pair.of(x1, x2));

            LineEquation line1 = new LineEquation(0, x1, 0, 100);
            LineEquation line2 = new LineEquation(0, x2, 0, 100);

            for (int y = 100 + 1; y < 1500; y++) {
                x1 = ((int) Math.round(line1.x(y)));

                if (system.deploy(newRobot.get(), x1, y)) { // Try moving left
                    while (system.deploy(newRobot.get(), x1 - 1, y)) x1--;
                } else {
                    x1++;
                    while (!system.deploy(newRobot.get(), x1, y)) x1++;
                }

                x2 = ((int) Math.round(line2.x(y)));
                if (system.deploy(newRobot.get(), x2, y)) { // Try moving right
                    while (system.deploy(newRobot.get(), x2 + 1, y)) x2++;
                } else {
                    x2--;
                    while (!system.deploy(newRobot.get(), x2, y)) x2--;
                }

                map.put(y, Pair.of(x1, x2));
            }

            return map;
        }

        static class LineEquation {
            private final double x2;
            private final double y2;
            private final double m;

            public LineEquation(double x1, double x2, double y1, double y2) {
                this.x2 = x2;
                this.y2 = y2;
                this.m = (y2 - y1) / (x2 - x1);
            }

            double x(double y) {
                return ((y - y2) / m) + x2;
            }

            double y(double x) {
                return ((x - x2) * m) + y2;
            }
        }
    }

    static class DroneSystem implements LongSupplier, LongConsumer {

        boolean isX;
        int x;
        int y;
        boolean isStationary;

        boolean deploy(IntCodeMachine machine, int x, int y) {
            if (x < 0) throw new RuntimeException("x < 0");
            if (y < 0) throw new RuntimeException("y < 0");
            this.x = x;
            this.y = y;
            isX = true;
            machine.runSynchronously(this, this);
            return isStationary;
        }

        @Override
        public void accept(long value) {
            if (value == 0) {
                isStationary = false;
            } else if (value == 1) {
                isStationary = true;
            } else {
                throw new Error();
            }
        }

        @Override
        public long getAsLong() {
            if (isX) {
                isX = false;
                return x;
            } else {
                isX = true;
                return y;
            }
        }
    }
}
