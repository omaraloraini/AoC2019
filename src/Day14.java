import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;

public class Day14 {
    public static void main(String[] args) throws IOException {
        System.out.println(Part1.answer());
        System.out.println(Part2.answer());
    }

    static final Path INPUT_PATH = Path.of(".", "input14.txt");

    static class Part1 {

        static long answer() throws IOException {
            List<Reaction> reactions = Files.readAllLines(INPUT_PATH).stream().map(Reaction::parse).collect(Collectors.toList());

            Map<String, Long> map = Map.of("ORE", Long.MAX_VALUE);
            Map<String, Long> result = tryProduceFuel(reactions, map, 1);
            assert result.containsKey("FUEL");

            return Long.MAX_VALUE - result.get("ORE");
        }
    }

    static class Part2 {
        static long answer() throws IOException {
            List<Reaction> reactions = Files.readAllLines(INPUT_PATH).stream().map(Reaction::parse).collect(Collectors.toList());

            long trillion = 1000000000000L;
            Map<String, Long> map = new HashMap<>();
            map.put("ORE", trillion);
            long totalFuel = 0;

            /*
            * Try to produce one million fuel, if it's not possible, try half the amount,
            * keep retrying until we hit zero, implying we don't have enough ore to produce _any_ fuel.
            * In that case, we try to turn the excess elements back to ore.
            * If we can't, exit loop.
            * */

            do { // Loop until no more fuel can be produced AND no more ore can be produced;
                long amount = 10000000;

                while (amount >= 1) {
                    map = tryProduceFuel(reactions, map, amount);
                    if (map.containsKey("FUEL")) {
                        totalFuel += map.get("FUEL");
                        map.remove("FUEL");
                    } else {
                        amount = amount / 2;
                    }
                }

            } while (tryProduceOre(reactions, map));

            return totalFuel;
        }
    }

    static boolean containsEnough(Map<String, Long> map, String element, long amount) {
        return map.containsKey(element) && map.get(element) >= amount;
    }


    static boolean containsEnough(Map<String, Long> all, Map<String, Long> part) {
        return part.entrySet().stream().allMatch(e -> containsEnough(all, e.getKey(), e.getValue()));
    }

    /* Applies the reaction, consuming reactants from map, and putting the product into it */
    static void applyReaction(Reaction reaction, Map<String, Long> map) {
        if (containsEnough(map, reaction.reactants)) {
            reaction.reactants.forEach((r,c) -> map.computeIfPresent(r, (k,v) -> v - c));
            map.computeIfPresent(reaction.product, (k,v) -> v + reaction.productCoefficient);
            map.putIfAbsent(reaction.product, reaction.productCoefficient);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /* Tries to produce targetAmount of fuel, returns a new map if it had, otherwise the original input */
    static Map<String, Long> tryProduceFuel(List<Reaction> reactions, Map<String, Long> available, long targetAmount) {

        HashMap<String, Long> map = new HashMap<>(available);
        Stack<Pair<String, Long>> stack = new Stack<>();
        stack.push(Pair.of("FUEL", targetAmount));

        Function<String, Long> getQuantity = key -> map.getOrDefault(key, 0L);

        LongBinaryOperator divCeil = (l1, l2) -> (long) Math.ceil((double) l1 / l2);

        while (!stack.isEmpty()) {
            var pair = stack.pop();
            String element = pair.first();
            Long quantityRequired = pair.second();

            if (containsEnough(map, element, quantityRequired)) continue;

            Optional<Reaction> optionalReaction = reactions.stream()
                    .filter(r -> r.product.equals(element))
                    .map(r -> {
                        long factor = divCeil.applyAsLong(quantityRequired - getQuantity.apply(element), r.productCoefficient);
                        return r.multiply(factor);
                    })
                    .findFirst();

            if (optionalReaction.isEmpty()) return available;

            Reaction reaction = optionalReaction.get();

            if (containsEnough(map, reaction.reactants)) {
                applyReaction(reaction, map);
            }
            else {
                stack.push(pair);
                for (var reactant : reaction.reactants.entrySet()) {
                    stack.push(Pair.of(reactant.getKey(), reactant.getValue()));
                }
            }
        }

        return map;
    }

    /* Applies reactions in reverse trying to produce ore, returns true if at least one ore is produced */
    /* Mutates _map_ */
    static boolean tryProduceOre(List<Reaction> reactions, Map<String, Long> map) {
        boolean atLeastOne = false;
        while (reactions.stream()
                .filter(reaction -> containsEnough(map, reaction.product, reaction.productCoefficient))
                .map(reaction -> reaction.multiply(map.get(reaction.product) / reaction.productCoefficient))
                .peek(reaction -> applyReaction(reaction, map))
                .mapToInt(ignore -> 1)
                .sum() > 0) {

            atLeastOne = true;
        }
        return atLeastOne;
    }

    static class Reaction {
        final Map<String, Long> reactants;
        final String product;
        final long productCoefficient;

        Reaction(Map<String, Long> reactants, String product, long productCoefficient) {
            this.reactants = reactants;
            this.product = product;
            this.productCoefficient = productCoefficient;
        }

        Reaction multiply(long by) {
            HashMap<String, Long> reactants = new HashMap<>(this.reactants);
            reactants.replaceAll((k,v) -> v * by);
            return new Reaction(reactants, this.product, this.productCoefficient * by);
        }

        static Reaction parse(String input) {
            String[] split = input.split("=>");
            assert split.length == 2;
            Map<String, Long> reactants = Arrays.stream(split[0].split(","))
                    .map(String::trim)
                    .map(s -> s.split(" "))
                    .collect(Collectors.toMap(s -> s[1], s -> Long.parseLong(s[0])));

            String[] product = split[1].trim().split(" ");
            return new Reaction(reactants, product[1], Long.parseLong(product[0]));
        }

        @Override
        public String toString() {
            return reactants.toString() + " => " + productCoefficient + " " + product;
        }
    }
}
