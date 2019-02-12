WAS-Deployer
===

This is a little utility to deploy WARs on IBM Websphere Application Server (WAS) using its web interface.

WAS has a quite capable command line interface but the sysadmins only let me use the web-interface so I automated this
mindless task for my own sanity.

This is "tested" on WAS 8.5.5.9, 8.5.5.13 and 9.0.0.10 traditional profile only (not the Liberty one), use it at your own risk.

The code is written in Java and uses Selenium with the Chrome WebDriver, it can be run headless or not.
On the first startup the Chrome WebDriver is downloaded for the official website, a Chrome installation is required.

```
usage: java -jar was-deployer.jar deploy -a <user:password> [-c <app_context_root>] -f
       <war_path> | -r <war_remote_path> [-g] -i | -u -n <app_name>  -s
       <server_url> [-sn <server_name>]
 -a,--auth <user:password>             WAS username and password, e.g.
                                       wsadmin:secret
 -c,--contextroot <app_context_root>   context-root (if not supplied the name of
                                       the application is used)
 -f,--file <war_path>                  location of the WAR to deploy, it needs
                                       to be accessible by the server
 -g,--gui                              the browser is shown, by the default it
                                       is run headless
 -i,--install                          install the application
 -n,--name <app_name>                  name of the application
 -r,--remote <war_remote_path>         remote location of the WAR to deploy
 -s,--server <server_url>              URL of the server, e.g.
                                       https://localhost:9043
 -sn,--servernames <server_name>       Java regex of the server where the WAR
                                       will be installed, e.g.
                                       (.*AppCluster.*|.*webserver.*)
 -u,--update                           update the application

version 1.0.1

example: java -jar was-deployer deploy -i -f ./app.war -n app -s 'https://localhost:9043' -a wsadmin:secret

```

```
Copyright (c) 2019 Alessio Bolognino
 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
