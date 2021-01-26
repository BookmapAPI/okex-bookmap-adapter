/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stableapps.bookmapadapter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 *
 * @author aris
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitialResponse {

	public boolean result;
	public String channel;

}
