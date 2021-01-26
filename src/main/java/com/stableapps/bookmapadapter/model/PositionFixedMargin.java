/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 *
 * @author aris
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionFixedMargin {

	@JsonProperty("lever_rate")
	public int leverRate;
	@JsonProperty("avgprice")
	public double avgPrice;
	@JsonProperty("contract_id")
	public long contractId;
	@JsonProperty("hold_amount")
	public double holdAmount;
	@JsonProperty("contract_name")
	public String contractName;
	@JsonProperty("costprice")
	public double costPrice;
	@JsonProperty("forcedprice")
	public double forcedPrice;
	@JsonProperty("bondfreez")
	public double bondFreeze;
	@JsonProperty("eveningup")
	public double eveningUp;
	@JsonProperty("fixmargin")
	public double fixMargin;
	public double balance;
	public int position;
	@JsonProperty("profitreal")
	public double profitReal;
	@JsonProperty("position_id")
	public long positionId;

}
