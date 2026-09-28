package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RcaResult(

        String rootCause,

        double confidence,

        String impact,

        List<Object> evidence,

                @JsonDeserialize(contentUsing = RecommendationDeserializer.class)
                List<String> recommendations

) {

        public static class RecommendationDeserializer extends JsonDeserializer<String> {

                @Override
                public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                        JsonNode node = parser.getCodec().readTree(parser);
                        if (node.isTextual()) {
                                return node.asText();
                        }

                        List<String> parts = new ArrayList<>();
                        collectText(node, parts);
                        return String.join("; ", parts);
                }

                private static void collectText(JsonNode node, List<String> parts) {
                        if (node.isContainerNode()) {
                                Iterator<JsonNode> children = node.elements();
                                while (children.hasNext()) {
                                        collectText(children.next(), parts);
                                }
                        } else if (!node.isNull()) {
                                parts.add(node.asText());
                        }
                }
        }
}