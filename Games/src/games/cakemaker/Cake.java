package games.cakemaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Cake {

    private String base;
    private String cream;
    private final List<String> toppings;

    public Cake() {
        this.toppings = new ArrayList<>();
    }

    public Cake(Cake other) {
        this();
        if (other == null) {
            return;
        }

        this.base = other.base;
        this.cream = other.cream;
        this.toppings.addAll(other.toppings);
    }

    public void clear() {
        base = null;
        cream = null;
        toppings.clear();
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setCream(String cream) {
        this.cream = cream;
    }

    public void addTopping(String topping) {
        if (topping == null || topping.isBlank()) {
            return;
        }
        if (!toppings.contains(topping)) {
            toppings.add(topping);
        }
    }

    public String getBase() {
        return base;
    }

    public String getCream() {
        return cream;
    }

    public List<String> getToppings() {
        return Collections.unmodifiableList(toppings);
    }

    public int getTotalLayers() {
        int count = 0;
        if (base != null) {
            count++;
        }
        if (cream != null) {
            count++;
        }
        count += toppings.size();
        return count;
    }

    public boolean matches(Cake other) {
        if (other == null) {
            return false;
        }

        if (!Objects.equals(base, other.base)) {
            return false;
        }
        if (!Objects.equals(cream, other.cream)) {
            return false;
        }

        if (toppings.size() != other.toppings.size()) {
            return false;
        }

        Set<String> thisSet = new HashSet<>(toppings);
        Set<String> otherSet = new HashSet<>(other.toppings);
        return thisSet.equals(otherSet);
    }

    public Cake copy() {
        return new Cake(this);
    }
}
