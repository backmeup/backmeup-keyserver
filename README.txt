If you would run the keyserver on your system you need tomcat and an postgress Database.

To keep your local machine clean we have created an vmware image.
The image of the keyserver can be found on:
http://service.x-net.at/Linux/keysrv/bmu-keysrv01.7z

This is an ubuntu 12.04 image with everything configured (tomcat, database).

The System starts everthing automatically at startup.
The System gets its ip address with dhcp.

Login credentials:
User: root
PWD: toor

To get the newest codeset from the repo:
cd /root/backmeup-keyserver
git pull


Before you build the keyserver code import the new schema to database!
See SQL section below.

To build and deploy everything:
mvn clean tomcat:redeploy


Get the local ip address:
ifconfig




SQL:
!!WARNING!!
This will wipe all data from the keyserver database!

cd /root/keysrv
git pull
cp db_keysrv.sql /tmp/
chmod 777 /tmp/db_keysrv.sql
su - postgres
psql db_keysrv < /tmp/db_keysrv.sql
exit
rm /tmp/db_keysrv.sql