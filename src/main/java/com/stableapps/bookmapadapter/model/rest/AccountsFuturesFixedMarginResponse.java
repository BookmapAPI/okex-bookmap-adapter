package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountsFuturesFixedMarginResponse extends AccountsFutures {
    
    AccountsFuturesFixedMarginContract[] contracts;
    
    @JsonProperty("auto_margin")
    double autoMargin;
    
    public double getRealizedPnl() {
        double pnl = 0.0;
        
        for (AccountsFuturesFixedMarginContract contract : contracts) {
            pnl += contract.getRealizedPnl();
        }
        return pnl;
    }
    
    public double getUnrealizedPnl() {
        double pnl = 0.0;
        
        for (AccountsFuturesFixedMarginContract contract : contracts) {
            pnl += contract.getUnrealizedPnl();
        }
        return pnl;
    }
}
