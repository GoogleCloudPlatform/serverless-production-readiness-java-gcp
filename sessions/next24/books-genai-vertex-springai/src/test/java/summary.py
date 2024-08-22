def extract_summary_from_text(text: str) -> str:
    #https://cloud.google.com/vertex-ai/docs/generative-ai/learn/models
    #https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/text-embeddings#text-embedding-python_vertex_ai_sdk
    model = TextGenerationModel.from_pretrained("text-bison")
    rolling_prompt_template = get_prompt_for_summary_1()
    final_prompt_template = get_prompt_for_summary_2()

    if not rolling_prompt_template or not final_prompt_template:
        return ""  # return empty summary for empty prompts
    #context is the rolling prompt.
    context = ""
    #sumaries has every iteration.
    summaries = ""
    for page in pages(text, 16000):
        prompt = rolling_prompt_template.format(text=page,context=context)  # TODO set placeholder values in format
        context = model.predict(prompt, max_output_tokens=256).text
        summaries += f"\n{context}"

    prompt = final_prompt_template.format(text=summaries)  # TODO set placeholder values in format
    return model.predict(prompt, max_output_tokens=256).text


def get_prompt_for_summary_1() -> str:
    # TODO provide the prompt, you can use {} references for substitution
    # See https://www.w3schools.com/python/ref_string_format.asp
    #context is the rolling prompt.
    return """
        Taking the following context delimited by double backquotes into consideration:

        ``{context}``

        Write a concise summary of the following text delimited by double backquotes.

        ``{text}``

        CONCISE SUMMARY:
    """

def get_prompt_for_summary_2() -> str:
    # TODO provide the prompt, you can use {} references for substitution
    # See https://www.w3schools.com/python/ref_string_format.asp

    return """
        Please give me a summary with an introduction, three once sentence bullet points, and a conclusion from the following text delimited by double backquotes.

        ``Text:{text}``

        SUMMARY:
    """
# output from gemini prompt
# import com.google.cloud.aiplatform.v1.TextGenerationModel;
# import com.google.cloud.aiplatform.v1.TextGenerationModel.TextGenerationModelName;
# import com.google.cloud.aiplatform.v1.TextGenerationPredictResponse;
# import com.google.cloud.aiplatform.v1.TextSegment;
# import java.util.ArrayList;
# import java.util.List;
#
# public class Summary {
#
#   public static String extractSummaryFromText(String text) {
#     TextGenerationModel model = TextGenerationModel.fromName(TextGenerationModelName.of("us-central1", "text-bison-001").toString());
#     String rollingPromptTemplate = getPromptForSummary1();
#     String finalPromptTemplate = getPromptForSummary2();
#
#     if (rollingPromptTemplate == null || finalPromptTemplate == null) {
#       return ""; // return empty summary for empty prompts
#     }
#
#     // context is the rolling prompt.
#     String context = "";
#     // summaries has every iteration.
#     List<String> summaries = new ArrayList<>();
#     for (String page : pages(text, 16000)) {
#       String prompt = rollingPromptTemplate.format(text, context); // TODO set placeholder values in format
#       TextGenerationPredictResponse response = model.predict(prompt, 256);
#       context = response.getGeneratedText();
#       summaries.add(context);
#     }
#
#     String prompt = finalPromptTemplate.format(text, String.join("\n", summaries)); // TODO set placeholder values in format
#     return model.predict(prompt, 256).getGeneratedText();
#   }
#
#   public static String getPromptForSummary1() {
#     // TODO provide the prompt, you can use {} references for substitution
#     // See https://www.w3schools.com/python/ref_string_format.asp
#     // context is the rolling prompt.
#     return """
#         Taking the following context delimited by double backquotes into consideration:
#
#         ``{context}``
#
#         Write a concise summary of the following text delimited by double backquotes.
#
#         ``{text}``
#
#         CONCISE SUMMARY:
#     """;
#   }
#
#   public static String getPromptForSummary2() {
#     // TODO provide the prompt, you can use {} references for substitution
#     // See https://www.w3schools.com/python/ref_string_format.asp
#
#     return """
#         Please give me a summary with an introduction, three once sentence bullet points, and a conclusion from the following text delimited by double backquotes.
#
#         ``Text:{text}``
#
#         SUMMARY:
#     """;
#   }
#
#   public static List<String> pages(String text, int pageSize) {
#     List<String> pages = new ArrayList<>();
#     int start = 0;
#     while (start < text.length()) {
#       int end = Math.min(start + pageSize, text.length());
#       pages.add(text.substring(start, end));
#       start = end;
#     }
#     return pages;
#   }
# }
