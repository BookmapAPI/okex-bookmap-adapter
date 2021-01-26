package com.stableapps.bookmapadapter.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesPositionsOfCurrencyResponse {

    boolean result;
    
    @JsonProperty("margin_mode")
    String marginMode;
    
    ArrayList<FuturesPosition> holding;
}
