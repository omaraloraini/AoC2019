import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class Day5 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day5.Part1.answer());
        System.out.println(Day5.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input5.txt");

    enum AddressingMode {
        POSITION(0),
        IMMEDIATE(1);

        final int constant;
        AddressingMode(int constant) {
            this.constant = constant;
        }

        static AddressingMode mapConstant(int constant) {
            for (AddressingMode mode : AddressingMode.values()) {
                if (mode.constant == constant) return mode;
            }
            throw new IllegalArgumentException("Unknown OpCode: " + constant);
        }
    }

    enum OpCode {
        ADD(1, 4),
        MULTIPLY(2, 4),
        LESS_THAN(7, 4),
        EQUAL(8, 4),
        HALT(99, 1),
        INPUT(3 , 2),
        OUTPUT(4, 2),
        JUMP_IF_TRUE(5, 3),
        JUMP_IF_FALSE(6, 3);

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

    static void runIntCode(int[] memory, IntSupplier read, IntConsumer write) {

        BiFunction<AddressingMode, Integer, Integer> load = (mode, i) ->
                mode == AddressingMode.POSITION
                        ? memory[memory[i]]
                        : memory[i];

        Map<OpCode, IntBinaryOperator> opCodeOperationMap = Map.of(
                OpCode.ADD, Integer::sum,
                OpCode.MULTIPLY, (i, j) -> i * j,
                OpCode.LESS_THAN, (i, j) -> i < j ? 1 : 0,
                OpCode.EQUAL, (i, j) -> i == j ? 1 : 0
        );

        int cursor = 0;
        for (;;) {

            int instruction = memory[cursor];
            OpCode opcode = OpCode.mapConstant(instruction % 100);
            AddressingMode firstMode = AddressingMode.mapConstant((instruction / 100) % 10);
            AddressingMode secondMode = AddressingMode.mapConstant((instruction / 1000) % 10);
            int nextInstruction = cursor + opcode.size;

            switch (opcode) {
                case ADD:
                case MULTIPLY:
                case LESS_THAN:
                case EQUAL:
                    int op1 = load.apply(firstMode, cursor + 1);
                    int op2 = load.apply(secondMode, cursor + 2);
                    int dstAddress = load.apply(AddressingMode.IMMEDIATE, cursor + 3);

                    memory[dstAddress] = opCodeOperationMap.get(opcode).applyAsInt(op1, op2);
                    break;
                case INPUT:
                    op1 = read.getAsInt();
                    dstAddress = load.apply(AddressingMode.IMMEDIATE, cursor + 1);
                    memory[dstAddress] = op1;
                    break;
                case OUTPUT:
                    op1 = load.apply(AddressingMode.POSITION, cursor + 1);
                    write.accept(op1);
                    break;
                case JUMP_IF_TRUE:
                case JUMP_IF_FALSE:
                    op1 = load.apply(firstMode, cursor + 1);
                    op2 = load.apply(secondMode, cursor + 2);

                    if (opcode == OpCode.JUMP_IF_TRUE && op1 != 0)
                        nextInstruction = op2;
                    else if (opcode == OpCode.JUMP_IF_FALSE && op1 == 0)
                        nextInstruction = op2;

                    break;
                case HALT:
                    return;
            }

            cursor = nextInstruction;
        }
    }

    private static int[] readProgram() throws IOException {
        return Arrays.stream(Files
                .readString(INPUT_PATH)
                .split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static class Part1 {
        public static long answer() throws IOException {
            var source = readProgram();

            int[] out = new int[1];
            runIntCode(source, () -> 1, w -> out[0] = w);
            return out[0];
        }

    }
    private static class Part2 {
        public static long answer() throws IOException {
            var source = readProgram();

            int[] out = new int[1];
            runIntCode(source, () -> 5, w -> out[0] = w);

            return out[0];
        }
    }
}
