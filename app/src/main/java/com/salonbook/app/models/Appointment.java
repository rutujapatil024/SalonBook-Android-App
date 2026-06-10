package com.salonbook.app.models;

import java.util.HashMap;
import java.util.Map;

public class Appointment {
    private String id;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String stylistId;
    private String serviceId;
    private String serviceName;
    private String stylistName;
    private String date;
    private String timeSlot;
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    public Appointment() {}

    public Appointment(String customerId, String customerName, String customerPhone,
                       String stylistId, String serviceId, String serviceName,
                       String stylistName, String date, String timeSlot) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.stylistId = stylistId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.stylistName = stylistName;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = "PENDING";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("customerPhone", customerPhone);
        map.put("stylistId", stylistId);
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("stylistName", stylistName);
        map.put("date", date);
        map.put("timeSlot", timeSlot);
        map.put("status", status);
        return map;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getStylistId() { return stylistId; }
    public void setStylistId(String stylistId) { this.stylistId = stylistId; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getStylistName() { return stylistName; }
    public void setStylistName(String stylistName) { this.stylistName = stylistName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
