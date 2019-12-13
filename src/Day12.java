import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Day12 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input12.txt");
    static Pattern pattern = Pattern.compile("<x=(-?[0-9]+), y=(-?[0-9]+), z=(-?[0-9]+)>");

    static class Part1 {
        static long answer() throws IOException {
            IntVector3[] positions = readAndParse();

            return simulate(Arrays.asList(positions));
        }

        static long simulate(List<IntVector3> positions) {
            List<IntVector3> velocities = positions.stream().map(ignore -> IntVector3.ZERO).collect(Collectors.toList());
            return simulate(1000, positions, velocities);
        }

        static long simulate(int steps, List<IntVector3> positions, List<IntVector3> velocities) {
            if (steps == 0) return zipApply(positions, velocities, Part1::totalEnergy).stream().mapToInt(i -> i).sum();

            List<IntVector3> gravities = positions.stream().map(v -> applyGravity(positions, v)).collect(Collectors.toList());
            var newVelocities = zipApply(velocities, gravities, IntVector3::add);
            var newPositions = zipApply(positions, newVelocities, IntVector3::add);
            return simulate(steps - 1, newPositions, newVelocities);
        }

        static <A, B, T> List<T> zipApply(List<A> as, List<B> bs, BiFunction<A, B, T> function) {
            assert as.size() == bs.size();
            ArrayList<T> ts = new ArrayList<>(as.size());
            for (int i = 0; i < as.size(); i++) {
                A a = as.get(i);
                B b = bs.get(i);
                T t = function.apply(a, b);
                ts.add(t);
            }
            return ts;
        }

        static int totalEnergy(IntVector3 position, IntVector3 velocity) {
            int potential = Math.abs(position.x) + Math.abs(position.y) + Math.abs(position.z);
            int kinetic = Math.abs(velocity.x) + Math.abs(velocity.y) + Math.abs(velocity.z);
            return potential * kinetic;
        }
    }

    static class Part2 {

        static long answer() throws IOException {
            var positions = readAndParse();

            return simulate(positions);
        }

        static long gcd(long x, long y) {
            //noinspection SuspiciousNameCombination
            return y == 0 ? x : gcd(y, x % y);
        }

        static long lcm(long x, long y) {
            return (x * y) / gcd(x, y);
        }

        static long simulate(IntVector3[] positions) {
            int length = positions.length;

            var xs = Arrays.stream(positions).mapToInt(v -> v.x).toArray();
            var ys = Arrays.stream(positions).mapToInt(v -> v.y).toArray();
            var zs = Arrays.stream(positions).mapToInt(v -> v.z).toArray();

            var velocities = new IntVector3[length];
            Arrays.fill(velocities, IntVector3.ZERO);

            var gravities = new IntVector3[length];

            OptionalInt x = OptionalInt.empty();
            OptionalInt y = OptionalInt.empty();
            OptionalInt z = OptionalInt.empty();

            for (int i = 1; x.isEmpty() || y.isEmpty() || z.isEmpty(); i++) {
                Arrays.fill(gravities, IntVector3.ZERO);

                for (int j = 0; j < length; j++) {
                    gravities[j] = applyGravity(positions, positions[j]);
                }

                for (int j = 0; j < length; j++) {
                    velocities[j] = velocities[j].add(gravities[j]);
                }

                for (int j = 0; j < length; j++) {
                    positions[j] = positions[j].add(velocities[j]);
                }

                if (x.isEmpty() &&
                        Arrays.stream(velocities).allMatch(v -> v.x == 0) &&
                        Arrays.equals(xs, Arrays.stream(positions).mapToInt(v -> v.x).toArray())) {

                    x = OptionalInt.of(i);
                }

                if (y.isEmpty() &&
                        Arrays.stream(velocities).allMatch(v -> v.y == 0) &&
                        Arrays.equals(ys, Arrays.stream(positions).mapToInt(v -> v.y).toArray())) {

                    y = OptionalInt.of(i);
                }

                if (z.isEmpty() &&
                        Arrays.stream(velocities).allMatch(v -> v.z == 0) &&
                        Arrays.equals(zs, Arrays.stream(positions).mapToInt(v -> v.z).toArray())) {

                    z = OptionalInt.of(i);
                }
            }

            return lcm(lcm(x.getAsInt(), y.getAsInt()), z.getAsInt());
        }

    }

    private static IntVector3[] readAndParse() throws IOException {
        return Files.readAllLines(INPUT_PATH)
                .stream()
                .map(line -> {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) throw new RuntimeException("match error");
                    int x = Integer.parseInt(matcher.group(1));
                    int y = Integer.parseInt(matcher.group(2));
                    int z = Integer.parseInt(matcher.group(3));
                    return new IntVector3(x, y, z);
                })
                .toArray(IntVector3[]::new);
    }

    static class IntVector3 {
        final int x;
        final int y;
        final int z;

        IntVector3(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static IntVector3 ZERO = new IntVector3(0, 0, 0);

        @Override
        public String toString() {
            return "IntVector3(" + x + ", " + y + ", " + z + ')';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntVector3 that = (IntVector3) o;
            return x == that.x &&
                    y == that.y &&
                    z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        IntVector3 add(IntVector3 that) {
            return new IntVector3(this.x + that.x, this.y + that.y, this.z + that.z);
        }
    }

    static IntVector3 applyGravity(IntVector3 center, IntVector3 vector3) {
        Comparator<Integer> gravity = Comparator.reverseOrder();
        int x = gravity.compare(vector3.x, center.x);
        int y = gravity.compare(vector3.y, center.y);
        int z = gravity.compare(vector3.z, center.z);
        return new IntVector3(x, y, z);
    }

    static IntVector3 applyGravity(List<IntVector3> centers, IntVector3 vector3) {
        return centers.stream()
                .map(center -> applyGravity(center, vector3))
                .reduce(IntVector3.ZERO, IntVector3::add);
    }

    static IntVector3 applyGravity(IntVector3[] centers, IntVector3 vector3) {
        return Arrays.stream(centers)
                .map(center -> applyGravity(center, vector3))
                .reduce(IntVector3.ZERO, IntVector3::add);
    }
}
