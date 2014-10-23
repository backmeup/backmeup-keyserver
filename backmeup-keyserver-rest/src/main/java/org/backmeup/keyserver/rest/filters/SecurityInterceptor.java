package org.backmeup.keyserver.rest.filters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.rest.auth.KeyserverSecurityContext;
/*
import org.backmeup.rest.BusinessLogicContextHolder;
import org.backmeup.rest.cdi.JNDIBeanManager;
*/
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class SecurityInterceptor implements ContainerRequestFilter {
    private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse("Access forbidden", 403, new Headers<>());
    private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource", 401, new Headers<>());
    private static final String AUTHORIZATION_PROPERTY = "Authorization";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Context
    private ServletContext context;

    /*
     * private BusinessLogic logic;
     * 
     * private BusinessLogic getLogic() { BusinessLogicContextHolder
     * contextHolder = new BusinessLogicContextHolder(context);
     * 
     * logic = contextHolder.get();
     * 
     * if (logic == null) { logic = fetchInstanceFromJndi(BusinessLogic.class);
     * contextHolder.set(logic); }
     * 
     * return logic; }
     */

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();

        if (!method.isAnnotationPresent(PermitAll.class)) {
            if (method.isAnnotationPresent(DenyAll.class)) {
                requestContext.abortWith(ACCESS_FORBIDDEN);
                return;
            }

            // Get authorization header
            final MultivaluedMap<String, String> headers = requestContext.getHeaders();
            final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);

            // If no authorization header, deny access
            if (authorization == null || authorization.isEmpty()) {
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }

            // Get token from header
            final String accessToken = authorization.get(0);

            // Split username/userId and password tokens
            final StringTokenizer tokenizer = new StringTokenizer(accessToken, ";");
            final String appId = tokenizer.nextToken(); // userId can only be a
                                                        // String
            final String password = tokenizer.nextToken();

            // Verify token
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
                Set<String> rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));

                AppUser user = resolveUser(appId, password);
                if (user == null) {
                    requestContext.abortWith(ACCESS_DENIED);
                    return;
                }

                if (!isUserAllowed(user, rolesSet)) {
                    requestContext.abortWith(ACCESS_DENIED);
                    return;
                }

                requestContext.setSecurityContext(new KeyserverSecurityContext(user));
            }
        }
    }

    private AppUser resolveUser(final String userId, final String password) {
        // TODO replace test implementation

        return new AppUser(userId, password, AppUser.Approle.CORE);

        /*
         * try { return getLogic().getAppUserByUserId(userId); } catch
         * (UnknownAppUserException uaue) { return null; }
         */
    }

    private boolean isUserAllowed(final AppUser user, final Set<String> rolesSet) {
        
        //TODO replace test function
        
        boolean isAllowed = true;

        // Verify user role
        // if (rolesSet.contains(userRole)) {
        // isAllowed = true;
        // }

        return isAllowed;
    }

    /*
    private <T> T fetchInstanceFromJndi(Class<T> classType) {
        try {
            JNDIBeanManager jndiManager = JNDIBeanManager.getInstance();
            return jndiManager.getBean(classType);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }
    */
}
