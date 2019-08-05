package com.sinkedship.cerberus.bootstrap;

import com.sinkedship.cerberus.core.Service;

import java.util.List;

/**
 * @author Derrick Guan
 */
public interface RegisterSuccessHandler<X extends Service> {

    void onSuccess(List<X> registerServices);

}
