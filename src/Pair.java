import java.util.Objects;

public class Pair<L, R>  {
    private final L first;
    private final R second;

    static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
        return new Pair<>(first, second);
    }

    private Pair(L first, R second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        this.first = first;
        this.second = second;
    }

    public L first() {
        return first;
    }

    public R second() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return first.equals(pair.first) &&
                second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Pair(" + first + ", " + second + ')';
    }
}
