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
import javax.ws.rs.ext.Provider;

import org.backmeup.keyserver.core.Keyserver;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.rest.auth.KeyserverSecurityContext;
import org.backmeup.keyserver.rest.auth.LoginTokenRequired;
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
    private static final String TOKEN_PROPERTY = "Token";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityInterceptor.class);

    @Inject
    private Keyserver keyserverLogic;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();

        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        
        if (method.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(ACCESS_FORBIDDEN);
            return;
        }

        // parse and verify app
        App app = this.parseAuthorizationHeader(requestContext);
        if (app == null) {
            requestContext.abortWith(ACCESS_DENIED);
            return;
        }

        // verify app roles
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            Set<String> rolesSet = new HashSet<>(Arrays.asList(method.getAnnotation(RolesAllowed.class).value()));

            if (!isAppAllowed(app, rolesSet)) {
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }
        }

        // parse and verify token
        AuthResponse auth = null;
        try {
            Token token = this.parseTokenHeader(requestContext);
            auth = this.authenticateToken(token);
            
            if (method.isAnnotationPresent(LoginTokenRequired.class) && auth == null) {
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }
        } catch (IllegalArgumentException e) {
            requestContext.abortWith(new ServerResponse("Unkown token kind", 400, new Headers<>()));
            LOGGER.info("Unkown token kind", e);
            return;
        } catch (KeyserverException e) {
            requestContext.abortWith(ACCESS_DENIED);
            LOGGER.info("Exception in SecurityInterceptor:", e);
            return;
        }

        requestContext.setSecurityContext(new KeyserverSecurityContext(app, auth));
    }

    private App parseAuthorizationHeader(ContainerRequestContext requestContext) {
        // Get authorization header
        final List<String> authorization = requestContext.getHeaders().get(AUTHORIZATION_PROPERTY);

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

    private Token parseTokenHeader(ContainerRequestContext requestContext) {
        // Get token header
        final List<String> tokenHeader = requestContext.getHeaders().get(TOKEN_PROPERTY);

        if (tokenHeader == null || tokenHeader.isEmpty()) {
            return null;
        }

        // Split token type and base64 token
        final StringTokenizer tokenizer = new StringTokenizer(tokenHeader.get(0), ";");
        if (tokenizer.countTokens() == 2) {
            String type = tokenizer.nextToken();
            String token = tokenizer.nextToken();
            return new Token(Token.Kind.valueOf(type.toUpperCase()), token);
        } else {
            return null;
        }
    }
    
    private AuthResponse authenticateToken(Token token) throws KeyserverException {
        if (token != null) {
            switch (token.getKind()) {
                case INTERNAL:
                    return this.keyserverLogic.authenticateWithInternalToken(token.getB64Token());
                case EXTERNAL:
                    // at this time we only accept internal tokens
                case ONETIME:
                    // we will never support onetime tokens here
                default:
                    break;
            }
        }
        
        return null;
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
