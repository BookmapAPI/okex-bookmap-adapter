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
public class OrderInfo {

	@JsonProperty("symbol")
	public String symbol;
	@JsonProperty("lever_rate")
	public Integer leverRate;
	@JsonProperty("amount")
	public Integer amount;
	@JsonProperty("fee")
	public Double fee;
	@JsonProperty("contract_name")
	public String contractName;
	@JsonProperty("unit_amount")
	public Integer unitAmount;
	@JsonProperty("type")
	public Integer type;
	@JsonProperty("price_avg")
	public Double priceAvg;
	@JsonProperty("deal_amount")
	public Integer dealAmount;
	@JsonProperty("price")
	public Double price;
	@JsonProperty("create_date")
	public Long createDate;
	@JsonProperty("order_id")
	public Long orderId;
	@JsonProperty("status")
	public Integer status;

}
