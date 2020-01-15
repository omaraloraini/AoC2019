import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class Day22 {

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
    }

    static class Part1 {
        static long answer() throws IOException {
            List<String> lines = Files.readAllLines(INPUT_PATH);
            int deckSize = 10007;
            int value = 2019;
            long[] shuffle = shuffle(deckSize, lines);

            int i = 0;
            while (i < shuffle.length) {
                if (shuffle[i] == value) break;
                i++;
            }
            return i;
        }
    }


    static long[] shuffle(int deckSize, List<String> input) {

        LongUnaryOperator inverse = /* maps final position to starting position */
                reverse(parseInput(input, deckSize))
                .stream()
                .reduce(LongUnaryOperator::andThen)
                .orElseThrow();

        return LongStream.range(0, deckSize).map(inverse).toArray();
    }

    private static <T> List<T> reverse(List<T> list) {
        ArrayList<T> reversed = new ArrayList<>(list.size());
        for (int i = list.size() - 1; i >= 0; i--) {
            T t = list.get(i);
            reversed.add(t);
        }
        return reversed;
    }

    static List<LongUnaryOperator> parseInput(List<String> list, long deckSize) {
        return list.stream().map(line -> {
            if (line.equals("deal into new stack")) return dealNewStack(deckSize);
            else {
                String[] parts = line.split(" ");

                if (parts[0].equals("cut")) {
                    long n = Integer.parseInt(parts[1]);
                    return cutN(n, deckSize);
                }
                long n = Integer.parseInt(parts[3]);
                return dealWithIncrement(n, deckSize);
            }
        }).collect(Collectors.toList());
    }

    static final Path INPUT_PATH = Path.of(".", "input22.txt");

    static long[] extendedEuclideanAlgorithm(long a, long b) {
        if (b > a) {
            long[] result = extendedEuclideanAlgorithm(b, a);
            long temp = result[1];
            result[1] = result[2];
            result[2] = temp;
            return result;
        }

        long r0, r1, rn;
        long s0, s1, sn;
        long t0, t1, tn;

        r0 = a;
        r1 = b;
        s0 = 1;
        s1 = 0;
        t0 = 0;
        t1 = 1;

        long q;
        while (r1 != 0) {
            q = r0 / r1;
            rn = r0 - q * r1;
            sn = s0 - q * s1;
            tn = t0 - q * t1;

            r0 = r1;
            s0 = s1;
            t0 = t1;
            r1 = rn;
            s1 = sn;
            t1 = tn;
        }

        return s0 == 0
                ? new long[]{r0, s1, t1 + 1}
                : new long[]{r0, s0, t0};
    }

    static LongUnaryOperator dealWithIncrement(long n, long deckSize) {
        long[] cs = extendedEuclideanAlgorithm(n, deckSize);
        return i -> Math.floorMod(i * cs[1], deckSize);
    }

    static LongUnaryOperator dealNewStack(long deckSize) {
        return i -> deckSize - i - 1;
    }

    static LongUnaryOperator cutN(long n, long deckSize) {
        return i -> Math.floorMod(i + n, deckSize);
    }
}
