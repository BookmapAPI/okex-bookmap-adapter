package com.stableapps.bookmapadapter.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("serial")
public class CustomSubscribeContractMarketDepthInitialResponseDeserializer extends StdDeserializer<SubscribeContractMarketDepthInitialResponse> {
    public CustomSubscribeContractMarketDepthInitialResponseDeserializer() { 
        this(null); 
    } 
 
    public CustomSubscribeContractMarketDepthInitialResponseDeserializer(Class<?> vc) { 
        super(vc); 
    }
 
    @Override
    public SubscribeContractMarketDepthInitialResponse deserialize(JsonParser jp, DeserializationContext ctxt) 
      throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        SubscribeContractMarketDepthInitialResponse response = new SubscribeContractMarketDepthInitialResponse();
        response.channel = node.get("channel").asText();
        response.data = new InitialResponse();
        String event = node.get("event").asText();
        response.data.result = event.equals("subscribe") ? true : false;
        response.data.channel = response.channel;
        return response;
    }
}
