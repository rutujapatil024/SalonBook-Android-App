package com.salonbook.app.models;

import java.util.HashMap;
import java.util.Map;

public class WalkIn {
    private String id;
    private String customerName;
    private String phone;
    private String serviceId;
    private String serviceName;
    private String stylistId;
    private String stylistName;
    private String date;
    private String timeSlot;

    public WalkIn() {}

    public WalkIn(String customerName, String phone, String serviceId, String serviceName,
                  String stylistId, String stylistName, String date, String timeSlot) {
        this.customerName = customerName;
        this.phone = phone;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.stylistId = stylistId;
        this.stylistName = stylistName;
        this.date = date;
        this.timeSlot = timeSlot;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("customerName", customerName);
        map.put("phone", phone);
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("stylistId", stylistId);
        map.put("stylistName", stylistName);
        map.put("date", date);
        map.put("timeSlot", timeSlot);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStylistId() { return stylistId; }
    public void setStylistId(String stylistId) { this.stylistId = stylistId; }

    public String getStylistName() { return stylistName; }
    public void setStylistName(String stylistName) { this.stylistName = stylistName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
}
