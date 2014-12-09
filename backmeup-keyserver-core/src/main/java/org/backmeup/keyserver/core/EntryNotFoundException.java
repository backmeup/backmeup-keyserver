package org.backmeup.keyserver.core;

public class EntryNotFoundException extends KeyserverException {

    private static final long serialVersionUID = -5007898461216047038L;

    protected static final String APP = "app not found";

    protected static final String USERNAME = "username not found";
    
    protected static final String SERVICE_USER_ID = "serviceUserId not found";

    protected static final String ACCOUNT = "account not found";
    
    protected static final String PROFILE = "profile not found";
    
    protected static final String INDEX = "index key not found";

    protected static final String TOKEN = "token not found";
    
    protected static final String TOKEN_ANNOTATION = "token annotation not found";
    
    protected static final String TOKEN_USER_REMOVED = "user of token doesn't exist anymore";

    protected static final String PLUGIN_KEY = "plugin key not found";

    protected static final String PLUGIN = "plugin not found";
    
    public EntryNotFoundException(String message) {
        super(message);
    }
}
