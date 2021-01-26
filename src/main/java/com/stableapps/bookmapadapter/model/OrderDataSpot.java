package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDataSpot extends OrderData {
    
    double notional;
    String side;
    
    @JsonProperty("filled_size")
    double filledSize;

    @JsonProperty("filled_notional")
    double filledNotional;

    @JsonProperty("margin_trading")
    int marginTrading;
    
}
