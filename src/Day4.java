import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Day4 {
    public static void main(String[] args) {
        System.out.println(Day4.Part1.answer());
        System.out.println(Day4.Part2.answer());
    }

    static final int START = 108457;
    static final int END = 562041;

    /* a non increasing number from right to left */
    static boolean nonIncreasing(int i) {
        int last = 9;

        for (int current = i % 10; current <= last && i > 0; i /= 10, current = i % 10) {
            last = current;
        }

        return i == 0;
    }

    static List<Pair<Integer, Integer>> groupAdjacent(int i) {
        List<Pair<Integer, Integer>> list = new ArrayList<>();

        int last = i % 10;
        int c = 1;

        while (i > 0) {
            i /= 10;
            int d = i % 10;
            if (last == d) {
                c++;
            } else {
                list.add(Pair.of(last, c));
                c = 1;
            }
            last = d;
        }

        return list;
    }

    static class Part1 {
        static long answer() {
            return IntStream.range(START, END).filter(Part1::match).count();
        }

        static boolean match(int password) {
            return nonIncreasing(password) &&
                    groupAdjacent(password).stream().anyMatch(p -> p.second() >= 2);
        }
    }

    static class Part2 {
        static long answer() {
            return IntStream.range(START, END).filter(Part2::match).count();
        }

        static boolean match(int password) {
            return nonIncreasing(password) &&
                    groupAdjacent(password).stream().anyMatch(p -> p.second() == 2);
        }
    }
}
