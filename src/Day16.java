import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Day16 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input16.txt");

    static class Part1 {
        static String answer() throws IOException {
            String input = Files.readString(INPUT_PATH).trim();
            int[] signal = digits(input);

            return Arrays.stream(nthPhase(signal, 100))
                    .limit(8)
                    .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                    .toString();
        }

        static int[] nthPhase(int[] signal, int n) {
            if (n == 0) return signal;
            return nthPhase(nextPhase(signal), n - 1);
        }

        /* 0 1 0 -1 */
        static int coefficient(int i, int r) {
            int cycle = 4 * r;
            i++;
            int j = (i % cycle) / r;
            switch (j) {
                case 1:
                    return 1;
                case 3:
                    return -1;
                default:
                    return 0;
            }
        }

        static int[] nextPhase(int[] vector) {
            int n = vector.length;
            int[] result = new int[n];

            for (int i = 0; i < n; i++) {
                int sum = 0;
                for (int j = 0; j < n; j++) {
                    int c = coefficient(j, i + 1);
                    sum += c == 0
                            ? 0
                            : c == 1 ? vector[j] : -vector[j];
                }
                result[i] = Math.abs(sum) % 10;
            }

            return result;
        }
    }

    static class Part2 {
        static String answer() throws IOException {
            String input = Files.readString(INPUT_PATH).trim();

            int[] signal = digits(input);
            int messageOffset = Arrays.stream(signal).limit(7).reduce(0, (acc, i) -> acc * 10 + i);
            int n = signal.length * 10000 - messageOffset;

            int[] out = new int[n];
            for (int i = out.length - 1, j = signal.length - 1; i >= 0; i--, j--) {
                if (j < 0) j = signal.length - 1;
                out[i] = signal[j];
            }

            for (int i = 0; i < 100; i++) {
                for (int j = out.length - 2; j >= 0; j--) {
                    out[j] = (out[j] + out[j + 1]) % 10;
                }
            }

            return Arrays.stream(out).limit(8).mapToObj(Integer::toString).collect(Collectors.joining(""));
        }
    }

    static int[] digits(String number) {
        int[] ints = new int[number.length()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = Character.digit(number.charAt(i), 10);
        }

        return ints;
    }
}
