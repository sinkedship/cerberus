package com.sinkedship.cerberus.bootstrap;

import com.sinkedship.cerberus.core.Service;

import java.util.List;

/**
 * @author Derrick Guan
 */
public interface RegisterFailureHandler<X extends Service> {

    void onFailure(List<X> registerServices);

}
