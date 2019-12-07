import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Day3 {
    public static void main(String[] args) throws IOException {
        System.out.println(Day3.Part1.answer());
        System.out.println(Day3.Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input3.txt");

    static List<FlatVector> pointsToVectors(List<Point> points) {
        return IntStream
                .range(0, points.size() - 1)
                .mapToObj(i -> FlatVector.newFlatVector(points.get(i), points.get(i + 1)))
                .collect(Collectors.toList());
    }

    static List<Point> parseTrace(String input) {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Point.ORIGIN);

        for (String part : input.split(",")) {
            char direction = part.charAt(0);
            int magnitude = Integer.parseInt(part.substring(1));
            var p1 = points.get(points.size() - 1);
            switch (direction) {
                case 'R':
                    points.add(new Point(p1.x + magnitude, p1.y));
                    break;
                case 'D':
                    points.add(new Point(p1.x, p1.y - magnitude));
                    break;
                case 'L':
                    points.add(new Point(p1.x - magnitude, p1.y));
                    break;
                case 'U':
                    points.add(new Point(p1.x, p1.y + magnitude));
                    break;
                default:
                    throw new RuntimeException("Unknown direction.");
            }
        }

        return points;
    }

    static class Point {

        static Point ORIGIN = new Point(0 ,0);

        private final int x;
        private final int y;

        Point(int x, int y){
            this.x = x;
            this.y = y;
        }

        int manhattanDistance(Point that) {
            IntUnaryOperator abs = i -> i >= 0 ? i : -i;
            return abs.applyAsInt(this.x - that.x) + abs.applyAsInt(this.y - that.y);
        }

        @Override
        public String toString() {
            return "Point{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    /* for lack of better name, Flat => Vertical or horizontal */
    static abstract class FlatVector {
        abstract Point head();
        abstract Point tail();
        abstract Optional<Point> intercepts(FlatVector that);
        /* extending that infinitely and checking if `this` intercept it */
        abstract Optional<Integer> infiniteIntercept(FlatVector that);
        abstract long length();
        abstract boolean pointInside(Point point);

        static FlatVector newFlatVector(Point a, Point b) {
            if (a.x == b.x) {
                return new VerticalVector(a, b);
            } else if (a.y == b.y) {
                return new HorizontalVector(a, b);
            } else {
                throw new IllegalArgumentException();
            }
        }

        private static class VerticalVector extends FlatVector{

            private final Point head;
            private final Point tail;

            /* a is always below b */
            private final Point a, b;

            VerticalVector(Point head, Point tail) {
                this.head = head;
                this.tail = tail;

                if (head.y < tail.y) {
                    a = head;
                    b = tail;
                } else {
                    a = tail;
                    b = head;
                }
            }

            @Override
            Point head() {
                return head;
            }

            @Override
            Point tail() {
                return tail;
            }

            @Override
            Optional<Point> intercepts(FlatVector that) {
                return this.infiniteIntercept(that)
                        .flatMap(y -> that.infiniteIntercept(this)
                                .map(x -> new Point(x, y)));

            }

            @Override
            Optional<Integer> infiniteIntercept(FlatVector that) {
                if (that instanceof VerticalVector) return Optional.empty();
                else {
                    int y = that.head().y;
                    return (a.y < y && b.y > y) ? Optional.of(y) : Optional.empty();
                }
            }

            @Override
            long length() {
                return b.y - a.y;
            }

            @Override
            boolean pointInside(Point point) {
                int x = a.x;
                return x == point.x && a.y <= point.y && b.y >= point.y;
            }
        }

        private static class HorizontalVector extends FlatVector {
            private final Point head;
            private final Point tail;

            /* a is always to the left of b */
            private final Point a, b;

            public HorizontalVector(Point head, Point tail) {
                this.head = head;
                this.tail = tail;

                if (head.x < tail.x) {
                    a = head;
                    b = tail;
                } else {
                    a = tail;
                    b = head;
                }
            }

            @Override
            Point head() {
                return head;
            }

            @Override
            Point tail() {
                return tail;
            }

            @Override
            Optional<Point> intercepts(FlatVector that) {
                return this.infiniteIntercept(that)
                        .flatMap(x -> that.infiniteIntercept(this)
                                .map(y -> new Point(x, y)));
            }

            @Override
            Optional<Integer> infiniteIntercept(FlatVector that) {
                if (that instanceof HorizontalVector) return Optional.empty();
                else {
                    int x = that.head().x;
                    return (a.x < x && b.x > x) ? Optional.of(x) : Optional.empty();
                }
            }

            @Override
            long length() {
                return b.x - a.x;
            }

            @Override
            boolean pointInside(Point point) {
                int y = a.y;
                return y == point.y && a.x <= point.x && b.x >= point.x;
            }
        }
    }

    static class Part1 {
        static long answer() throws IOException {
            String[] traces = Files.readString(INPUT_PATH).split("\\n");
            List<FlatVector> cable1 = pointsToVectors(parseTrace(traces[0]));
            List<FlatVector> cable2 = pointsToVectors(parseTrace(traces[1]));

            return cable1.stream()
                    .flatMap(a -> cable2.stream()
                            .flatMap(b -> a.intercepts(b).stream()))
                    .mapToLong(p -> p.manhattanDistance(Point.ORIGIN))
                    .min()
                    .getAsLong();
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            String[] traces = Files.readString(INPUT_PATH).split("\\n");
            List<FlatVector> cable1 = pointsToVectors(parseTrace(traces[0]));
            List<FlatVector> cable2 = pointsToVectors(parseTrace(traces[1]));

            return cable1.stream()
                    .flatMap(a -> cable2.stream()
                            .flatMap(b -> a.intercepts(b).stream()))
                    .mapToLong(p -> lengthToPoint(cable1, p) + lengthToPoint(cable2, p))
                    .min()
                    .getAsLong();
        }

        static long lengthToPoint(List<FlatVector> vectors, Point point) {
            long length = 0;

            for (FlatVector vector : vectors) {
                if (vector.pointInside(point)) {
                    length += FlatVector.newFlatVector(vector.head(), point).length();
                    break;
                } else {
                    length += vector.length();
                }
            }

            return length;
        }
    }
}
