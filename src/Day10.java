import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Day10 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input10.txt");

    static class Part1 {
        static long answer() throws IOException {
            Set<Point> points = readAndParse();
            assert points.size() > 1;
            return points.stream().mapToInt(p -> countInsight(points, p)).max().getAsInt();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            Set<Point> points = readAndParse();
            assert points.size() > 200;

            Point center = points.stream().max(Comparator.comparingInt(p -> countInsight(points, p))).get();
            int vaporizedCount  = 0;
            while (vaporizedCount <= 200) {
                List<Point> shotMe = evaporate(points, center);
                if (shotMe.size() + vaporizedCount > 200) {
                        Point bet = shotMe.get(200 - vaporizedCount - 1);
                        return bet.x * 100 + bet.y;

                } else {
                    vaporizedCount += shotMe.size();
                    points.removeAll(shotMe);
                }
            }

            throw new RuntimeException("Ops");
        }

        /* The angel of the given point from the y-axis centered at center */
        static double angel(Point center, Point point) {
            double theta = Math.atan2(point.x - center.x, center.y - point.y);
            return theta < 0 ? theta + 2 * Math.PI : theta;
        }

        static List<Point> evaporate(Set<Point> points, Point center) {
            return pointsInsight(points, center)
                    .sorted(Comparator.comparingDouble(p -> angel(center, p)))
                    .collect(Collectors.toList());
        }
    }

    static class Point {
        private final int x;
        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x &&
                    y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Point{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    static Set<Point> readAndParse() throws IOException {
        String[] lines = Files.readAllLines(INPUT_PATH).toArray(String[]::new);
        HashSet<Point> points = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < lines[i].length(); j++) {
                char c = lines[i].charAt(j);
                if (c == '#') points.add(new Point(j, i));
            }
        }
        return points;
    }

    static int gcd(int x, int y) {
        return y == 0 ? x : gcd(y, x % y);
    }

    static List<Point> integerPointsInBetween(Point a, Point b) {
        if (a.x > b.x || a.x == b.x && a.y > b.y) return integerPointsInBetween(b, a);

        int i = b.x - a.x;
        int j = b.y - a.y;
        int gcd = Math.abs(gcd(i, j));
        i /= gcd;
        j /= gcd;

        ArrayList<Point> points = new ArrayList<>();
        for (int x = a.x + i, y = a.y + j; x < b.x || (i == 0 && y < b.y); x += i, y += j) {
            points.add(new Point(x, y));
        }
        return points;
    }

    static Stream<Point> pointsInsight(Collection<Point> points, Point point) {
        return points
                .stream()
                .filter(Predicate.not(point::equals))
                .filter(p -> integerPointsInBetween(point, p)
                        .stream()
                        .allMatch(Predicate.not(points::contains)));
    }

    static int countInsight(Collection<Point> points, Point point) {
        return (int) pointsInsight(points, point).count();
    }
}

