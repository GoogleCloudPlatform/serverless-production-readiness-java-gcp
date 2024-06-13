package services;
import org.junit.jupiter.api.Test;

import services.utility.FileUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilityTest {

    @Test
    public void testGetTitle() {
        String fileName = "book-author-year-public.txt";
        String expectedTitle = "book";
        String actualTitle = FileUtility.getTitle(fileName);
        assertEquals(expectedTitle, actualTitle);
    }

    @Test
    public void testGetAuthor() {
        String fileName = "book-author-year-public.txt";
        String expectedAuthor = "author";
        String actualAuthor = FileUtility.getAuthor(fileName);
        assertEquals(expectedAuthor, actualAuthor);
    }

    @Test
    public void testGetYear() {
        String fileName = "book-author-year-public.txt";
        String expectedYear = "year-01-01";
        String actualYear = FileUtility.getYear(fileName);
        assertEquals(expectedYear, actualYear);
    }

    @Test
    public void testGetPublicPrivate() {
        String fileName = "book-author-year-public.txt";
        String expectedPublicPrivate = "public";
        String actualPublicPrivate = FileUtility.getPublicPrivate(fileName);
        assertEquals(expectedPublicPrivate, actualPublicPrivate);
    }

    @Test
    public void testGetPublicPrivateWithoutExtension() {
        String fileName = "book-author-year-public";
        String expectedPublicPrivate = "public";
        String actualPublicPrivate = FileUtility.getPublicPrivate(fileName);
        assertEquals(expectedPublicPrivate, actualPublicPrivate);
    }

    @Test
    public void testGetPublicPrivateWithPDFExtension() {
        String fileName = "book-author-year-public.pdf";
        String expectedPublicPrivate = "public";
        String actualPublicPrivate = FileUtility.getPublicPrivate(fileName);
        assertEquals(expectedPublicPrivate, actualPublicPrivate);
    }

    @Test
    public void testGetPublicPrivateWithDOCXExtension() {
        String fileName = "book-author-year-public.DOCX";
        String expectedPublicPrivate = "public";
        String actualPublicPrivate = FileUtility.getPublicPrivate(fileName);
        assertEquals(expectedPublicPrivate, actualPublicPrivate);
    }
}

