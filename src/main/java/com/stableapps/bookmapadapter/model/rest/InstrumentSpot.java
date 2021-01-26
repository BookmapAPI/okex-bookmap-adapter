package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstrumentSpot extends InstrumentGeneric {

    @JsonProperty("base_currency")
    String baseCurrency;
    @JsonProperty("min_size")
    double minSize;
    @JsonProperty("size_increment")
    double sizeIncrement;
}
