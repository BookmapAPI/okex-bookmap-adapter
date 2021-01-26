package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountsFuturesCrossMargin extends AccountsFutures{

    double margin;

    @JsonProperty("realized_pnl")
    double realizedPnl;
    
    @JsonProperty("unrealized_pnl")
    double unrealizedPnl;
    
    @JsonProperty("margin_ratio")
    double marginRatio;
}
