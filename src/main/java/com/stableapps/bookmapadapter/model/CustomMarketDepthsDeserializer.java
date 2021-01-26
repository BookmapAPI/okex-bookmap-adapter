package com.stableapps.bookmapadapter.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("serial")
public class CustomMarketDepthsDeserializer extends StdDeserializer<MarketDepths> {
    public CustomMarketDepthsDeserializer() { 
        this(null); 
    } 
 
    public CustomMarketDepthsDeserializer(Class<?> vc) { 
        super(vc); 
    }
 
    @Override
    public MarketDepths deserialize(JsonParser jp, DeserializationContext ctxt) 
      throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode node1 = node.get(0);
        ArrayNode asks = (ArrayNode)node1.get("asks");
        
        List<MarketDepth> askDatas = new ArrayList<>();;
        MarketDepth askData;
        
        for (JsonNode nod : asks) {
            Double price = nod.get(0).asDouble();
            Double size = nod.get(1).asDouble();
            
            askData = MarketDepth.builder()
                    .price(price)
                    .contractAmount(size)
//                  .coinAmount(ask.get(2))
//                  .coinCumulant(ask.get(3))
//                  .contractCumulant(ask.get(4))
                    .coinAmount(0)
                    .coinCumulant(0)
                    .contractCumulant(0)
                    .build();
                  askDatas.add(askData);
        }
        

        ArrayNode bids = (ArrayNode)node1.get("bids");
        
        List<MarketDepth> bidDatas = new ArrayList<>();;
        MarketDepth bidData;
        
        for (JsonNode nod : bids) {
            Double price = nod.get(0).asDouble();
            Double size = nod.get(1).asDouble();
            
            bidData = MarketDepth.builder()
                    .price(price)
                    .contractAmount(size)
//                  .coinAmount(ask.get(2))
//                  .coinCumulant(ask.get(3))
//                  .contractCumulant(ask.get(4))
                    .coinAmount(0)
                    .coinCumulant(0)
                    .contractCumulant(0)
                    .build();
                  bidDatas.add(bidData);
        }
        
        Collections.sort(askDatas, (a, b) -> a.getPrice() < b.getPrice() ? 1 : a.getPrice() == b.getPrice() ? 0 : -1);
        Collections.sort(bidDatas, (a, b) -> a.getPrice() < b.getPrice() ? -1 : a.getPrice() == b.getPrice() ? 0 : 1);
        
        MarketDepths depth = new MarketDepths();
        depth.instrument_id = node1.get("instrument_id").asText();
        depth.askDatas = askDatas;
        depth.bidDatas = bidDatas;
 
        return depth;
    }
}
