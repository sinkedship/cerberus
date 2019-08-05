package com.sinkedship.cerberus.core.api;

import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.DataCenterConfig;
import com.sinkedship.cerberus.commons.exception.CerberusException;
import com.sinkedship.cerberus.core.Service;

/**
 * Simple factory creates {@link Registry} according to {@link DataCenter}.
 *
 * @author Derrick Guan
 */
public interface RegistryFactory {

    Registry createRegistry(DataCenterConfig dataCenterConfig) throws CerberusException;
}
