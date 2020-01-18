import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Day22 {

    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input22.txt");

    static class Part1 {
        static long answer() throws IOException {
            List<String> lines = Files.readAllLines(INPUT_PATH);
            int deckSize = 10007;
            BigInteger value = BigInteger.valueOf(2019);
            LinearCongruentialFunction shuffle = shuffle(deckSize, lines);
            return shuffle.apply(2019).longValue();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            List<String> lines = Files.readAllLines(INPUT_PATH);
            long deckSize  = 119315717514047L;
            long shuffles = 101741582076661L;
            BigInteger position = BigInteger.valueOf(2020);

            LinearCongruentialFunction f = shuffle(deckSize, lines);

            BigInteger m = f.m;
            BigInteger A = f.a.modPow(BigInteger.valueOf(shuffles), m);
            BigInteger B = f.b.multiply(BigInteger.ONE.subtract(A)).multiply(BigInteger.ONE.subtract(f.a).modInverse(m));
            return position.subtract(B).multiply(A.modInverse(m)).mod(m).longValue();
        }
    }

    static LinearCongruentialFunction shuffle(long deckSize, List<String> input) {
        return input.stream().map(line -> {
            if (line.equals("deal into new stack")) return dealNewStackFunction(deckSize);
            else {
                String[] parts = line.split(" ");

                if (parts[0].equals("cut")) {
                    long n = Integer.parseInt(parts[1]);
                    return cutNFunction(n, deckSize);
                } else {
                    long n = Integer.parseInt(parts[3]);
                    return dealWithIncrementFunction(n, deckSize);
                }

            }
        }).reduce(LinearCongruentialFunction::andThen).orElseThrow();
    }

    static class LinearCongruentialFunction { /* an + b (mod m) */
        final BigInteger a;
        final BigInteger b;
        final BigInteger m;

        LinearCongruentialFunction(BigInteger a, BigInteger b, BigInteger m) {
            this.a = a;
            this.b = b;
            this.m = m;
        }

        LinearCongruentialFunction(long a, long b, long m) {
            this.a = BigInteger.valueOf(a);
            this.b = BigInteger.valueOf(b);
            this.m = BigInteger.valueOf(m);
        }

        BigInteger apply(long n) { return apply(BigInteger.valueOf(n)); }
        BigInteger apply(BigInteger n) {
            return a.multiply(n).add(b).mod(m);
        }

        /* f.compose(g) =:= g(f(x)) */
        LinearCongruentialFunction compose(LinearCongruentialFunction g) {
            if (!m.equals(g.m)) throw new IllegalArgumentException();

            /* f(x) = ax + b
            *  g(x) = cx + d
            *  f(g(x)) = a(cx + d) + b = acx + ad + b = (ac)x + (ad + b) (mod m)
            */
            BigInteger a = this.a;
            BigInteger b = this.b;
            BigInteger c = g.a;
            BigInteger d = g.b;

            return new LinearCongruentialFunction(
                    a.multiply(c).mod(m),
                    a.multiply(d).add(b).mod(m),
                    m);
        }

        /* g(f(x) */
        LinearCongruentialFunction andThen(LinearCongruentialFunction g) {
            return g.compose(this);
        }

        @Override
        public String toString() {
            return "ModuloArithmeticFunction( " +a+ " n + " +b+ " (mod " +m+ ") )";
        }
    }

    static LinearCongruentialFunction dealWithIncrementFunction(long n, long deckSize) {
        return new LinearCongruentialFunction(n, 0, deckSize);
    }

    static LinearCongruentialFunction dealNewStackFunction(long deckSize) {
        return new LinearCongruentialFunction(-1, -1, deckSize);
    }

    static LinearCongruentialFunction cutNFunction(long n, long deckSize) {
        return new LinearCongruentialFunction(1, -n, deckSize);
    }
}
