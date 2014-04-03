# Backmeup Keyserver
Backemup Keyserver provides a secure storage for personal keys. 

## Requirements 
To build and run the Keyserver on your system you need [Apache Maven 3.x](https://maven.apache.org/), a servlet container and a database system. We recommend the following:
* [Apache Tomcat 7.x](https://tomcat.apache.org/)
* [PostgresSQL 9.3.x](http://www.postgresql.org/)
Additional dependencies will be resolved and loaded from Maven. Have a look at `pom.xml`.

## Configuration
Before starting the Maven build we have to check and if necessary adjust various configuration files.
`pom.xml`.
Check the properties section and change the settings if necessary. It is very likely that you have to adjust the properties `<config.database.url>` and `<config.tomcat.manager.url>`. 

#### src/main/ressources/keyserver.properties
Adjust the file paths. 

#### src/main/webapp/META-INF/context.xml
Adjust the database settings. 

#### src/test/resources/integrationtests.properties
Adjust the settings that are used for the integration test. The settings refer to the deploy keyserver. 

#### PostgreSQL
First, you have to create a database user (with the name `dbu_keysrv`):
```sql
CREATE USER dbu_keysrv WITH PASSWORD 'xxx';
```

Second, import the database schema. The script can be found in `src/main/sql/db-keyserver-schema.sql`. Create a new schema or use an existing one (default name: db_keysrv) and execute the script on it. Use the command line client (psql) or pgAdmin. 

#### Tomcat
Configure access to the manager application. In the file tomcat-users.xml add roles for manager-gui and manager-script and add a user:
```xml
<tomcat-users>
  <!-- ... -->
  <role rolename="manager-gui"/>
  <role rolename="manager-script"/>
  
  <user username="xxx" password=" xxx " roles="manager-gui,manager-script"/>
</tomcat-users>
```

#### Maven
Authentication information (username/password) from the pom-file is hidden in the Maven settings file (`settings.xml`). The pom-file specifies two keys (`backmeup.keyserver.postgres`, `backmeup.keyserver.tomcat`) that have to match the credentials configured in the steps above (PostgreSQL, Tomcat):
```xml
<servers>
    <!-- ... -->
    <server>
      <id>backmeup.keyserver.tomcat</id>
      <username>xxx</username>
      <password>xxx</password>
    </server>

    <server>
      <id>backmeup.keyserver.postgres</id>
      <username>dbu_keysrv</username>
      <password>xxx</password>
    </server>
</servers>
```
## Build & Deploy

## Contribute

## Support

## License 
