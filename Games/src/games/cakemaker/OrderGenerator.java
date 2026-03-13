package games.cakemaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OrderGenerator {

    public static final String BASE_VANILLA = "Vanilla";
    public static final String BASE_CHOCOLATE = "Chocolate";
    public static final String BASE_STRAWBERRY = "Strawberry";

    public static final String CREAM_VANILLA = "Vanilla Cream";
    public static final String CREAM_CHOCOLATE = "Chocolate Cream";
    public static final String CREAM_STRAWBERRY = "Strawberry Cream";

    public static final String TOPPING_CHERRY = "Cherry";
    public static final String TOPPING_SPRINKLES = "Sprinkles";
    public static final String TOPPING_CHOC_CHIPS = "Chocolate Chips";
    public static final String TOPPING_STRAWBERRY = "Strawberries";

    private final Random random;
    private final List<Integer> toppingCountCycle;
    private int toppingCycleIndex;
    private int generatedOrderCount;

    public OrderGenerator() {
        this.random = new Random();
        this.toppingCountCycle = new ArrayList<>();
        this.toppingCycleIndex = 0;
        this.generatedOrderCount = 0;
    }

    public Cake generateOrder() {
        Cake order = new Cake();
        order.setBase(randomFrom(getBaseOptions()));
        order.setCream(randomFrom(getCreamOptions()));

        List<String> toppings = new ArrayList<>(getToppingOptions());
        Collections.shuffle(toppings, random);
        int toppingCount;
        if (generatedOrderCount == 0) {
            toppingCount = 0; // 2-step cake right away
        } else if (generatedOrderCount == 1) {
            toppingCount = 1; // 3-step cake right away
        } else {
            toppingCount = nextToppingCount();
        }
        generatedOrderCount++;

        for (int i = 0; i < toppingCount; i++) {
            order.addTopping(toppings.get(i));
        }

        return order;
    }

    public List<String> getBaseOptions() {
        return List.of(BASE_VANILLA, BASE_CHOCOLATE, BASE_STRAWBERRY);
    }

    public List<String> getCreamOptions() {
        return List.of(CREAM_VANILLA, CREAM_CHOCOLATE, CREAM_STRAWBERRY);
    }

    public List<String> getToppingOptions() {
        return List.of(TOPPING_CHERRY, TOPPING_SPRINKLES, TOPPING_CHOC_CHIPS, TOPPING_STRAWBERRY);
    }

    private String randomFrom(List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    private int nextToppingCount() {
        if (toppingCycleIndex >= toppingCountCycle.size()) {
            refillToppingCountCycle();
        }
        int count = toppingCountCycle.get(toppingCycleIndex);
        toppingCycleIndex++;
        return count;
    }

    private void refillToppingCountCycle() {
        toppingCountCycle.clear();
        toppingCountCycle.add(0);
        toppingCountCycle.add(1);
        toppingCountCycle.add(0);
        toppingCountCycle.add(1);
        toppingCountCycle.add(2);
        toppingCountCycle.add(3);
        Collections.shuffle(toppingCountCycle, random);
        toppingCycleIndex = 0;
    }
}
