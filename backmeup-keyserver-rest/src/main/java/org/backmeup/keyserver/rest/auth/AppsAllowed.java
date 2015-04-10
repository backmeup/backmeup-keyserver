package org.backmeup.keyserver.rest.auth;

import java.lang.annotation.*;

import org.backmeup.keyserver.model.App;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface AppsAllowed {
    App.Approle[] value();
}
