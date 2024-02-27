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
// write a unit test to test the FileUtility class
    public static String getPublicPrivate(String fileName) {
        String [] fileNameArray = fileName.split("-");
        String publicPrivate = fileNameArray[3];
        if(publicPrivate.contains(".txt")){
            publicPrivate = publicPrivate.replaceAll("\\.txt$", "");
        }
        return publicPrivate;
    }

}
