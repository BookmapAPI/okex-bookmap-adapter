package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = PlaceOrderRequestTokenOrMarginSerializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class PlaceOrderRequestTokenOrMargin extends PlaceOrderRequest {

	String side;
    @JsonProperty("instrument_id")
	String instrumentId;
    @JsonProperty("order_type")
	String duration;
    @JsonProperty("margin_trading")
	int marginTrading;

    double price;
    int notional;
}
