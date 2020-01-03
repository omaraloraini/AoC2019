import java.util.Objects;

public class Tuple<F, S, T> {
    private final F first;
    private final S second;
    private final T third;

    static <T1, T2, T3> Tuple<T1, T2, T3> of(T1 first, T2 second, T3 third) {
        return new Tuple<>(first, second, third);
    }

    private Tuple(F first, S second, T third) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F first() {
        return first;
    }

    public S second() {
        return second;
    }
    public T third() {
        return third;
    }

    public Pair<F, S> firstPair() {
        return Pair.of(first, second);
    }

    public Pair<S, T> secondPair() {
        return Pair.of(second, third);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?, ?> tuple = (Tuple<?, ?, ?>) o;
        return first.equals(tuple.first) &&
                second.equals(tuple.second) &&
                third.equals(tuple.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }

    @Override
    public String toString() {
        return "Tuple(" + first + ", " + second + ", " + third + ')';
    }
}
