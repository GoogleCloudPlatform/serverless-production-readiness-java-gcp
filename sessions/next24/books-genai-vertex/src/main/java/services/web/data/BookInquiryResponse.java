package services.web.data;

import java.util.List;

public record BookInquiryResponse(String book, String author, List<String> topics, String analysis) {
}
