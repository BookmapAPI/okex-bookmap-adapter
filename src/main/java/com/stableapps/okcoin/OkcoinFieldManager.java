package com.stableapps.okcoin;

import java.awt.Component;
import java.awt.Container;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import com.stableapps.bookmapadapter.util.Constants;

import velox.api.layer0.credentialscomponents.CredentialsCheckbox;
import velox.api.layer0.credentialscomponents.CredentialsComponent;
import velox.api.layer0.credentialscomponents.CredentialsFieldManager;
import velox.api.layer0.credentialscomponents.CredentialsSerializationField;

public class OkcoinFieldManager implements CredentialsFieldManager {

    public static class CustomCheckbox extends CredentialsCheckbox {

        public boolean previousTradingCheckboxValue;

        public CustomCheckbox(String name, boolean isKey, String label) {
            super(name, isKey, label);
        }
    }

    CustomCheckbox tradingCheckbox = new CustomCheckbox(Constants.ENABLE_TRADING_CHECKBOX_NAME, true,
            Constants.ENABLE_TRADING_CHECKBOX_LABEL);

    List<CredentialsComponent> components = new LinkedList<>();

    {
        Container compound = tradingCheckbox.getCompoundComponent();
        Component[] elements = compound.getComponents();
        
        for (Component element : elements) {
            if (element instanceof JCheckBox) {
                ((JCheckBox)element).setEnabled(false);
            }
        }
        components.add(tradingCheckbox);
    }

    @Override
    public List<CredentialsComponent> getCredentialsComponents() {
        return components;
    }

    @Override
    public boolean isConfigured(Map<String, CredentialsSerializationField> arg0) {
        return true;
    }

}
