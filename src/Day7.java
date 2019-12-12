import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

@SuppressWarnings("ALL")
public class Day7 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input7.txt");

    static <T> Set<List<T>> permutation(List<T> choices) {
        return permutation(Collections.emptyList(), choices);
    }

    static <T> Set<List<T>> permutation(List<T> prefix, List<T> suffix) {
        Set<List<T>> ps = new HashSet<>();

        if (suffix.size() == 0) {
            ps.add(prefix);
            return ps;
        }

        for (T choice : suffix) {
            List<T> newSuffix = new java.util.ArrayList<>(suffix);
            List<T> newPrefix = new java.util.ArrayList<>(prefix);

            newSuffix.remove(choice);
            newPrefix.add(choice);

            ps.addAll(permutation(newPrefix, newSuffix));
        }

        return ps;
    }

    private static int[] readProgram() throws IOException {
        return Arrays.stream(Files
                .readString(INPUT_PATH)
                .split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    static class Part1 {
        static long answer() throws IOException {
            int[] source = readProgram();

            return permutation(List.of(0, 1, 2, 3, 4))
                    .stream()
                    .mapToInt(phases -> {
                        int signal = 0;
                        for (Integer phase : phases) {
                            signal = copyAndRun(source, supplyTwo(phase, signal));
                        }
                        return signal;
                    })
                    .max()
                    .getAsInt();
        }

        /* Copies source and run it. Returns the last output of the machine or 0 */
        static int copyAndRun(int[] source, IntSupplier read) {
            int[] memory = Arrays.copyOf(source, source.length);
            int[] out = new int[1];

            Day5.runIntCode(memory, read, w -> out[0] = w);
            return out[0];
        }

        static IntSupplier supplyTwo(int first, int second) {
            return new IntSupplier() {
                boolean isSecond = false;
                public int getAsInt() {
                    if (isSecond) {
                        return second;
                    } else {
                        isSecond = true;
                        return first;
                    }
                }
            };
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            int[] source = readProgram();

            return permutation(List.of(5, 6, 7, 8, 9))
                    .stream()
                    .mapToLong(phases -> runWithPhase(source, phases))
                    .max()
                    .getAsLong();
        }

        static long runWithPhase(int[] source, List<Integer> phases) {
            assert phases.size() == 5;

            AmplifierSequence amplifierSequence = new AmplifierSequence();
            amplifierSequence.register(new Amplifier(Arrays.copyOf(source, source.length), amplifierSequence, List.of(phases.get(1))));
            amplifierSequence.register(new Amplifier(Arrays.copyOf(source, source.length), amplifierSequence, List.of(phases.get(0))));
            amplifierSequence.register(new Amplifier(Arrays.copyOf(source, source.length), amplifierSequence, List.of(phases.get(2))));
            amplifierSequence.register(new Amplifier(Arrays.copyOf(source, source.length), amplifierSequence, List.of(phases.get(3))));
            amplifierSequence.register(new Amplifier(Arrays.copyOf(source, source.length), amplifierSequence, List.of(phases.get(4), 0)));

            return amplifierSequence.start();
        }

        static class AsynchronousIntCodeMachine {
            private final int[] memory;
            private final IntConsumer outputBuffer;
            private int cursor;
            private State state;

            enum State {
                INITIALIZED, RUNNING, WAITING_FOR_INPUT, HALTED
            }

            AsynchronousIntCodeMachine(int[] memory, IntConsumer outputBuffer) {
                this.memory = memory;
                this.outputBuffer = outputBuffer;
                cursor = 0;
                state = State.INITIALIZED;
            }

            private int load(Day5.AddressingMode mode, int address) {
                return mode == Day5.AddressingMode.POSITION
                        ? memory[memory[address]]
                        : memory[address];
            }


            private IntBinaryOperator operatorMap(Day5.OpCode opCode) {
                switch (opCode) {
                    case ADD: return Integer::sum;
                    case MULTIPLY: return (i, j) -> i * j;
                    case LESS_THAN: return (i, j) -> i < j ? 1 : 0;
                    case EQUAL: return (i, j) -> i == j ? 1 : 0;
                    default: throw new IllegalArgumentException(
                            "operatorMap maps two parameters opcodes, not the case for: " + opCode);
                }
            }

            private void fetchDecodeExecute() {
                int instruction = memory[cursor];
                Day5.OpCode opcode = Day5.OpCode.mapConstant(instruction % 100);
                Day5.AddressingMode firstMode = Day5.AddressingMode.mapConstant((instruction / 100) % 10);
                Day5.AddressingMode secondMode = Day5.AddressingMode.mapConstant((instruction / 1000) % 10);
                int nextInstruction = cursor + opcode.size;

                switch (opcode) {
                    case ADD:
                    case MULTIPLY:
                    case LESS_THAN:
                    case EQUAL:
                        int op1 = load(firstMode, cursor + 1);
                        int op2 = load(secondMode, cursor + 2);
                        int dstAddress = load(Day5.AddressingMode.IMMEDIATE, cursor + 3);

                        memory[dstAddress] = operatorMap(opcode).applyAsInt(op1, op2);
                        break;
                    case INPUT:
                        this.state = State.WAITING_FOR_INPUT;
                        return; /* Don't change the cursor*/
                    case OUTPUT:
                        op1 = load(Day5.AddressingMode.POSITION, cursor + 1);
                        outputBuffer.accept(op1);
                        break;
                    case JUMP_IF_TRUE:
                    case JUMP_IF_FALSE:
                        op1 = load(firstMode, cursor + 1);
                        op2 = load(secondMode, cursor + 2);

                        if (opcode == Day5.OpCode.JUMP_IF_TRUE && op1 != 0)
                            nextInstruction = op2;
                        else if (opcode == Day5.OpCode.JUMP_IF_FALSE && op1 == 0)
                            nextInstruction = op2;

                        break;
                    case HALT:
                        this.state = State.HALTED;
                        break;
                }

                cursor = nextInstruction;
            }

            private void run() {
                while (this.state == State.RUNNING) {
                    fetchDecodeExecute();
                }
            }

            void start() {
                if (this.state == State.INITIALIZED) {
                    this.state = State.RUNNING;
                    run();
                } else {
                    throw new IllegalStateException("Machine has already started");
                }
            }

            void continueWith(int readValue) {
                if (this.state == State.WAITING_FOR_INPUT) {
                    int dstAddress = load(Day5.AddressingMode.IMMEDIATE, cursor + 1);
                    memory[dstAddress] = readValue;
                    cursor += Day5.OpCode.INPUT.size;
                    this.state = State.RUNNING;

                    run();
                } else {
                    throw new IllegalStateException("Machine was not waiting for input");
                }
            }

            boolean hasNotStarted() { return state == State.INITIALIZED; }
            boolean hasHalted() { return state == State.HALTED; }
        }

        /* Circular sequence of amplifiers */
        static class AmplifierSequence {
            List<Amplifier> amplifiers = new ArrayList<>();

            Amplifier getPrevious(Amplifier a) {
                int index = amplifiers.indexOf(a);
                if (index >= 0) {
                    int prevIndex = index == 0 ? amplifiers.size() - 1 : index - 1;
                    return amplifiers.get(prevIndex);
                } else {
                    throw new IllegalStateException("The supplied Amplifier is not registered with the sequence");
                }
            }

            void register(Amplifier a) {
                amplifiers.add(a);
            }

            /* Start the pipeline and returns the last output of the last amplifier */
            int start() {
                Amplifier last = amplifiers.get(amplifiers.size() - 1);
                return last.start();
            }
        }

        static class Amplifier implements IntSupplier, IntConsumer {
            private final AsynchronousIntCodeMachine machine;
            private final AmplifierSequence sequence;
            private Queue<Integer> output;

            Amplifier(int[] memory, AmplifierSequence sequence, List<Integer> initialOutput){
                machine = new AsynchronousIntCodeMachine(memory, this);
                this.sequence = sequence;
                output = new LinkedList<>(initialOutput);
            }

            @Override
            public int getAsInt() {
                while (output.isEmpty()) {
                    if (machine.hasNotStarted()) {
                        machine.start();
                    } else if (machine.hasHalted()) {
                        throw new RuntimeException("No output is available");
                    } else {
                        int input = sequence.getPrevious(this).getAsInt();
                        machine.continueWith(input);
                    }
                }
                return output.poll();
            }

            @Override
            public void accept(int value) {
                output.offer(value);
            }

            int start() {
                while (!machine.hasHalted()) {
                    if (machine.hasNotStarted()) {
                        machine.start();
                    } else {
                        int input = sequence.getPrevious(this).getAsInt();
                        machine.continueWith(input);
                    }
                }

                Integer signal = output.poll();
                if (signal == null) throw new RuntimeException("Last amplifier has not output");
                return signal;
            }
        }
    }
}
