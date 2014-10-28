package org.backmeup.keyserver.core;

public class EntryNotFoundException extends KeyserverException {

    private static final long serialVersionUID = -5007898461216047038L;

    protected static final String APP = "app not found";

    protected static final String USERNAME = "username not found";

    protected static final String ACCOUNT = "account not found";

    protected static final String TOKEN = "token not found";
    
    protected static final String TOKEN_ANNOTATION = "token annotation not found";

    public EntryNotFoundException(String message) {
        super(message);
    }
}
