package services.utility;

public class CloudUtility {
    public static String extractRegion(String regionString) {
        if (regionString == null) {
            return "";  // Handle null input
        }

        String[] parts = regionString.split("-");
        if (parts.length >= 2) {
            return parts[0] + "-" + parts[1]; // Join the first two parts
        } else {
            return "";   // Invalid format if no dash is found
        }
    }
}
