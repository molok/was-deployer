WAS-Deployer
===

This is a little utility to deploy WARs on IBM Websphere Application Server (WAS) using its web interface.

WAS has a quite capable command line interface but the sysadmins only let me use the web-interface so I automated this
mindless task for my own sanity.

This is "tested" on WAS 8.5.5.9, 8.5.5.13 and 9.0.0.10 traditional profile only (not the Liberty one), use it at your own risk.

The code is written in Java and uses Selenium with the Chrome WebDriver, it can be run headless or not.
On the first startup the Chrome WebDriver is downloaded for the official website, a Chrome installation is required.

```
WAS-Deployer v1.0.2

usage:

java -jar was-deployer.jar deploy|start|stop|uninstall|list|config-sample [<args>]

deploy
 -a,--auth <user:password>         WAS username and password, e.g. wsadmin:secret
 -c,--contextroot <context_root>   context-root (if not supplied the name of the application is
                                   used)
 -f,--file <war_path>              location of the WAR to deploy, it needs to be accessible by
                                   the server
 -g,--gui                          the browser is shown, by the default it is run headless
 -i,--install                      install the application
 -n,--name <app_name>              name of the application (if not supplied the name it is
                                   derived from the war filename)
 -p,--profile <profile_name>       Name of the profile set in
                                   $HOME/.config/was-deployer.yaml, overrides --server,
                                   --auth and --servernames
 -r,--remote <war_remote_path>     remote location of the WAR to deploy
 -s,--server <server_url>          URL of the server, e.g. https://example.org:9043
 -sn,--servernames <server_name>   Java regex of the server where the WAR will be installed,
                                   e.g. (.*AppCluster.*|.*webserver.*)
 -u,--update                       update the application
 -v,--verbose                      Increase verbosity
 -vv,--very-verbose                Increase verbosity more

list
 -a,--auth <user:password>     WAS username and password, e.g. wsadmin:secret
 -n,--name <app_name>          name of the application, if not supplied all installed
                               applications will be listed
 -p,--profile <profile_name>   Name of the profile set in
                               $HOME/.config/was-deployer.yaml, overrides --server,
                               --auth
 -s,--server <server_url>      URL of the server, e.g. https://example.org:9043
 -v,--verbose                  Increase verbosity
 -vv,--very-verbose            Increase verbosity more

start
 -a,--auth <user:password>     WAS username and password, e.g. wsadmin:secret
 -n,--name <app_name>          name of the application, if not supplied all installed
                               applications will be listed
 -p,--profile <profile_name>   Name of the profile set in
                               $HOME/.config/was-deployer.yaml, overrides --server,
                               --auth
 -s,--server <server_url>      URL of the server, e.g. https://example.org:9043
 -v,--verbose                  Increase verbosity
 -vv,--very-verbose            Increase verbosity more

stop
 -a,--auth <user:password>     WAS username and password, e.g. wsadmin:secret
 -n,--name <app_name>          name of the application, if not supplied all installed
                               applications will be listed
 -p,--profile <profile_name>   Name of the profile set in
                               $HOME/.config/was-deployer.yaml, overrides --server,
                               --auth
 -s,--server <server_url>      URL of the server, e.g. https://example.org:9043
 -v,--verbose                  Increase verbosity
 -vv,--very-verbose            Increase verbosity more

uninstall
 -a,--auth <user:password>     WAS username and password, e.g. wsadmin:secret
 -n,--name <app_name>          name of the application, if not supplied all installed
                               applications will be listed
 -p,--profile <profile_name>   Name of the profile set in
                               $HOME/.config/was-deployer.yaml, overrides --server,
                               --auth
 -s,--server <server_url>      URL of the server, e.g. https://example.org:9043
 -v,--verbose                  Increase verbosity
 -vv,--very-verbose            Increase verbosity more

config-sample


examples: 
deploy app.war to a WAS local instance
    $ java -jar was-deployer.jar deploy -i -f ./app.war -n app \
                                        -s 'https://localhost:9043' \
                                        -a wsadmin:secret

deploy app.war to a WAS instance (dev) configured in $HOME/.config/was-deployer.yaml
    $ java -jar was-deployer.jar deploy -i -f ./app.war -n app -p dev

$HOME/.config/was-deployer.yaml looks like this:
    profiles:
        - name: "dev"
          user: "wsadmin"
          password: "secret"
          server: "https://localhost:9043" 
          serverNames: ""
          
stop an application on dev instance:
    $ java -jar was-deployer.jar stop -n app -p dev          


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
