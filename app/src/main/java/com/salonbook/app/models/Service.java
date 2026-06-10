package com.salonbook.app.models;

import java.util.HashMap;
import java.util.Map;

public class Service {
    private String id;
    private String name;
    private String category;
    private int duration;
    private double price;

    public Service() {}

    public Service(String name, String category, int duration, double price) {
        this.name = name;
        this.category = category;
        this.duration = duration;
        this.price = price;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", category);
        map.put("duration", duration);
        map.put("price", price);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
