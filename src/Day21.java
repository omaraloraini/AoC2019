import java.io.IOException;
import java.nio.file.Path;
import java.util.function.LongSupplier;

public class Day21 {

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input21.txt");

    static class Part1 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            String s = "NOT C T\n" +
                    "AND D T\n" +
                    "OR T J\n" +
                    "NOT A T\n" +
                    "OR T J\n" +
                    "WALK\n";

            long[] out = new long[1];

            machine.runSynchronously(new LongSupplier() {
                int index = 0;
                @Override
                public long getAsLong() {
                    return (int) s.charAt(index++);
                }
            }, value -> {
                if (value > 256) {
                    out[0] = value;
                }
            });

            return out[0];
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            String s = "NOT C J\n" +
                    "OR E T\n" +
                    "OR H T\n" +
                    "AND T J\n" +
                    "NOT B T\n" +
                    "OR T J\n" +
                    "AND D J\n" +
                    "NOT A T\n" +
                    "OR T J\n" +
                    "RUN\n";

            long[] out = new long[1];

            machine.runSynchronously(new LongSupplier() {
                int index = 0;

                @Override
                public long getAsLong() {
                    return (int) s.charAt(index++);
                }
            }, value -> {
                if (value > 256) {
                    out[0] = value;
                }
            });

            return out[0];
        }
    }
}