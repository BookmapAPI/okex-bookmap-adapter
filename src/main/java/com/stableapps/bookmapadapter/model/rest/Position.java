package com.stableapps.bookmapadapter.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Position {

	@JsonProperty("buy_price_avg")
	public Double buyPriceAvg;
	@JsonProperty("symbol")
	public String symbol;
	@JsonProperty("lever_rate")
	public Integer leverRate;
	@JsonProperty("buy_available")
	public Integer buyAvailable;
	@JsonProperty("contract_id")
	public Long contractId;
	@JsonProperty("sell_risk_rate")
	public String sellRiskRate;
	@JsonProperty("buy_amount")
	public Integer buyAmount;
	@JsonProperty("buy_risk_rate")
	public String buyRiskRate;
	@JsonProperty("contract_type")
	public String contractType;
	@JsonProperty("sell_flatprice")
	public String sellFlatprice;
	@JsonProperty("buy_bond")
	public Double buyBond;
	@JsonProperty("sell_profit_lossratio")
	public String sellProfitLossratio;
	@JsonProperty("buy_flatprice")
	public String buyFlatprice;
	@JsonProperty("buy_profit_lossratio")
	public String buyProfitLossratio;
	@JsonProperty("sell_amount")
	public Integer sellAmount;
	@JsonProperty("sell_bond")
	public Double sellBond;
	@JsonProperty("sell_price_cost")
	public Double sellPriceCost;
	@JsonProperty("buy_price_cost")
	public Double buyPriceCost;
	@JsonProperty("create_date")
	public Long createDate;
	@JsonProperty("sell_price_avg")
	public Double sellPriceAvg;
	@JsonProperty("sell_available")
	public Integer sellAvailable;

}
