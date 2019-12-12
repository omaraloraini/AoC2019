import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Day8 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day8.Part1.answer());
        System.out.println(Day8.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input8.txt");

    private static class Part1 {
        static final int LAYER_SIZE = 25 * 6;

        public static long answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            int[] digits = new int[input.length()];
            for (int i = 0; i < input.length(); i++) {
                digits[i] = Character.digit(input.charAt(i), 10);
            }


            @SuppressWarnings("OptionalGetWithoutIsPresent")
            int[] layer = IntStream
                    .range(0, input.length() / LAYER_SIZE)
                    .mapToObj(i -> Arrays.stream(digits).skip(i * LAYER_SIZE).limit(LAYER_SIZE).toArray())
                    .min(Comparator.comparing(array -> countEqual(array, 0)))
                    .get();

            return countEqual(layer, 1) * countEqual(layer, 2);
        }

        static int countEqual(int[] array, int i) {
            int count = 0;
            for (int value : array) {
                if (value == i) count++;
            }
            return count;
        }
    }

    private static class Part2 {
        static final int WIDTH = 25;
        static final int HEIGHT = 6;
        static final int LAYER_SIZE = WIDTH * HEIGHT;

        static final String WHITE = "\u25A1";
        static final String BLACK = "\u25A0";

        public static String answer() throws IOException {
            String input = Files.readString(INPUT_PATH);
            int[] digits = new int[input.length()];
            for (int i = 0; i < input.length(); i++) {
                digits[i] = Character.digit(input.charAt(i), 10);
            }

            return IntStream
                    .range(0, input.length() / LAYER_SIZE)
                    .mapToObj(i -> Arrays.stream(digits).skip(i * LAYER_SIZE).limit(LAYER_SIZE).toArray())
                    .reduce((first, second) -> {
                        for (int i = 0; i < first.length; i++) {
                            if (first[i] == 2) first[i] = second[i];
                        }
                        return first;
                    })
                    .map(array -> Arrays.stream(array).mapToObj(i -> i == 0 ? BLACK : WHITE))
                    .map(stream -> stream.collect(Collectors.joining("")))
                    .map(s -> s.replaceAll("(.{25})", "$1\n"))
                    .get();
        }
    }
}