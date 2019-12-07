import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Day1 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input1.txt");

    static class Part1 {

        static long answer() throws IOException {

            return Files
                    .readAllLines(INPUT_PATH)
                    .stream()
                    .mapToLong(Long::parseLong)
                    .map(Part1::calculateFuel)
                    .sum();
        }

        static long calculateFuel(long mass) {
            if (mass < 9) return 0;
            long rounded = (long) (mass / 3.0);
            return rounded - 2;
        }
    }

    static class Part2 {
        static long answer() throws IOException {

            return Files
                    .readAllLines(INPUT_PATH)
                    .stream()
                    .mapToLong(Long::parseLong)
                    .map(Part2::calculateFuel)
                    .sum();
        }

        static long calculateFuel(long mass) {
            if (mass < 9) return 0;
            long rounded = (long) (mass / 3.0);
            long fuelRequired = rounded - 2;
            return fuelRequired + calculateFuel(fuelRequired);
        }
    }
}
