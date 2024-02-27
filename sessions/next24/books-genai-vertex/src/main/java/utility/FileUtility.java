package utility;

public class FileUtility {

    public static String getTitle (String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[0];
    }

    public static String getAuthor(String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[1];
    }

    public static String getYear(String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[2]+"-01"+"-01";
    }

    public static String getPublicPrivate(String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[3];
    }

}
