package org.backmeup.keyserver.model;


public class EntryNotFoundException extends KeyserverException {

    private static final long serialVersionUID = -5007898461216047038L;

    public static final String APP = "app not found";

    public static final String USERNAME = "username not found";
    
    public static final String SERVICE_USER_ID = "serviceUserId not found";

    public static final String ACCOUNT = "account not found";
    
    public static final String PROFILE = "profile not found";
    
    public static final String INDEX = "index key not found";

    public static final String TOKEN = "token not found or expired";
    
    public static final String TOKEN_ANNOTATION = "token annotation not found";
    
    public static final String TOKEN_USER_REMOVED = "user of token doesn't exist anymore";

    public static final String TOKEN_USED_TO_EARLY = "token used to early";

    public static final String PLUGIN_KEY = "plugin key not found";

    public static final String PLUGIN = "plugin not found";
        
    public EntryNotFoundException(String message) {
        super(message);
    }
}
