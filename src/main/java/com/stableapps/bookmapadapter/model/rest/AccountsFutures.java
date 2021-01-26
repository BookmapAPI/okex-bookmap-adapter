package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountsFutures {

    @JsonProperty("margin_mode")
    String marginMode;
    
    @JsonProperty("total_avail_balance")
    double totalAvailBalance;

    double equity;
}
