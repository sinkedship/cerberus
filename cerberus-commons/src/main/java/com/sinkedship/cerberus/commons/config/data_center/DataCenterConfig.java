package com.sinkedship.cerberus.commons.config.data_center;

import com.sinkedship.cerberus.commons.DataCenter;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * @author Derrick Guan
 */
public abstract class DataCenterConfig {

    private final DataCenter center;

    private final RegistrationInfo registrationInfo;

    private static class RegistrationInfo {
        private String registerHost;

        String getRegisterHost() {
            return registerHost;
        }

        void setRegisterHost(String registerHost) {
            this.registerHost = registerHost;
        }
    }

    /**
     * Server side specific configuration
     * should not be used in client and will not take any affects if it does set by client.
     *
     * @param registerHost the host registered to data center of services.
     */
    public void setRegisterHost(String registerHost) {
        Preconditions.checkArgument(!StringUtils.isBlank(registerHost),
                "Register host cannot be empty");
        registrationInfo.setRegisterHost(registerHost);
    }

    /**
     * The host registered to data center of services.
     *
     * @return the host registered to data center of services, null otherwise.
     */
    public Optional<String> getRegisterHost() {
        return Optional.ofNullable(registrationInfo.getRegisterHost());
    }

    public DataCenterConfig(DataCenter center) {
        this.center = center;
        registrationInfo = new RegistrationInfo();
    }

    public final DataCenter getDataCenter() {
        return center;
    }

}
