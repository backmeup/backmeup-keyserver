package org.backmeup.keyserver.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.keyserver.core.Keyserver;
import org.backmeup.keyserver.rest.cdi.JNDIBeanManager;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All rest classes derive from this class to gain access to the BusinessLogic.
 * Note: The derived classes always delegate the incoming REST call to the
 * business logic.
 */
public class Base {
    private static final Logger LOGGER = LoggerFactory.getLogger(Base.class);

    private Mapper mapper;

    @Inject
    @ApplicationScoped
    private Keyserver keyserverLogic;

    public Keyserver getKeyserverLogic() {
        return keyserverLogic;
    }

    protected synchronized Mapper getMapper() {
        if (mapper == null) {
            mapper = fetchInstanceFromJndi(Mapper.class);
        }
        return mapper;
    }
    
    protected <T, U> U map(final T source, final Class<U> destType) {
        return this.getMapper().map(source, destType);
    }
    
    protected <T, U> List<U> map(final List<T> source, final Class<U> destType) {
        final List<U> dest = new ArrayList<>();

        for (T element : source) {
            dest.add(this.getMapper().map(element, destType));
        }

        return dest;
    }

    private <T> T fetchInstanceFromJndi(Class<T> classType) {
        try {
            JNDIBeanManager jndiManager = JNDIBeanManager.getInstance();
            return jndiManager.getBean(classType);
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }
}
