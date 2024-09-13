In which category does the book {title} by {author} fit in best?
What is the name of the main character?
Is the main character portrayed in one of the following ways: positive, neutral, ambiguous or negative?
Recommend other books with similar characters.
Return book category, main character name, main character sentiment and book recommendations strictly in a valid JSON format

Answer in a format as in the example following the column :
"{\n" +
        "  \"book_category\": \"Historical Fiction\",\n" +
        "  \"main_character_name\": \"Friedrich Nietzsche\",\n" +
        "  \"main_character_sentiment\": \"Ambiguous\",\n" +
        "  \"book_recommendations\": [\n" +
        "    \"The Master and Margarita by Mikhail Bulgakov\",\n" +
        "    \"The Name of the Rose by Umberto Eco\",\n" +
        "    \"The Remains of the Day by Kazuo Ishiguro\"\n" +
        "  ]\n" +
        "}"