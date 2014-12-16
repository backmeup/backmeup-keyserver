package org.backmeup.keyserver.rest.filters;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.core.Keyserver;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.rest.auth.KeyserverSecurityContext;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityInterceptor.class);

    @Context
    private ServletContext context;

    @Inject
    private Keyserver keyserverLogic;

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

            String appId;
            String password;
            // Split appId and password tokens
            final StringTokenizer tokenizer = new StringTokenizer(accessToken, ";");
            if (tokenizer.countTokens() == 2) {
                appId = tokenizer.nextToken();
                password = tokenizer.nextToken();
            } else {
                appId = "";
                password = "";
            }

            // Verify token
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
                Set<String> rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));

                App app = resolveApp(appId, password);
                if (app == null) {
                    requestContext.abortWith(ACCESS_DENIED);
                    return;
                }

                if (!isAppAllowed(app, rolesSet)) {
                    requestContext.abortWith(ACCESS_DENIED);
                    return;
                }

                requestContext.setSecurityContext(new KeyserverSecurityContext(app));
            }
        }
    }

    private App resolveApp(final String appId, final String password) {
        try {
            return keyserverLogic.authenticateApp(appId, password);
        } catch (KeyserverException e) {
            LOGGER.info("Login failed. Appid \"{}\" or password wrong.", appId, e);
            return null;
        }
    }

    private boolean isAppAllowed(final App app, final Set<String> rolesSet) {        
        if (rolesSet.contains(app.getAppRole().name())) {
            return true;
        }
        
        return false;
    }
    
    /*
    private <T> T fetchInstanceFromJndi(Class<T> classType) {
        try {
            JNDIBeanManager jndiManager = JNDIBeanManager.getInstance();
            return jndiManager.getBean(classType);
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }
    */
}
