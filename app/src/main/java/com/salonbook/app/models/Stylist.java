package com.salonbook.app.models;

import java.util.HashMap;
import java.util.Map;

public class Stylist {
    private String id;
    private String name;
    private String specialization;
    private int experience;
    private float rating;
    private boolean available;
    private String photoPath;

    public Stylist() {}

    public Stylist(String name, String specialization, int experience, boolean available) {
        this.name = name;
        this.specialization = specialization;
        this.experience = experience;
        this.rating = 4.0f;
        this.available = available;
        this.photoPath = "";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("specialization", specialization);
        map.put("experience", experience);
        map.put("rating", rating);
        map.put("available", available);
        map.put("photoPath", photoPath != null ? photoPath : "");
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
