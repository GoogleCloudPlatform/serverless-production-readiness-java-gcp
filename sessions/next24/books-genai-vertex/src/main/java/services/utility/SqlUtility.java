package services.utility;

public class SqlUtility {


    public static String replaceUnderscoresWithSpaces(String inputString) {
        return inputString == null ? "" : inputString.trim().replace("_", " ");
    }
}
