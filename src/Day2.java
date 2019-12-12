import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Day2 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day2.Part1.answer());
        System.out.println(Day2.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input2.txt");
    static final long PART2_PRODUCT = 19690720;

    enum OpCode {
        ADD(1, 4),
        MULTIPLY(2, 4),
        HALT(99, 1);

        final int constant;
        final int size;

        OpCode(int constant, int size) {
            this.constant = constant;
            this.size = size;
        }

        static OpCode mapConstant(int constant) {
            for (OpCode opCode : OpCode.values()) {
                if (opCode.constant == constant) return opCode;
            }
            throw new IllegalArgumentException("Unknown OpCode: " + constant);
        }
    }

    static class Part1 {
        static long answer() throws IOException {
            var source = Arrays.stream(Files
                    .readString(INPUT_PATH)
                    .split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();

            source[1] = 12;
            source[2] = 2;

            runIntCode(source);

            return source[0];
        }

        /* Mutates parameter memory */
        static void runIntCode(int[] memory) {
            int cursor = 0;

            while (true) {
                OpCode opcode = OpCode.mapConstant(memory[cursor]);

                switch (opcode) {
                    case ADD:
                    case MULTIPLY:
                        int op1 = memory[memory[cursor + 1]];
                        int op2 = memory[memory[cursor + 2]];
                        int dst = memory[cursor + 3];

                        int value = opcode == OpCode.ADD ? op1 + op2 : op1 * op2;
                        memory[dst] = value;
                        break;
                    case HALT:
                        return;
                }

                cursor += opcode.size;
            }
        }
    }

    static class Part2 {
        /* 100 * noun + verb = PART2_PRODUCT */
        static long answer() throws IOException {
            var source = Arrays.stream(Files
                    .readString(INPUT_PATH)
                    .split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();

            for (int noun = 0; noun < 100; noun++)
                for (int verb = 0; verb < 100; verb++) {
                    if (copyAndRun(source, noun, verb) == PART2_PRODUCT) {
                        return 100 * noun + verb;
                    }
                }

            throw new RuntimeException("No answer was found.");
        }

        /* Copies memory, updates values in index 0 and 1 with i and j */
        static long copyAndRun(int[] memory, int i, int j) {
            int[] copy = Arrays.copyOf(memory, memory.length);

            copy[1] = i;
            copy[2] = j;

            Part1.runIntCode(copy);
            return copy[0];
        }
    }
}
