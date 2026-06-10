package com.salonbook.app.utils;

public class Constants {

    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_SERVICES = "services";
    public static final String COLLECTION_STYLISTS = "stylists";
    public static final String COLLECTION_APPOINTMENTS = "appointments";
    public static final String COLLECTION_WALKINS = "walkins";

    // Appointment Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Roles
    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_OWNER = "Salon Owner";

    // Intent Extras
    public static final String EXTRA_SERVICE_ID = "serviceId";
    public static final String EXTRA_STYLIST_ID = "stylistId";
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    public static final String EXTRA_EDIT_MODE = "editMode";
    public static final String EXTRA_SERVICE_NAME = "serviceName";
    public static final String EXTRA_SERVICE_CATEGORY = "serviceCategory";
    public static final String EXTRA_SERVICE_DURATION = "serviceDuration";
    public static final String EXTRA_SERVICE_PRICE = "servicePrice";
    public static final String EXTRA_STYLIST_NAME = "stylistName";
    public static final String EXTRA_STYLIST_SPEC = "stylistSpec";
    public static final String EXTRA_STYLIST_EXP = "stylistExp";
    public static final String EXTRA_STYLIST_AVAILABLE = "stylistAvailable";
    public static final String EXTRA_STYLIST_PHOTO = "stylistPhoto";

    // Broadcast Actions
    public static final String ACTION_APPOINTMENT_STATUS_CHANGED = "com.salonbook.app.APPOINTMENT_STATUS_CHANGED";
    public static final String ACTION_APPOINTMENT_REMINDER = "com.salonbook.app.APPOINTMENT_REMINDER";

    // Notification
    public static final String NOTIFICATION_CHANNEL_ID = "salonbook_reminders";
    public static final int NOTIFICATION_ID = 1001;

    // Time Slots (9 AM to 7 PM, 1-hour intervals)
    public static final String[] TIME_SLOTS = {
            "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM",
            "5:00 PM", "6:00 PM", "7:00 PM"
    };

    // Service Categories
    public static final String[] CATEGORIES = {
            "Haircut", "Facial", "Massage", "Waxing",
            "Manicure", "Pedicure", "Bridal", "Other"
    };

    // Stylist Specializations
    public static final String[] SPECIALIZATIONS = {
            "Hair", "Skin", "Nails", "Bridal", "General"
    };

    // Languages
    public static final String[] LANGUAGE_CODES = {"en", "hi", "mr", "gu", "ta", "pa"};

    /**
     * Maps a service category to a stylist specialization.
     */
    public static String mapCategoryToSpecialization(String category) {
        if (category == null) return "General";
        switch (category) {
            case "Haircut": return "Hair";
            case "Facial":
            case "Massage":
            case "Waxing": return "Skin";
            case "Manicure":
            case "Pedicure": return "Nails";
            case "Bridal": return "Bridal";
            default: return "General";
        }
    }
}
