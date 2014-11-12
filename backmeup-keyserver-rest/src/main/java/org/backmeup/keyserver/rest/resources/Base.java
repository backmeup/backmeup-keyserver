package org.backmeup.keyserver.rest.resources;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;



import org.backmeup.keyserver.core.Keyserver;
//import org.backmeup.logic.BusinessLogic;
//import org.backmeup.rest.BusinessLogicContextHolder;
import org.backmeup.keyserver.rest.cdi.JNDIBeanManager;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All rest classes derive from this class to gain access to the BusinessLogic. 
 * Note: The derived classes always delegate the incoming REST call to the business logic.
 */
public class Base {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Mapper mapper;
    
    @Inject
    private Keyserver keyserverLogic;

    public Keyserver getKeyserverLogic() {
        return keyserverLogic;
    }
    
    protected Mapper getMapper() {
    	if (mapper == null) {
    		mapper = fetchInstanceFromJndi(Mapper.class);
    	}
    	return mapper;
    }

    private <T> T fetchInstanceFromJndi(Class<T> classType) {
        try {
            JNDIBeanManager jndiManager = JNDIBeanManager.getInstance();
            return jndiManager.getBean(classType);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }
}
