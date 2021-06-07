package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentGeneric {

    @JsonProperty("instrument_id")
    String instrumentId;
    @JsonProperty("quote_currency")
    String quoteCurrency;
    @JsonProperty("base_currency")
    String baseCurrency;
    @JsonProperty("tick_size")
    double tickSize;
}
