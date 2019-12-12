import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class Day11 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day11.Part1.answer());
        System.out.println(Day11.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input11.txt");

    private static class Part1 {
        public static long answer() throws IOException {
            PaintingRobot robot = new PaintingRobot(0, 0, PaintingRobot.Direction.UP, readProgram(), Color.BLACK);
            robot.paint();
            return robot.colorMap.size();
        }
    }

    private static class Part2 {
        public static String answer() throws IOException {
            PaintingRobot robot = new PaintingRobot(0, 0, PaintingRobot.Direction.UP, readProgram(), Color.WHITE);
            robot.paint();
            return draw(robot.colorMap);
        }

        static final String WHITE =  "#";
        static final String BLACK =  " ";

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        static String draw(Map<Pair<Integer, Integer>, Color> colorMap) {
            Set<Pair<Integer, Integer>> pairs = colorMap.keySet();
            assert pairs.size() > 0;
            int minX = pairs.stream().min(Comparator.comparingInt(Pair::first)).get().first();
            int minY = pairs.stream().min(Comparator.comparingInt(Pair::second)).get().second();
            int maxX = pairs.stream().max(Comparator.comparingInt(Pair::first)).get().first();
            int maxY = pairs.stream().max(Comparator.comparingInt(Pair::second)).get().second();

            Color[][] grid = new Color[maxY - minY + 1][maxX - minX + 1];
            for (Map.Entry<Pair<Integer, Integer>, Color> entry : colorMap.entrySet()) {
                Pair<Integer, Integer> position = entry.getKey();
                int x = position.first() + Math.abs(minX);
                int y = position.second() + Math.abs(minY);
                Color color = entry.getValue();
                grid[y][x] = color;
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = grid.length - 1; i >= 0; i--) {
                Color[] row = grid[i];
                for (Color color : row) {
                    String s = color == null ? " " : color == Color.BLACK ? BLACK : WHITE;
                    stringBuilder.append(s);
                }
                stringBuilder.append('\n');
            }

            return stringBuilder.toString();
        }
    }

    private static long[] readProgram() throws IOException {
        String input = Files.readString(INPUT_PATH);
        return Arrays
                .stream(input
                        .split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong).toArray();
    }

    enum Color {
        BLACK(0), WHITE(1);

        private final long constant;
        Color(long constant) {

            this.constant = constant;
        }

        static Color mapConstant(long constant) {
            for (Color color : Color.values()) {
                if (color.constant == constant) return color;
            }
            throw new IllegalArgumentException("Unknown Color constant:" + constant);
        }
    }

    static class HashMapWithDefault<K, V> extends HashMap<K, V> {
        private final V defaultValue;

        HashMapWithDefault(V defaultValue) {
            this.defaultValue = defaultValue;
        }
        @Override
        public V get(Object key) {
            V v = super.get(key);
            return v == null ? defaultValue : v;
        }
    }

    static class PaintingRobot implements LongSupplier, LongConsumer {
        private int x;
        private int y;

        enum Direction {
            LEFT, UP, RIGHT, DOWN;

            Direction turnLeft() {
                switch (this) {
                    case LEFT: return DOWN;
                    case UP: return LEFT;
                    case RIGHT: return UP;
                    case DOWN: return RIGHT;
                    default: throw new Error("Should not get here");
                }
            }

            Direction turnRight() {
                switch (this) {
                    case LEFT: return UP;
                    case UP: return RIGHT;
                    case RIGHT: return DOWN;
                    case DOWN: return LEFT;
                    default: throw new Error("Should not get here");
                }
            }
        }

        private Direction direction;
        private final Day9.IntCodeMachine machine;
        private final HashMap<Pair<Integer, Integer>, Color> colorMap;
        boolean setColor = true;

        PaintingRobot(int x, int y, Direction initialDirection, long[] intCode, Color initialColor) {
            this.x = x;
            this.y = y;
            this.direction = initialDirection;
            machine = new Day9.IntCodeMachine(intCode);
            colorMap = new HashMapWithDefault<>(Color.BLACK);
            colorMap.put(Pair.of(x, y), initialColor);
        }

        void paint() {
            machine.runSynchronously(this, this);
        }

        @Override
        public void accept(long value) {
            if (setColor) {
                colorMap.put(Pair.of(x, y), Color.mapConstant(value));
            } else {
                if (value == 0) {
                    direction = direction.turnLeft();
                } else if (value == 1) {
                    direction = direction.turnRight();
                } else {
                    throw new RuntimeException("Unknown turn: " + value);
                }

                switch (direction) {
                    case LEFT:
                        x = x - 1;
                        break;
                    case UP:
                        y = y + 1;
                        break;
                    case RIGHT:
                        x = x + 1;
                        break;
                    case DOWN:
                        y = y - 1;
                        break;
                }
            }

            setColor = !setColor;
        }

        @Override
        public long getAsLong() {
            return colorMap.get(Pair.of(x, y)).constant;
        }
    }
}
