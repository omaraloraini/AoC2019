import java.io.IOException;
import java.nio.file.Path;

public class Day9 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day9.Part1.answer());
        System.out.println(Day9.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input9.txt");

    static class Part1 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);

            long[] out = new long[1];
            machine.runSynchronously(() -> 1, l -> out[0] = l);
            return out[0];
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);

            long[] out = new long[1];
            machine.runSynchronously(() -> 2, l -> out[0] = l);
            return out[0];
        }
    }
}
