import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class IntCodeMachine {

    enum AddressingMode {
        POSITION(0),
        IMMEDIATE(1),
        RELATIVE(2);

        final int constant;

        AddressingMode(int constant) {
            this.constant = constant;
        }

        static AddressingMode mapConstant(int constant) {
            for (AddressingMode mode : AddressingMode.values()) {
                if (mode.constant == constant) return mode;
            }
            throw new IllegalArgumentException("Unknown Addressing mode: " + constant);
        }
    }

    enum OpCode {
        ADD(1, 4),
        MULTIPLY(2, 4),
        LESS_THAN(7, 4),
        EQUAL(8, 4),
        HALT(99, 1),
        INPUT(3, 2),
        OUTPUT(4, 2),
        JUMP_IF_TRUE(5, 3),
        JUMP_IF_FALSE(6, 3),
        SET_RELATIVE_BASE(9, 2);

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

    private long[] memory;
    private int cursor;
    private int relativeBase;

    public IntCodeMachine(long[] initialMemory) {
        memory = Arrays.copyOf(initialMemory, initialMemory.length);
        cursor = 0;
        relativeBase = 0;
    }

    static IntCodeMachine fromFile(Path path) throws IOException {
        String input = Files.readString(path);
        long[] memory = Arrays
                .stream(input
                        .split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .toArray();
        return new IntCodeMachine(memory);
    }

    static IntCodeMachine fromFile(Path path, Consumer<long[]> memoryUpdates) throws IOException {
        String input = Files.readString(path);
        long[] memory = Arrays
                .stream(input
                        .split(","))
                .map(String::trim)
                .mapToLong(Long::parseLong)
                .toArray();

        memoryUpdates.accept(memory);

        return new IntCodeMachine(memory);
    }

    private void ensureMemorySize(int address) {
        if (address >= memory.length) {
            memory = Arrays.copyOf(memory, memory.length * 2);
            ensureMemorySize(address);
        }
        if (address < 0) throw new RuntimeException("Negative address not allowed");
    }

    private long load(AddressingMode mode, int address) {
        switch (mode) {
            case POSITION:
                ensureMemorySize(address);
                address = (int) memory[address];
                ensureMemorySize(address);
                return memory[address];
            case IMMEDIATE:
                return memory[address];
            case RELATIVE:
                ensureMemorySize(address);
                address = (int) memory[address] + relativeBase;
                ensureMemorySize(address);
                return memory[address];
            default:
                throw new RuntimeException("Should not have gotten here");
        }
    }

    private void store(AddressingMode mode, int address, long value) {
        switch (mode) {
            case POSITION:
                ensureMemorySize(address);
                memory[address] = value;
                break;
            case RELATIVE:
                address = address + relativeBase;
                ensureMemorySize(address);
                memory[address] = value;
                break;
            case IMMEDIATE: throw new IllegalArgumentException("Can't store using immediate mode");
        }
    }

    private int loadCurrentInstruction() {
        ensureMemorySize(cursor);
        long l = memory[cursor];
        if (l > Integer.MAX_VALUE) throw new RuntimeException("l > Integer.MAX_VALUE");
        return (int) l;
    }

    private LongBinaryOperator operatorMap(OpCode opCode) {
        switch (opCode) {
            case ADD:
                return Long::sum;
            case MULTIPLY:
                return (i, j) -> i * j;
            case LESS_THAN:
                return (i, j) -> i < j ? 1 : 0;
            case EQUAL:
                return (i, j) -> i == j ? 1 : 0;
            default:
                throw new IllegalArgumentException(
                        "operatorMap maps two parameters opcodes, not the case for: " + opCode);
        }
    }

    private boolean fetchDecodeExecute(LongSupplier reader, LongConsumer writer) {
        int instruction = loadCurrentInstruction();
        OpCode opcode = OpCode.mapConstant(instruction % 100);
        AddressingMode firstMode = AddressingMode.mapConstant((instruction / 100) % 10);
        AddressingMode secondMode = AddressingMode.mapConstant((instruction / 1000) % 10);
        AddressingMode thirdMode = AddressingMode.mapConstant((instruction / 10000) % 10);
        int nextInstruction = cursor + opcode.size;

        switch (opcode) {
            case ADD:
            case MULTIPLY:
            case LESS_THAN:
            case EQUAL:
                long op1 = load(firstMode, cursor + 1);
                long op2 = load(secondMode, cursor + 2);
                int dstAddress = (int) load(AddressingMode.IMMEDIATE, cursor + 3);

                store(thirdMode, dstAddress, operatorMap(opcode).applyAsLong(op1, op2));
                break;
            case INPUT:
                long value = reader.getAsLong();
                dstAddress = (int) load(AddressingMode.IMMEDIATE, cursor + 1);
                store(firstMode, dstAddress, value);
                break;
            case OUTPUT:
                op1 = load(firstMode, cursor + 1);
                writer.accept(op1);
                break;
            case JUMP_IF_TRUE:
            case JUMP_IF_FALSE:
                op1 = load(firstMode, cursor + 1);
                op2 = load(secondMode, cursor + 2);

                if (opcode == OpCode.JUMP_IF_TRUE && op1 != 0)
                    nextInstruction = (int) op2;
                else if (opcode == OpCode.JUMP_IF_FALSE && op1 == 0)
                    nextInstruction = (int) op2;

                break;
            case SET_RELATIVE_BASE:
                op1 = load(firstMode, cursor + 1);
                relativeBase = relativeBase + (int) op1;
                break;
            case HALT:
                return true;
        }

        cursor = nextInstruction;
        return false;
    }

    class AsyncRun {
        private final LongConsumer writer;

        AsyncRun(LongConsumer writer) {
            this.writer = writer;
        }

        int state = 0; /* 0: Can run, 1: waiting for input, 2: halt */
        boolean run() {
            while (true) {
                switch (state) {
                    case 0:
                        state = fetchDecodeExecute(writer, Optional.empty());
                        break;
                    case 1: return false;
                    case 2: return true;
                }
            }
        }

        void continueWith(long value) {
            state = fetchDecodeExecute(writer, Optional.of(value));
        }

        private int fetchDecodeExecute(LongConsumer writer, Optional<Long> value) {
            int instruction = loadCurrentInstruction();
            OpCode opcode = OpCode.mapConstant(instruction % 100);
            AddressingMode firstMode = AddressingMode.mapConstant((instruction / 100) % 10);
            AddressingMode secondMode = AddressingMode.mapConstant((instruction / 1000) % 10);
            AddressingMode thirdMode = AddressingMode.mapConstant((instruction / 10000) % 10);
            int nextInstruction = cursor + opcode.size;

            switch (opcode) {
                case ADD:
                case MULTIPLY:
                case LESS_THAN:
                case EQUAL:
                    long op1 = load(firstMode, cursor + 1);
                    long op2 = load(secondMode, cursor + 2);
                    int dstAddress = (int) load(AddressingMode.IMMEDIATE, cursor + 3);

                    store(thirdMode, dstAddress, operatorMap(opcode).applyAsLong(op1, op2));
                    break;
                case INPUT:
                    if (value.isEmpty()) return 1; /* async return */
                    dstAddress = (int) load(AddressingMode.IMMEDIATE, cursor + 1);
                    store(firstMode, dstAddress, value.get());
                    break;
                case OUTPUT:
                    op1 = load(firstMode, cursor + 1);
                    writer.accept(op1);
                    break;
                case JUMP_IF_TRUE:
                case JUMP_IF_FALSE:
                    op1 = load(firstMode, cursor + 1);
                    op2 = load(secondMode, cursor + 2);

                    if (opcode == OpCode.JUMP_IF_TRUE && op1 != 0)
                        nextInstruction = (int) op2;
                    else if (opcode == OpCode.JUMP_IF_FALSE && op1 == 0)
                        nextInstruction = (int) op2;

                    break;
                case SET_RELATIVE_BASE:
                    op1 = load(firstMode, cursor + 1);
                    relativeBase = relativeBase + (int) op1;
                    break;
                case HALT:
                    return 2; /* halt */
            }
            cursor = nextInstruction;
            return 0;
        }
    }

    AsyncRun async(LongConsumer writer) {
        return new AsyncRun(writer);
    }

    void runSynchronously(LongSupplier reader, LongConsumer writer) {
        //noinspection StatementWithEmptyBody
        while (!fetchDecodeExecute(reader, writer)) ;
    }

    void runSynchronouslyWithSystemInOut() {
        Scanner scanner = new Scanner(System.in);
        runSynchronously(scanner::nextLong, System.out::println);
    }

    IntCodeMachine fork() {
        return new IntCodeMachine(this.memory);
    }
}
