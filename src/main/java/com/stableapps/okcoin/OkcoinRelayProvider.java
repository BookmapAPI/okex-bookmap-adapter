package com.stableapps.okcoin;

import com.stableapps.bookmapadapter.provider.RelayProvider;
import com.stableapps.bookmapadapter.util.Constants;

import velox.api.layer0.annotations.Layer0CredentialsFieldsManager;
import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.ExtendedLoginData;
import velox.api.layer1.data.LoginData;


@Layer0LiveModule(fullName = OkcoinConstants.ADAPTER_FULL_NAME, shortName = OkcoinConstants.ADAPTER_SHORT_NAME)
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0CredentialsFieldsManager(OkcoinFieldManager.class)
public class OkcoinRelayProvider extends RelayProvider {

    public OkcoinRelayProvider() {
        super(OkcoinConstants.EXCHANGE, OkcoinConstants.WS_PORT_NUMBER, OkcoinConstants.WS_LINK);
        Log.info("Relay " + this.hashCode());
    }

    @Override
    public void login(LoginData loginData) {
        ExtendedLoginData extendedLoginData = (ExtendedLoginData) loginData;

        if (Boolean
                .valueOf(extendedLoginData.extendedData.get(Constants.ENABLE_TRADING_CHECKBOX_NAME).getStringValue())) {
            throw new IllegalArgumentException("Trading not supported");
        } else {
            Layer1ApiProvider provider = new OkcoinRealTimeProvider(OkcoinConstants.EXCHANGE, OkcoinConstants.WS_PORT_NUMBER, OkcoinConstants.WS_LINK);
            setProvider(provider);
            Log.info("OkexRealtimeProvider " + provider.hashCode());
        }
        ListenableHelper.addListeners(provider, this);
        super.login(loginData);
    }
}
