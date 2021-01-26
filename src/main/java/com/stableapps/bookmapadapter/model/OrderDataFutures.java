package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDataFutures extends OrderData {

    @JsonProperty("filled_qty")
    double filledQty;

    double fee;
    
    @JsonProperty("price_avg")
    double priceAvg;

    @JsonProperty("contract_val")
    int contractVal;
    
    int leverage;

    String status;
}
