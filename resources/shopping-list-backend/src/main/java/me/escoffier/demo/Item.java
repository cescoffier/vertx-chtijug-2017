package me.escoffier.demo;

public class Item {

    private String name;

    private int quantity = 1;

    public Item() {
        // Because of Jackson
    }

    public Item(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Item setName(String name) {
        this.name = name;
        return this;
    }

    public int getQuantity() {
        return quantity;
    }

    public Item setQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }
}
