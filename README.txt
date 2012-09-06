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
cd /root/keysrv
git pull

To build everything:
mvn clean tomcat:redeploy

Get the local ip address:
ifconfig