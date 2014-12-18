package org.backmeup.keyserver.rest.cdi;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNDIBeanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JNDIBeanManager.class);

    private static final String JNDI_NAME_1 = "java:comp/BeanManager";
    private static final String JNDI_NAME_2 = "java:comp/env/BeanManager";
    private static final String JNDI_NAME_3 = "java:comp/app/BeanManager";

    private static JNDIBeanManager instance;
    
    private final BeanManager beanManager;

    public JNDIBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(type));
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, type, creationalContext);
    }
    
    public static synchronized JNDIBeanManager getInstance() {
        if (instance == null) {
            BeanManager beanManager = fetchBeanManagerFromJndi();
            instance = new JNDIBeanManager(beanManager);
        }
        return instance;
    }

    private static BeanManager fetchBeanManagerFromJndi() {
        List<String> jndiNames = Arrays.asList(JNDI_NAME_1, JNDI_NAME_2, JNDI_NAME_3);
        return fetchBeanManagerFromJndi(jndiNames);
    }

    private static BeanManager fetchBeanManagerFromJndi(List<String> jndiNames) {
        failIfNoNamesLeft(jndiNames);

        String jndiName = jndiNames.get(0);
        try {

            BeanManager beanManager = lookupBeanManager(jndiName);
            LOGGER.debug("BeanManger successfully located");
            return beanManager;

        } catch (Exception e) {
            LOGGER.debug("BeanManager under JNDI name {} not found", jndiName, e);

            List<String> remainingNames = jndiNames.subList(1, jndiNames.size());
            return fetchBeanManagerFromJndi(remainingNames);
        }
    }

    private static void failIfNoNamesLeft(List<String> jndiNames) {
        if (jndiNames.isEmpty()) {
            throw new IllegalStateException("BeanManager cannot be found");
        }
    }

    private static BeanManager lookupBeanManager(String jndiName) throws NamingException {
        InitialContext context = new InitialContext();
        return (BeanManager) context.lookup(jndiName);
    }
}
