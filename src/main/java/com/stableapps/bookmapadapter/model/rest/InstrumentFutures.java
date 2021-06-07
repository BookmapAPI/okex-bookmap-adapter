package com.stableapps.bookmapadapter.model.rest;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentFutures extends InstrumentGeneric {

    @JsonProperty("underlying_index")
    String underlyingIndex;
    @JsonProperty("contract_val")
    double contractVal;
    @JsonProperty("contract_val_currency")
    String contractValCurrency;
    String listing;
    String delivery;
    @JsonProperty("trade_increment")
    int tradeIncrement;
    @JsonProperty("alias")
    String delivery_week;
}
