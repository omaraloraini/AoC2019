import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class Day13 {

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input13.txt");
    static long[] readProgram() throws IOException {
        String input = Files.readString(INPUT_PATH);
        return Arrays
                .stream(input
                        .split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong).toArray();
    }

    static class Part1 {
        static long answer() throws IOException {
            long[] memory = readProgram();
            Day9.IntCodeMachine machine = new Day9.IntCodeMachine(memory);
            ArcadeCabinet cabinet = new ArcadeCabinet(false);
            machine.runSynchronously(cabinet, cabinet);
            return cabinet.grid.values().stream().filter(tile -> tile == Tile.BLOCK).count();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            long[] memory = readProgram();
            memory[0] = 2;
            Day9.IntCodeMachine machine = new Day9.IntCodeMachine(memory);
            ArcadeCabinet cabinet = new ArcadeCabinet(false);
            machine.runSynchronously(cabinet, cabinet);
            return cabinet.getScore();
        }

        static long answerInteractive() throws IOException {
            long[] memory = readProgram();
            memory[0] = 2;
            Day9.IntCodeMachine machine = new Day9.IntCodeMachine(memory);
            ArcadeCabinet cabinet = new ArcadeCabinet(true);
            machine.runSynchronously(cabinet, cabinet);
            return cabinet.getScore();
        }
    }

    static class ArcadeCabinet implements LongSupplier, LongConsumer {

        final Map<Pair<Long, Long>, Tile> grid = new HashMap<>();

        static final int readXState = 0;
        static final int readYState = 1;
        static final int readIdState = 2;
        private final boolean interactiveRender;

        private long score = 0;

        private long x = 0;
        private long y = 0;
        private int state = 0;

        private long ballX = 0;
        private long paddleX = 0;

        ArcadeCabinet(boolean interactiveRender) {
            this.interactiveRender = interactiveRender;
        }

        private void stateTransition() {
            state = (state + 1) % 3;
        }

        @Override
        public void accept(long value) {
            switch (state) {
                case readXState:
                    x = value;
                    break;
                case readYState:
                    y = value;
                    break;
                case readIdState:
                    if (x == -1 && y == 0) {
                        score = value;
                    } else {
                        Tile tile = Tile.mapConstant((int) value);
                        grid.put(Pair.of(x, y), tile);
                        if (tile == Tile.BALL) {
                            ballX = x;
                        } else if(tile == Tile.HORIZONTAL_PADDLE) {
                            paddleX = x;
                        }
                    }
                    break;
            }
            stateTransition();
        }

        @Override
        public long getAsLong() {
            if (interactiveRender) {
                /* Clear console */
                System.out.print("\033[H\033[2J");
                System.out.flush();

                String render = render();
                System.out.println(render);
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (ballX < paddleX) {
                return -1;
            } else if (ballX > paddleX) {
                return 1;
            } else {
                return 0;
            }
        }

        String render() {
            int maxX = (int)(long) grid.keySet().stream().max(Comparator.comparingLong(Pair::first)).get().first();
            int maxY = (int)(long) grid.keySet().stream().max(Comparator.comparingLong(Pair::second)).get().second();

            String[][] array = new String[maxY + 1][maxX + 1];
            for (String[] strings : array) {
                Arrays.fill(strings, " ");
            }

            for (Map.Entry<Pair<Long, Long>, Tile> entry : grid.entrySet()) {
                Pair<Long, Long> key = entry.getKey();
                int x = (int)(long) key.first();
                int y = (int)(long) key.second();
                String text = mapTile(entry.getValue());
                array[y][x] = text;
            }


            StringBuilder builder = new StringBuilder();
            for (String[] row : array) {
               builder.append(String.join("", row)).append('\n');
            }

            builder.append("Score: ").append(score);
            return builder.toString();
        }

        private static String mapTile(Tile tile) {
            switch (tile) {
                case EMPTY: return " ";
                case WALL: return "|";
                case BLOCK: return "#";
                case HORIZONTAL_PADDLE: return "_";
                case BALL: return "*";
                default:
                    throw new Error("Should not get here");
            }
        }

        public long getScore() {
            return score;
        }
    }

    enum Tile {
        EMPTY(0),
        WALL(1),
        BLOCK(2),
        HORIZONTAL_PADDLE(3),
        BALL(4);

        final int constant;
        Tile(int constant) {

            this.constant = constant;
        }

        static Tile mapConstant(int constant) {
            for (Tile tile : Tile.values()) {
                if (tile.constant == constant) return tile;
            }
            throw new IllegalArgumentException("Unknown tile id: " + constant);
        }
    }
}
