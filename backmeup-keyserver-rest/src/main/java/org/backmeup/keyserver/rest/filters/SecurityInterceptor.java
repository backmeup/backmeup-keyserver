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
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

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

            App app = this.parseAuthorizationHeader(requestContext);
            if (app == null) {
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }

            // Verify token
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                Set<String> rolesSet = new HashSet<>(Arrays.asList(method.getAnnotation(RolesAllowed.class).value()));
                
                if (!isAppAllowed(app, rolesSet)) {
                    requestContext.abortWith(ACCESS_DENIED);
                    return;
                }                
            }
            
            requestContext.setSecurityContext(new KeyserverSecurityContext(app));
        }
    }
    
    private App parseAuthorizationHeader(ContainerRequestContext requestContext) {
     // Get authorization header
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);

        // If no authorization header, deny access
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        // Split appId and password tokens
        final StringTokenizer tokenizer = new StringTokenizer(authorization.get(0), ";");
        if (tokenizer.countTokens() == 2) {
            String appId = tokenizer.nextToken();
            String password = tokenizer.nextToken();
            return resolveApp(appId, password);
        } else {
            return null;
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
}
