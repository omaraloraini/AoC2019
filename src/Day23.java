import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class Day23 {
    private static final Path INPUT_PATH = Path.of("input23.txt");

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static class Part1 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            HashMap<Long, Nic> addressNicMap = new HashMap<>();
            long[] out = new long[1];

            Network network = (address, x, y) -> {
                if (address == 255) out[0] = y;
                else addressNicMap.get(address).receive(x, y);
            };

            for (long i = 0; i < 50; i++) addressNicMap.put(i, new Nic(network, machine.fork(), i));

            while (out[0] == 0) {
                for (Nic nic : addressNicMap.values()) {
                    nic.runAsync();
                }
            }

            return out[0];
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            IntCodeMachine machine = IntCodeMachine.fromFile(INPUT_PATH);
            HashMap<Long, Nic> addressNicMap = new HashMap<>();
            long[] out = new long[2];

            Network network = (address, x, y) -> {
                if (address == 255) {
                    out[0] = x;
                    out[1] = y;
                }
                else addressNicMap.get(address).receive(x, y);
            };

            for (long i = 0; i < 50; i++) addressNicMap.put(i, new Nic(network, machine.fork(), i));

            ArrayList<Pair<Long, Long>> sentByNat = new ArrayList<>();
            while (true) {
                for (Nic nic : addressNicMap.values()) {
                    nic.runAsync();
                }

                if (addressNicMap.values().stream().allMatch(Nic::isWaiting)) {
                    network.send(0, out[0], out[1]);
                    Pair<Long, Long> packet = Pair.of(out[0], out[1]);

                    if (sentByNat.size() > 0 && sentByNat.get(sentByNat.size() - 1).equals(packet)) {
                        break;
                    } else {
                        sentByNat.add(packet);
                    }
                }
            }

            return out[1];
        }
    }

    @FunctionalInterface
    interface Network {
        void send(long address, long x, long y);
    }

    static class Nic implements LongConsumer, LongSupplier /* Network Interface Controller */ {

        private final Network network;
        private final IntCodeMachine.AsyncRun machine;
        private final long address;
        private final Queue<Pair<Long, Long>> messages = new LinkedList<>();


        Nic(Network network, IntCodeMachine machine, long address) {
            this.network = network;
            this.machine = machine.async(this);
            this.address = address;
        }


        boolean halted = false;

        boolean isHalted() { return halted; }
        boolean isWaiting() {
            return messages.isEmpty() && !halted;
        }

        void runAsync() {
            while (true) {
                halted = machine.run();
                long probe1 = getAsLong();
                machine.continueWith(probe1);
                if (probe1 == -1) {
                    halted = machine.run();
                    break;
                }
            }
        }

        void receive(long x, long y) {
            messages.offer(Pair.of(x, y));
        }

        enum OutputState {ADDRESS, X, Y}

        OutputState outputState = OutputState.ADDRESS;
        long outputAddress;
        long outputX;

        @Override
        public void accept(long value) {
            switch (outputState) {
                case ADDRESS:
                    outputAddress = value;
                    outputState = OutputState.X;
                    break;
                case X:
                    outputX = value;
                    outputState = OutputState.Y;
                    break;
                case Y:
                    network.send(outputAddress, outputX, value);
                    outputState = OutputState.ADDRESS;
                    break;
            }
        }

        enum InputState {ADDRESS_INITIALIZE, X, Y}

        InputState inputState = InputState.ADDRESS_INITIALIZE;
        Pair<Long, Long> inputMessage;

        @Override
        public long getAsLong() {
            switch (inputState) {
                case ADDRESS_INITIALIZE:
                    inputState = InputState.X;
                    return address;
                case X:
                    if (messages.isEmpty()) return -1;
                    inputMessage = messages.poll();
                    inputState = InputState.Y;
                    return inputMessage.first();
                case Y:
                    inputState = InputState.X;
                    return inputMessage.second();
                default:
                    throw new Error();
            }
        }
    }
}
