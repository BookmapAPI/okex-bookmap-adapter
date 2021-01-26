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
public class CustomLoginResponseDeserializer extends StdDeserializer<LoginResponse> {
    public CustomLoginResponseDeserializer() { 
        this(null); 
    } 
 
    public CustomLoginResponseDeserializer(Class<?> vc) { 
        super(vc); 
    }
 
    @Override
    public LoginResponse deserialize(JsonParser jp, DeserializationContext ctxt) 
      throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        LoginResponse response = new LoginResponse();
        response.channel = node.get("event").asText();
        response.data = new LoginData();
        
        if (response.channel.equals("error")) {
            response.message = node.get("message").asText();
            response.errorCode = node.get("errorCode").asInt();
        } else {
            response.data.result = node.get("success").asBoolean();
        }
        return response;
    }
}
