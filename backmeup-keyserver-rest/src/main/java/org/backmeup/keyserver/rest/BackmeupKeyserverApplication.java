package org.backmeup.keyserver.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.backmeup.keyserver.rest.filters.SecurityInterceptor;
import org.backmeup.keyserver.rest.filters.TimingResourceFilter;
import org.backmeup.keyserver.rest.provider.JacksonJsonConfiguration;
import org.backmeup.keyserver.rest.provider.KeyserverExceptionMapper;
import org.backmeup.keyserver.rest.resources.Applications;
import org.backmeup.keyserver.rest.resources.Tokens;
import org.backmeup.keyserver.rest.resources.Users;

public class BackmeupKeyserverApplication extends Application {
    private final Set<Class<?>> set = new HashSet<>();
    private final Set<Object> singletons = new HashSet<>();

    public BackmeupKeyserverApplication() {
        // The default life-cycle for resource class instances is per-request.
        set.add(Applications.class);
        set.add(Users.class);
        set.add(Tokens.class);

        // The default life-cycle for providers (registered directly or via a feature) is singleton.
        set.add(JacksonJsonConfiguration.class);
        set.add(KeyserverExceptionMapper.class);
        set.add(TimingResourceFilter.class);
        set.add(SecurityInterceptor.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
