# Setup for the Olympic Summer Games 2024 Paris/Marseille

[[_TOC_]]

## Local Installation

For the Olympic Summer Games 2024 Paris/Marseille we use a dedicated hardware set-up to accommodate the requirements on site. In particular, two Lenovo P1 laptops with similar hardware configuration (32GB RAM, Intel Core i9-9880H) will be established as server devices running various services in a way that we can tolerate, with minimal downtimes, failures of either of the two devices. One is the old ``sap-p1-1`` that was already used for the "Paris 2024" event; the other one is a newly ordered one (which we have received).

### Installation Packages

The old laptop runs Mint Linux with a fairly modern 5.4 kernel, whilst the newer one runs Ubuntu. We keep both up to date with regular ``apt-get update && apt-get upgrade`` executions. Both have an up-to-date SAP JVM 8 (see [https://tools.hana.ondemand.com/#cloud](https://tools.hana.ondemand.com/#cloud)) installed under /opt/sapjvm_8. This is the runtime VM used to run the Java application server process.

Furthermore, both laptops have a MongoDB 4.4 installation configured through ``/etc/apt/sources.list.d/mongodb-org-4.4.list`` containing the line ``deb http://repo.mongodb.org/apt/debian jessie/mongodb-org/4.4 main``. Their respective configuration can be found under ``/etc/mongod.conf``. The WiredTiger storage engine cache size should be limited. Currently, the following entry in ``/etc/mongod.conf`` does this. Installing an older version of ``libssl`` may be required on newer Ubuntu versions (starting with 22.04) to be able to install MongoDB 4.4.

An installation of Python (e.g., ``python3``) will be required for the Manage2Sail update notification script (see ``configuration/on-site-scripts/paris2024/sap-p1-2/notifyAboutOSG2024TEV2023Updates``).

RabbitMQ is part of the distribution natively. It runs on both laptops. Both, RabbitMQ and MongoDB are installed as systemd service units and are launched during the boot sequence. The latest GWT version (currently our own fork, 2.11.0) is installed from [https://static.sapsailing.com/wt-2.11.0.zip](https://static.sapsailing.com/wt-2.11.0.zip) under ``/opt/gwt-2.11.0`` in case any development work would need to be done on these machines.

Both machines have been configured to use 2GB of swap space at ``/swapfile``.

### Mongo Configuration

On both laptops, the ``/etc/mongod.conf`` configuration configures ``/var/lib/mongodb`` to be the storage directory, and the in-memory cache size to be 2GB:

```
storage:
  dbPath: /var/lib/mongodb
  journal:
    enabled: true
  wiredTiger:
    engineConfig:
      cacheSizeGB: 2
```

The port is set to ``10201`` on ``sap-p1-1``:

```
# network interfaces
net:
  port: 10201
  bindIp: 0.0.0.0
```

and to ``10202`` on ``sap-p1-2``:

```
# network interfaces
net:
  port: 10202
  bindIp: 0.0.0.0
```

Furthermore, the replica set is configured to be ``paris2024`` on both:

```
replication:
  oplogSizeMB: 10000
  replSetName: paris2024
```

For "Paris 2024" we configured yet another MongoDB replica set that consisted only of the two on-site nodes and where we stored the backup copy of the ``security_service`` database. We should, however, be able to store the ``security_service`` DB backup in the same replica set of which the two local nodes with their MongoDB processes listening on ports ``10201/10202``. The ``security_service`` database is used as the target for a backup script for the ``security_service`` database. See below. We increased the priority of the ``sap-p1-1`` node from 1 to 2.

For log rotation, the following file must be created at ``/etc/logrotate.d/mongodb``:

```
compress
/var/log/mongodb/mongod.log
{
   rotate 5
   weekly
   postrotate
       /usr/bin/killall -SIGUSR1 mongod
   endscript
}
```

and likewise, if a second MongoDB replica set is running producing, e.g., ``/var/log/mongodb/mongod-security-service.log`` then you need to add a second file, e.g., at ``/etc/logrotate.d/mongodb-security-service`` like this:

```
compress
/var/log/mongodb/mongod-security-service.log
{
   rotate 5
   weekly
   postrotate
       /usr/bin/killall -SIGUSR1 mongod
   endscript
}
```

### User Accounts

The essential user account on both laptops is ``sailing``. The account is intended to be used for running the Java VM that executes the SAP Sailing Analytics server software. The account is currently still protected by a password that our on-site team should know. On both laptops the ``sailing`` account has a password-less SSH key installed under ``/home/sailing/.ssh`` that is contained in the ``known_hosts`` file of ``paris-ssh.sapsailing.com`` as well as the mutually other P1 laptop. This way, all tunnels can easily be created once logged on to this ``sailing`` account.

There are also still two personal accounts ``uhl`` and ``tim`` and an Eclipse development environment under ``/usr/local/eclipse``.

### Hostnames

DNS is available on site on the gateway host ``10.1.0.6``. This is essential for resolving ``www.igtimi.com``, the AWS SES SMTP server at ``email-smtp.eu-west-1.amazonaws.com`` and all e-mail address's domains for sendmail's domain verification. The DNS server is set for both, ``sap-p1-1`` and ``sap-p1-2``. It can be set from the command line using ``nmcli connection modify Wired\ connection\ 2 ipv4.dns "10.1.0.6"; nmcli connection down Wired\ connection\ 2; nmcli connection up Wired\ connection\ 2``. Currently, when testing in the SAP facilities with the SAP Guest WiFi, possibly changing IP addresses have to be updated in ``/etc/hosts``.

The domain name has been set to ``sapsailing.com`` so that the fully-qualified host names are ``sap-p1-1.sapsailing.com`` and ``sap-p1-2.sapsailing.com`` respectively. Using this domain name is helpful later when it comes to the shared security realm established with the central ``security-service.sapsailing.com`` replica set.

The hostname ``www.sapsailing.com`` is required by master instances when connected to the Internet in order to download polar data and wind estimation data from the archive server. Since direct access to ``www.sapsailing.com`` is blocked, we run this through the SSH tunnel to our jump host; in order to have matching certificates and appropriate hostname-based routing in the cloud for requests to ``www.sapsailing.com`` we alias this hostname in ``/etc/hosts`` to ``127.0.0.1`` (localhost).

### IP Addresses and VPN

Here are the IP addresses as indicated by SwissTiming:

```
Host					Internal IP	VPN IP
-----------------------------------------------------------------------------------------
TracTrac A (Linux)			10.1.1.104	10.8.0.128	STSP-SAL_client28
TracTrac B (Linux)			10.1.1.105	10.8.0.129	STSP-SAL_client29
SAP Analytics 1 Server A (Linux)	10.1.3.195	10.8.0.130	STSP-SAL_client30
SAP Analytics 2 Server B (Linux)	10.1.3.197	10.8.0.131	
SAP Client Jan (Windows)		10.1.3.220	10.8.0.132	
SAP Client Alexandro (Windows)		10.1.3.221	10.8.0.133	
SAP Client Axel (Windows)		10.1.3.227	10.8.0.134	
TracTrac Dev Jorge (Linux)		10.1.3.228	10.8.0.135	
TracTrac Dev Chris (Linux)		10.1.3.233	10.8.0.136	
```

The OpenVPN connection is set up with the GUI of the Linux Desktop. Therefore the management is done through Network Manager. Network Manager has a CLI, ``nmcli``. With that more properties of connections can be modified. The ``connection.secondaries`` property defines the UUID of a connection that will be established as soon as the initial connection is working. With ``nmcli connection show`` you will get the list of connections with the corresponding UUIDs. For the Medemblik Event the OpenVPN connection to the A server is bound to the wired interface and made "persistent" (meaning it will retry connecting after being disconnected) that is used with

```
sudo nmcli connection modify <Wired Connection 2> +connection.secondaries <UUID-of-OpenVPN-A>
nmcli connection modify <Name-of-OpenVPN-A> connection.autoconnect-retries 0
nmcli connection modify <Name-of-OpenVPN-A> vpn.persistent yes
nmcli connection modify <Name-of-OpenVPN-B> connection.autoconnect-retries 0
nmcli connection modify <Name-of-OpenVPN-B> vpn.persistent yes
```

For the OpenVPN connections we have received two alternative configuration files together with keys and certificates for our server and work laptops, as well as the certificates for the OpenVPN server (``ca.crt``, ``dh.pem``, ``pfs.key``). The "A" configuration, e.g., provided in a file named ``st-soft-aws_A.ovpn``, looks like this:

```
client
dev tun
proto udp
remote 3.122.96.235 1195
ca ca.crt
cert {name-of-the-certificate}.crt
key {name-of-the-key}.key
tls-version-min 1.2
tls-cipher TLS-ECDHE-RSA-WITH-AES-128-GCM-SHA256:TLS-ECDHE-ECDSA-WITH-AES-128-GCM-SHA256:TLS-ECDHE-RSA-WITH-AES-256-GCM-SHA384:TLS-DHE-RSA-WITH-AES-256-CBC-SHA256
cipher AES-256-CBC
auth SHA512
resolv-retry infinite
auth-retry none
nobind
persist-key
persist-tun
ns-cert-type server
comp-lzo
verb 3
tls-client
tls-auth pfs.key
```

Here, ``{name-of-the-certificate}.crt`` and ``{name-of-the-key}.key`` need to be replaced by the names of the files corresponding with the host to connect to the OpenVPN. The "B" configuration only differs in the ``remote`` specification, using a different IP address for the OpenVPN server, namely ``52.59.130.167``. It is useful to copy the ``.ovpn`` file and the other ``.key`` and ``.crt`` files into one directory.

If you don't want the default route to be changed to the OpenVPN connection, add this to the .ovpn file:

```
pull-filter ignore "route-gateway"
```

Under Windows download the latest OpenVPN client from [https://openvpn.net/client-connect-vpn-for-windows/](https://openvpn.net/client-connect-vpn-for-windows/). After installation, use the ``.ovpn`` file, adjusted with your personalized key/certificate, to establish the connection.

On Linux, go to the global settings through Gnome, node "Network" and press the "+" button next to VPN. Import the ``.ovpn`` file, then enable the OpenVPN connection by flicking the switch. The connection will show in the output of

```
	nmcli connection show
```

The connection IDs will be shown, e.g., ``st-soft-aws_A``. Such a connection can be stopped and restarted from the command line using the following commands:

```
	nmcli connection down st-soft-aws_A
	nmcli connection up st-soft-aws_A
```

### Tunnels

On both laptops there is a script ``/usr/local/bin/tunnels`` which establishes SSH tunnels using the ``autossh`` tool. The ``autossh`` processes are forked into the background using the ``-f`` option. It seems important to then pass the port to use for sending heartbeats using the ``-M`` option. If this is omitted, according to my experience only one of several ``autossh`` processes survives. However, we have also learned that using the ``-M`` option together with the "port" ``0`` can help to stabilize the connection because in some cases, if ``-M`` is used with a real port, port collisions may result, and furthermore when re-connecting the release of those heartbeat ports cannot become an issue which otherwise it sometimes does. The ``-M 0`` option is particularly helpful when tunnelling to ``sapsailing.com`` which is provided through a network load balancer (NLB).

During regular operations we assume that we have an Internet connection that allows us to reach our jump host ``paris-ssh.sapsailing.com`` through SSH, establishing various port forwards. We also expect TracTrac to have their primary server available. Furthermore, we assume both our laptops to be in service. ``sap-p1-1`` then runs the master server instance, ``sap-p1-2`` runs a secondary master, which we can switch to in seconds. This comes at the expense of having to synchronise these devices, using crontabs and ${GIT_ROOT}/java/target/compareServers. They replicate the central security service at ``security-service.sapsailing.com`` using the RabbitMQ installation on ``rabbit.internal.sapsailing.com`` in the AWS region `eu-west-1`. The port forwarding through `paris-ssh.sapsailing.com` (in `eu-west-3`) to the internal RabbitMQ address (in eu-west-1) works through VPC peering. The RabbitMQ instance used for outbound replication, both, into the cloud and for the on-site replica, is `rabbit-eu-west-3.sapsailing.com`. The outside world, in particular all "S-paris2024-m" master security groups in all regions supported, access the on-site master through a reverse port forward on our jump host ``paris-ssh.sapsailing.com:8888`` which under regular operations points to ``sap-p1-1:8888`` where the master process runs.

On both laptops we establish a port forward from ``localhost:22443`` to ``sapsailing.com:443``. Together with the alias in ``/etc/hosts`` that aliases ``www.sapsailing.com`` to ``localhost``, requests to ``www.sapsailing.com:22443`` will end up on the archive server.

On both laptops, we maintain SSH connections to ``localhost`` with port forwards to the current TracTrac production server for HTTP, live data, and stored data. In the test we did on 2021-05-25, those port numbers were 9081, 14001, and 14011, respectively, for the primary server, and 9082, 14002, and 14012, respectively, for the secondary server. In addition to these port forwards, an entry in ``/etc/hosts`` is required for the hostname that TracTrac will use on site for their server(s), pointing to ``127.0.0.1`` to let the Sailing Analytics process connect to localhost with the port forwards. Tests have shown that if the port forwards are changed during live operations, e.g., to point to the secondary instead of the primary TracTrac server, the TracAPI continues smoothly which is a great way of handling such a fail-over process without having to re-start our master server necessarily or reconnect to all live races.

Furthermore, for administrative SSH access from outside, we establish reverse port forwards from our jump host ``paris-ssh.sapsailing.com`` to the SSH ports on ``sap-p1-1`` (on port 18122) and ``sap-p1-2`` (on port 18222).

Both laptops have a forward from ``localhost:22222`` to ``sapsailing.com:22`` through ``paris-ssh.sapsailing.com``, in order to be able to have a git remote ``ssh`` with the url ``ssh://trac@localhost:22222/home/trac/git``.

The port forwards vary for exceptional situations, such as when the Internet connection is not available, or when ``sap-p1-1`` that regularly runs the master process fails and we need to make ``sap-p1-2`` the new master. See below for the details of the configurations for those scenarios.

The tunnel configurations are established and configured using a set of scripts, each to be found under ``/usr/local/bin`` on each of the two laptops.

#### ssh_config and sshd_config tweaks

In order to recover quickly from failures we changed ``/etc/ssh/ssh_config`` on both of the P1s and added the following parameters:
```
ExitOnForwardFailure yes
ConnectTimeout 10
ServerAliveCountMax 3
ServerAliveInterval 10
```
For the server side on paris-ssh and on the both P1s the following parameters have been added to ``/etc/ssh/sshd_config``:
```
ClientAliveInterval 3
ClientAliveCountMax 3
GatewayPorts yes
```

The ``GatewayPorts`` directive is required in order to get port forwards (including reverse port forwards) accept the "*" as bind address to bind to 0.0.0.0 instead of 127.0.0.1. Without it a tunnel would only allow localhost connections to the forwarded ports. So for example from one of the P1s, `autossh -R portOnRemote:address:portLocally  ec2-user@paris-ssh.sapsailing.com`, would only allow connections from the `ec2-user@paris-ssh.sapsailing.com` instance to the `"port"`. By adding "yes", any host can access this port and be connected on to the `"portLocally"`.


ExitOnForwardFailure will force ssh to exit if one of the port forwards fails. ConnectTimeout manages the time in seconds until an initial connection fails. AliveInterval (client and server) manages the time in seconds after ssh/sshd are sending client and server alive probes. CountMax is the number of retries for those probes. 

The settings have been verified by executing a network change on both the laptops, the ssh tunnel returns after a couple of seconds.

#### Regular Operations: master on sap-p1-1, replica on sap-p1-2, with Internet / Cloud connection 

On sap-p1-1 two SSH connections are maintained, with the following default port forwards, assuming sap-p1-1 is the local master:

* paris-ssh.sapsailing.com: 10203-->10203; 5763-->rabbit-eu-west-3.sapsailing.com:5762; 15763-->rabbit-eu-west-3.sapsailing.com:15672; 5675:rabbit.internal.sapsailing.com:5672; 15675:rabbit.internal.sapsailing.com:15672; 10201<--10201; 18122<--22; 443:security-service.sapsailing.com:443; 8888<--8888; 9443<--9443
* sap-p1-2: 10202-->10202; 10201<--10201

On sap-p1-2, the following SSH connections are maintained, assuming sap-p1-2 is the local replica:

- paris-ssh.sapsailing.com: 10203-->10203; 5763-->rabbit-eu-west-3.sapsailing.com:5762; 15763-->rabbit-eu-west-3.sapsailing.com; 5675:rabbit.internal.sapsailing.com:5672; 15675:rabbit.internal.sapsailing.com:15672; 10202<--10202; 9444<--9443

A useful set of entries in your personal ``~/.ssh/config`` file for "off-site" use may look like this:

```
Host paris
    Hostname paris-ssh.sapsailing.com
    User ec2-user
    ForwardAgent yes
    ForwardX11Trusted yes
    LocalForward 18122 localhost:18122
    LocalForward 18222 localhost:18222
    LocalForward 9443 localhost:9443
    LocalForward 9444 localhost:9444

Host sap-p1-1
    Hostname localhost
    Port 18122
    User sailing
    ForwardAgent yes
    ForwardX11Trusted yes

Host sap-p1-2
    Hostname localhost
    Port 18222
    User sailing
    ForwardAgent yes
    ForwardX11Trusted yes
```

It will allow you to log on to the "jump host" ``paris-ssh.sapsailing.com`` with the simple command ``ssh paris`` and will establish the port forwards that will then allow you to connect to the two laptops using ``ssh sap-p1-1`` and ``ssh sap-p1-2``, respectively. Of course, when on site and with the two laptops in direct reach you may adjust the host entries for ``sap-p1-1`` and ``sap-p1-2`` accordingly, and you may then wish to establish only an SSH connection to ``sap-p1-1`` which then does the port forwards for HTTPS ports 9443/9444. This could look like this:

```
Host sap-p1-1
    Hostname 10.1.3.195
    Port 22
    User sailing
    ForwardAgent yes
    ForwardX11Trusted yes
    LocalForward 9443 localhost:9443
    LocalForward 9444 10.1.3.197:9443

Host sap-p1-2
    Hostname 10.1.3.197
    Port 22
    User sailing
    ForwardAgent yes
    ForwardX11Trusted yes
```

#### Operations with sap-p1-1 failing: master on sap-p1-2, with Internet / Cloud connection 

On sap-p1-1, if the operating system still runs and the failure affects only the Java process running the SAP Sailing Analytics, two SSH connections are maintained, with the following default port forwards, assuming sap-p1-1 is not running an SAP Sailing Analytics process currently:

* paris-ssh.sapsailing.com: 10203-->10203; 5763-->rabbit-eu-west-3.sapsailing.com:5762; 15763-->rabbit-eu-west-3.sapsailing.com:15672; 5675:rabbit.internal.sapsailing.com:5672; 15675:rabbit.internal.sapsailing.com:15672; 10201<--10201; 18122<--22; 443:security-service.sapsailing.com:443
* sap-p1-2: 10202-->10202; 10201<--10201

On sap-p1-2 two SSH connections are maintained, with the following default port forwards, assuming sap-p1-2 is the local master:

* paris-ssh.sapsailing.com: 10203-->10203; 5763-->rabbit-eu-west-3.sapsailing.com:5762; 15763-->rabbit-eu-west-3.sapsailing.com:15672; 5675:rabbit.internal.sapsailing.com:5672; 15675:rabbit.internal.sapsailing.com:15672; 10202<--10202; 18222<--22; 443:security-service.sapsailing.com:443; 8888<--8888
* sap-p1-1 (if the operating system on sap-p1-1 still runs): 10202-->10202; 10201<--10201

So the essential change is that the reverse forward from ``paris-ssh.sapsailing.com:8888`` now targets ``sap-p1-2:8888`` where we now assume the failover master to be running.

#### Operations with Internet Failing

When the Internet connection fails, replicating the security service from ``security-service.sapsailing.com`` / ``rabbit.internal.sapsailing.com`` will no longer be possible. Neither will outbound replication to ``rabbit-eu-west-3.sapsailing.com`` be possible, and cloud replicas won't be able to reach the on-site master anymore through the ``paris-ssh.sapsailing.com:8888`` reverse port forward. This also has an effect on the local on-site replica which no longer will be able to reach ``rabbit-eu-west-3.sapsailing.com`` which provides the on-site replica with the operation stream under regular circumstances.

There is little we can do against the lack of Internet connection regarding providing data to the cloud replicas and maintaining replication with ``security-service.sapsailing.com`` (we could theoretically try to work with local WiFi hotspots; but the key problem will be that TracTrac then neither has Internet connectivity for their on-site server, and we would have to radically change to a cloud-only set-up which is probably beyond what we'd be doing in this case). But we can ensure continued local operations with the replica on ``sap-p1-2`` now using a local on-site RabbitMQ installation between the two instances. For this, we replace the port forwards that during regular operations point to ``rabbit-eu-west-3.sapsailing.com`` by port forwards pointing to the RabbitMQ process on ``sap-p1-2``.

On ``sap-p1-1`` an SSH connection to ``sap-p1-2`` is maintained, with the following port forwards:

* sap-p1-2: 10202-->10202; 10201<--10201; 5763-->localhost:5672

So the essential changes are that there are no more SSH connections into the cloud, and the port forward on each laptop's port 5673, which would point to ``rabbit-eu-west-3.sapsailing.com`` during regular operations, now points to ``sap-p1-2:5672`` where the RabbitMQ installation takes over from the cloud instance.

### Letsencrypt Certificate for paris2024.sapsailing.com, security-service.sapsailing.com, paris2024-master.sapsailing.com, and paris2024-secondary-master.sapsailing.com

In order to allow us to access ``paris2024.sapsailing.com`` and ``security-service.sapsailing.com`` with any HTTPS port forwarding locally so that all ``JSESSION_GLOBAL`` etc. cookies with their ``Secure`` attribute are delivered properly, we need an SSL certificate. I've created one by doing

```
/usr/bin/sudo -u certbot docker run --rm -it --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" certbot/certbot certonly --manual -d paris2024.sapsailing.com
/usr/bin/sudo -u certbot docker run --rm -it --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" certbot/certbot certonly --manual -d paris2024-master.sapsailing.com
/usr/bin/sudo -u certbot docker run --rm -it --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" certbot/certbot certonly --manual -d paris2024-secondary-master.sapsailing.com
/usr/bin/sudo -u certbot docker run --rm -it --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" certbot/certbot certonly --manual -d security-service.sapsailing.com
```

as ``root`` on ``sapsailing.com``. The challenge displayed can be solved by creating an ALB rule for hostname header ``paris2024.sapsailing.com`` and the path as issued in the output of the ``certbot`` command, and as action specify a fixed response, response code 200, and pasting as text/plain the challenge data printed by the ``certbot`` command. Wait a few seconds, then confirm the Certbot prompt. The certificate will be issued and stored under ``/etc/letsencrypt/live/paris2024.sapsailing.com`` from where I copied it to ``/home/sailing/Downloads/letsencrypt`` on both laptops for later use with a local Apache httpd server. The certificate will expire on 2021-08-19, so after the Olympic Games, so we don't have to worry about renewing it.

### Local NGINX Webserver Setup

In order to be able to access the applications running on the local on-site laptops using HTTPS there is a web server on each of the two laptops, listening on port 9443 (HTTPS). The configuration for this is under ``/etc/nginx/sites-enabled/paris2024`` and looks like this:

```
server {
    listen              9443 ssl;
    server_name         paris2024.sapsailing.com;
    ssl_certificate     /etc/ssl/certs/paris2024.sapsailing.com.crt;
    ssl_certificate_key /etc/ssl/private/paris2024.sapsailing.com.key;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # set client body size to 100MB
    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:8888;
    }
}
```

The "Let's Encrypt"-provided certificate is used for SSL termination. With paris2024.sapsailing.com aliased in ``/etc/hosts`` to the address of the current master server, this allows accessing ``https://paris2024.sapsailing.com:9443`` with all benefits of cookie / session authentication.

Likewise, ``/etc/nginx/sites-enabled/security-service`` forwards to 127.0.0.1:8889 where a local copy of the security service may be deployed in case the Internet fails. In this case, the local port 443 must be forwarded to the NGINX port 9443 instead of security-service.sapsailing.com:443 through paris-ssh.sapsailing.com.

On sap-p1-1 is currently a nginx listening to paris2024-master.sapsailing.com with the following configuration:

```
server {
    listen              9443 ssl;
    server_name         paris2024-master.sapsailing.com;
    ssl_certificate     /etc/ssl/private/paris2024-master.sapsailing.com.fullchain.pem;
    ssl_certificate_key /etc/ssl/private/paris2024-master.sapsailing.com.privkey.pem;
    ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # set client body size to 100MB
    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:8888;
    }
}
```

### Backup

borgbackup is used to backup the ``/`` folder of both laptops towards the other machine. Folder where the borg repository is located is: ``/backup``.

The backup from sap-p1-1 to sap-p1-2 runs at 01:00 each day, and the backup from sap-p1-2 to sap-p1-1 runs at 02:00 each day. Details about the configuration can be found in ``/root/borg-backup.sh`` on either machine. Log files for the backup run are in ``/var/log/backup.log``. Crontab file is in ``/root``.

Both ``/backup`` folders have been mirrored to a S3 bucket called ``backup-sap-p1`` on June 14th 2021 and March 29th 2023.

### Monitoring and e-Mail Alerting

To be able to use ``mail`` to send notifications via email we use ``postfix`` which needs to be installed and configured to use the AWS SES as smtp relay:
```
sudo apt install postfix
```
During the installation process, select "Internet with smarthost", use ``sap-p1-[12].sapsailing.com`` as your system mail name, and for the SMTP relay host enter ``[email-smtp.eu-west-1.amazonaws.com]:588``.

The problem with AWS SES is that it seems to work reliably only when used from EC2 instances. Even a telnet connection from a non-EC2 instance will not work properly. Therefore, SES mail sending needs to work through another SSH port forward which points to ``email-smtp.eu-west-1.amazonaws.com:587``. Furthermore, the ``postfix`` mail transfer agent must be able to reach that port using the correct hostname because otherwise the certificate used for STARTTLS won't match. Therefore, we add a port forward from localhost 588 to email-smtp.eu-west-1.amazonaws.com:587 in our tunnel scripts.

Follow the instructions on [https://docs.aws.amazon.com/ses/latest/dg/postfix.html](https://docs.aws.amazon.com/ses/latest/dg/postfix.html) with the exception of the port number where instead of ``587`` you have to use ``588``, and use ``email-smtp.eu-west-1.amazonaws.com`` as the SMTP host. Furthermore, an entry in ``/etc/hosts`` is required, like this:

```
127.0.0.1       localhost email-smtp.eu-west-1.amazonaws.com
```
The authentication details that are required during the configuration of postfix according to the AWS documentation can be fetched from the content of ``/root/mail.properties`` of any running sailing EC2 instance.

Both laptops, ``sap-p1-1`` and ``sap-p1-2`` have monitoring scripts from the git folder ``configuration/on-site-scripts`` linked to ``/usr/local/bin``. These in particular include ``monitor-autossh-tunnels`` and ``monitor-mongo-replica-set-delay`` as well as a ``notify-operators`` script which contains the list of e-mail addresses to notify in case an alert occurs.

The ``monitor-autossh-tunnels`` script checks all running ``autossh`` processes and looks for their corresponding ``ssh`` child processes. If any of them is missing, an alert is sent using ``notify-operators``.

The ``monitor-mongo-replica-set-delay`` looks as the result of calling ``rs.printSecondaryReplicationInfo()`` and logs it to ``/tmp/mongo-replica-set-delay``. The average of the last ten values is compared to a threshold (currently 3s), and an alert is sent using ``notify-operators`` if the threshold is exceeded.

The ``monitor-disk-usage`` script checks the partition holding ``/var/lib/mongodb/``. Should it fill up to more than 90%, an alert will be sent using ``notify-operators``.

On ``sap-p1-2`` we run a script ``compare-secondary-to-primary-master`` every five minutes which basically does a ``compareServers -ael`` which uses the REST API for comparing server contents. If a difference is reported by the tool then an e-mail notification is sent out to the list of operators.

On ``paris-ssh.sapsailing.com`` we run ``monitor-on-site-servers`` every minute. Like other scripts it is installed as a symbolic link from ``/usr/local/bin`` to ``/root/code/configuration/on-site-scripts/paris2024/paris-ssh`` and that is called by a cron job from the ``root`` user's ``crontab`` file. The file checks that the primary and secondary master can be reached through HTTPS (localhost port 9443/9444, respectively), and the primary through HTTP, port 8888. This verifies that the SSH tunnels from the on-site laptops to the ``paris-ssh`` jump host are in place, up and running. Indirectly, this also verifies that the OpenVPN connections on the on-site laptops are working alright.

### Time Synchronizing
Setup chronyd service on desktop machine, in order to regurlary connect via VPN and relay the time towards the two P1s. Added
```
# Paris2024 configuration
server 10.1.3.221 iburst
```
to ``/etc/chrony/chrony.conf`` on the clients.
Added
```
# FOR PARIS SERVER SETUP
allow all
local stratum 10
```
to the server file, started ```chronyd``` service.

## AWS Setup

Our primary AWS region for the event will be Paris (eu-west-3). There, we have reserved the elastic IP ``13.39.66.118`` to which we've mapped the Route53 hostname ``paris-ssh.sapsailing.com`` with a simple A-record. The host assigned to the IP/hostname is to be used as a "jump host" for SSH tunnels. It runs Amazon Linux with a login-user named ``ec2-user``. The ``ec2-user`` has ``sudo`` permission. In the root user's crontab we have the same set of scripts hooked up that in our eu-west-1 production landscape is responsible for obtaining and installing the landscape manager's SSH public keys to the login user's account, aligning the set of ``authorized_keys`` with those of the registered landscape managers (users with permission ``LANDSCAPE:MANAGE:AWS``). The ``authorized_keys.org`` file also contains the two public SSH keys of the ``sailing`` accounts on the two laptops, so each time the script produces a new ``authorized_keys`` file for the ``ec2-user``, the ``sailing`` keys for the laptop tunnels don't get lost.

I added the EPEL repository like this:

```
   yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
```

Our "favorite" Availability Zone (AZ) in eu-west-3 is "1a" / "eu-west-3a".

The same host ``paris-ssh.sapsailing.com`` also runs a MongoDB 4.4 instance on port 10203.

For RabbitMQ we run a separate host, based on AWS Ubuntu 20. It brings the ``rabbitmq-server`` package with it (version 3.8.2 on Erlang 22.2.7), and we'll install it with default settings, except for the following change: In the new file ``/etc/rabbitmq/rabbitmq.conf`` we enter the line

```
    loopback_users = none
```

which allows clients from other hosts to connect (note how this works differently on different version of RabbitMQ; the local laptops have to use a different syntax in their ``rabbitmq.config`` file). The security groups for the RabbitMQ server are configured such that only ``172.0.0.0/8`` addresses from our VPCs can connect.

The RabbitMQ management plugin is enabled using ``rabbitmq-plugins enable rabbitmq_management`` for access from localhost. This will require again an SSH tunnel to the host. The host's default user is ``ubuntu``. The RabbitMQ management plugin is active on port 15672 and accessible only from localhost or an SSH tunnel with port forward ending at this host. RabbitMQ itself listens on the default port 5672. With this set-up, RabbitMQ traffic for this event remains independent and undisturbed from any other RabbitMQ traffic from other servers in our default ``eu-west-1`` landscape, such as ``my.sapsailing.com``. The hostname pointing to the internal IP address of the RabbitMQ host is ``rabbit-eu-west-3.sapsailing.com`` and has a timeout of 60s.

An autossh tunnel is established from ``paris-ssh.sapsailing.com`` to ``rabbit-eu-west-3.sapsailing.com`` which forwards port 15673 to port 15672, thus exposing the RabbitMQ web interface which otherwise only responds to localhost. This autossh tunnel is established by a systemctl service that is described in ``/etc/systemd/system/autossh-port-forwards.service`` in ``paris-ssh.sapsailing.com``.

### Local setup of rabbitmq

The above configuration needs also to be set on the rabbitmq installations of the P1s. The rabbitmq-server package has version 3.6.10. In that version the config file is located in ``/etc/rabbitmq/rabbitmq.config``, the entry is ``[{rabbit, [{loopback_users, []}]}].`` Further documentation for this version can be found here: [http://previous.rabbitmq.com/v3_6_x/configure.html](http://previous.rabbitmq.com/v3_6_x/configure.html)

### Cross-Region VPC Peering

The primary AWS region for the paris2024 replica set is eu-west-3 (Paris). In order to provide low latencies for the RHBs we'd like to add replicas also in other regions. Since we want to not expose the RabbitMQ running eu-west-3 to the outside world, we plan to peer the VPCs of other regions with the one in eu-west-3.

The pre-requisite for VPCs to get peered is that their CIDRs (such as 172.31.0.0/16) don't overlap. The default VPC in each region always uses the same CIDR (172.31.0.0/16), and hence in order to peer VPCs all but one must be non-default VPC. To avoid confusion when launching instances or setting up security groups it can be adequate for those peering regions other than our default region ``eu-west-1`` to set up non-default VPCs with peering-capable CIDRs and remove the default VPC. This way users cannot accidentally launch instances or define security groups for any VPC other than the peered one.

After having peered the VPCs, the VPCs default routing table must be extended by a route to the peered VPC's CIDR using the peering connection.

With peering in place it is possible to reach instances in peered VPCs by their internal IPs. In particular, it is possible to connect to a RabbitMQ instance with the internal IP and port 5672 even if that RabbitMQ runs in a different region whose VPC is peered.

### Global Accelerator

We have created a Global Accelerator [Paris2024](https://us-west-2.console.aws.amazon.com/ec2/v2/home?region=us-west-2#AcceleratorDetails:AcceleratorArn=arn:aws:globalaccelerator::017363970217:accelerator/TODO) which manages cross-region load balancing for us. There are two listeners: one for port 80 (HTTP) and one for port 443 (HTTPS). For each region an endpoint group must be created for both of the listeners, and the application load balancer (ALB) in that region has to be added as an endpoint.

The Route53 entry ``paris2024.sapsailing.com`` now is an alias A record pointing to this global accelerator (``TODO.awsglobalaccelerator.com.``).

### Geo-Blocking

While for Tokyo 2020 this was not requested, for Paris 2024 we heard rumors that it may. If it does, using the [AWS Web Application Firewall (WAF)](https://us-east-1.console.aws.amazon.com/wafv2/homev2/start) provides the solution. There, we can create so-called Web Access Control Lists (Web ACLs) which need to be created per region where an ALB is used.

A Web ACL consists of a number of rules and has a default action (typically "Allow" or "Block") for those requests not matched by any rule. An ACL can be associated with one or more resources, in particular with Application Load Balancers (ALBs) deployed in the region.

Rules, in turn, consist of statements that can be combined using logical operators. The rule type of interest for geo-blocking is "Originates from a country in" where one or more countries can be selected. When combined with an "Allow" or "Block" action, this results in the geo-blocking behavior desired.

For requests blocked by the rule, the response code, response headers and message body to return to the client can be configured. We can use this, e.g., to configure a 301 re-direct to a static page that informs the user about the geo-blocking.

### Application Load Balancers (ALBs) and Target Groups

In each region supported, a dedicated load balancer for the Global Accelerator-based event setup has been set up (``Paris2024ALB`` or simply ``ALB``). A single target group with the usual settings (port 8888, health check on ``/gwt/status``, etc.) must exist: ``S-paris2024`` (public).

Remember to activate *logging* for all Application Load Balancers. Each region requires its own S3 bucket. You may choose to re-use the tokyo2020 buckets. Once you need to create one or more new buckets for new regions, make sure to add them to the two Athena queries we use, copying the pattern you'll see for the ``tokyo2020`` log file buckets. Use the stored query "Create partitioned table for eu-west-1 logs" and modify accordingly to create a separate partitioned table for each new bucket you created for your regions, then add those tables to the cascade of ``select`` statements in those Athena queries:

```
select *
    from alb_log_partition_projection
    union all
    select *
    from alb_log_tokyo2020_ap_northeast_1_partition_projection
    ...
    union all
    select *
    from alb_log_{your-event-name}_{region-name}_partition_projection,
    ...
```

Note that no dedicated ``-m`` master target group is established. The reason is that the AWS Global Accelerator judges an ALB's health by looking at _all_ its target groups; should only a single target group not have a healthy target, the Global Accelerator considers the entire ALB unhealthy. With this, as soon as the on-site master server is unreachable, e.g., during an upgrade, all those ALBs would enter the "unhealthy" state from the Global Accelerator's perspective, and all public replicas which are still healthy would no longer receive traffic; the site would go "black." Therefore, we must ensure that the ALBs targeted by the Global Accelerator only have a single target group which only has the public replicas in that region as its targets.

Each ALB has an HTTP and an HTTPS listener. The HTTP listener has only a single rule redirecting all traffic permanently (301) to the corresponding HTTPS request. The HTTPS listener has three rules: the ``/`` path for ``paris2024.sapsailing.com`` is re-directed to the Olympic event with ID ``TODO``. All other traffic for ``paris2024.sapsailing.com`` goes to the public target group holding the regional replica(s). A default rule returns a 404 status with a static ``Not found`` text.

## Landscape Architecture

We have applied for a single SSH tunnel to IP address ``52.194.91.94`` which is our elastic IP for our SSH jump host in eu-west-3(d). 

The default production set-up is defined as follows:

### MongoDB

Three MongoDB nodes are intended to run during regular operations: sap-p1-1:10201, sap-p1-2:10202, and paris-ssh.sapsailing.com:10203. Since we have to work with SSH tunnels to keep things connected, we map everything using ``localhost`` ports such that both, sap-p1-2 and paris-ssh see sap-p1-1:10201 as their localhost:10201, and that both, sap-p1-1 and paris-ssh see sap-p1-2:10202 as their respective localhost:10202. Both, sap-p1-1 and sap-p1-2 see paris-ssh:10203 as their localhost:10203. This way, the MongoDB URI can be specified as

```
	mongodb://localhost:10201,localhost:10202,localhost:10203/paris2024?replicaSet=paris2024&retryWrites=true&readPreference=nearest
```

The cloud replica is not supposed to become primary, except for maybe in the unlikely event where operations would move entirely to the cloud. To achieve this, the cloud replica has priority 0 which can be configured like this:

```
    paris2024:PRIMARY> cfg = rs.conf()
    # Then search for the member localhost:10203; let's assume, it's in cfg.members[0] :
    cfg.members[0].priority=0
    rs.reconfig(cfg)
```

All cloud replicas shall use a MongoDB database name ``paris2024-replica``. In those regions where we don't have dedicated MongoDB support established (basically all but eu-west-1 currently), an image should be used that has a MongoDB server configured to use ``/home/sailing/mongo`` as its data directory and ``replica`` as its replica set name. See AMI SAP Sailing Analytics App HVM with MongoDB 1.137 (ami-05b6c7b1244f49d54) in eu-west-3 (already copied to the other peered regions except eu-west-1).

One way to monitor the health and replication status of the replica set is running the following command:

```
  watch 'echo "rs.printSecondaryReplicationInfo()" | \
  mongo "mongodb://localhost:10201/?replicaSet=paris2024&retryWrites=true&readPreference=nearest" | \
  grep "\(^source:\)\|\(syncedTo:\)\|\(behind the primary\)"'
```

It shows the replication state and in particular the delay of the replicas. A cronjob exists for ``sailing@sap-p1-1`` which triggers ``/usr/local/bin/monitor-mongo-replica-set-delay`` every minute which will use ``/usr/local/bin/notify-operators`` in case the average replication delay for the last ten read-outs exceeds a threshold (currently 3s). We have a cron job monitoring this (see above) and sending out alerts if things start slowing down.

In order to have a local copy of the ``security_service`` database, a CRON job exists for user ``sailing`` on ``sap-p1-1`` which executes the ``/usr/local/bin/clone-security-service-db-safe-exit`` script (versioned in git under ``configuration/on-site-scripts/paris2024/clone-security-service-db-safe-exit``) once per hour. See ``/home/sailing/crontab``. The script dumps ``security_service`` from the ``live`` replica set in ``eu-west-1`` to the ``/tmp/dump`` directory on ``ec2-user@paris-ssh.sapsailing.com`` and then sends the directory content as a ``tar.gz`` stream through SSH and restores it on the local ``mongodb://sap-p1-1:27017,sap-p1-2/security_service?replicaSet=security_service`` replica set, after copying an existing local ``security_service`` database to ``security_service_bak``. This way, even if the Internet connection dies during this cloning process, a valid copy still exists in the local ``paris2024`` replica set which can be copied back to ``security_service`` using the MongoDB shell command

```
    db.copyDatabase("security_service_bak", "security_service")
```

### Master

The master configuration on ``sap-p1-1`` is described in ``/home/sailing/servers/master/master.conf`` and can be used to produce a clean set-up like this:

```
      rm env.sh; cat master.conf | ./refreshInstance.sh auto-install-from-stdin
```

If the laptops cannot reach ``https://releases.sapsailing.com`` due to connectivity constraints, releases and environments can be downloaded through other channels to ``sap-p1-1:/home/trac/releases``, and the variable ``INSTALL_FROM_SCP_USER_AT_HOST_AND_PORT`` can be set to ``sailing@sap-p1-1`` to fetch the release file and environment file from there by SCP. Alternatively, ``sap-p1-2:/home/trac/releases`` may be used for the same.

This way, a clean new ``env.sh`` file will be produced from the config file, including the download and installation of a release. The ``master.conf`` file looks approximately like this:

```
INSTALL_FROM_RELEASE=build-202306271444
SERVER_NAME=paris2024
MONGODB_URI="mongodb://localhost:10201,localhost:10202,localhost:10203/${SERVER_NAME}?replicaSet=paris2024&retryWrites=true&readPreference=nearest"
# RabbitMQ in eu-west-1 (rabbit.internal.sapsailing.com) is expected to be found through SSH tunnel on localhost:5675
# Replication of shared services from central security-service.sapsailing.com through SSH tunnel 443:security-service.sapsailing.com:443
# with a local /etc/hosts entry mapping security-service.sapsailing.com to 127.0.0.1
REPLICATE_MASTER_QUEUE_HOST=localhost
REPLICATE_MASTER_QUEUE_PORT=5675
REPLICATE_MASTER_BEARER_TOKEN="***"
# Outbound replication to RabbitMQ through SSH tunnel with port forward on port 5673, regularly to rabbit-eu-west-3.sapsailing.com
# Can be re-mapped to the RabbitMQ running on sap-p1-2
REPLICATION_HOST=localhost
REPLICATION_PORT=5673
USE_ENVIRONMENT=live-master-server
ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dpolardata.source.url=https://www.sapsailing.com:22443 -Dwindestimation.source.url=https://www.sapsailing.com:22443"
ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sailing.omegatiming.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sailing\.omegatiming\.com(:[0-9]*)?$"
# Place additional secrets here, e.g., from root@sapsailing.com:secrets
MANAGE2SAIL_ACCESS_TOKEN=...
IGTIMI_CLIENT_ID=...
IGTIMI_CLIENT_SECRET=...
GOOGLE_MAPS_AUTHENTICATION_PARAMS="..."
```

### Secondary Master

The secondary master configuration on ``sap-p1-2`` can be used to fail over quickly if the primary master on ``sap-p1-1`` fails for some reason. The configuration is described in ``/home/sailing/servers/secondary_master/secondary_master.conf`` and can be used to produce a clean set-up like this:

```
      rm env.sh; cat secondary_master.conf | ./refreshInstance.sh auto-install-from-stdin
```

This way, a clean new ``env.sh`` file will be produced from the config file, including the download and installation of a release. The ``secondary_master.conf`` file looks approximately like this:

```
INSTALL_FROM_RELEASE=build-202306271444
SERVER_NAME=paris2024
MONGODB_URI="mongodb://sap-p1-1:27017,sap-p1-2:27017/${SERVER_NAME}?replicaSet=security_service&retryWrites=true&readPreference=nearest"
# RabbitMQ in eu-west-1 (rabbit.internal.sapsailing.com) is expected to be found through SSH tunnel on localhost:5675
# Replication of shared services from central security-service.sapsailing.com through SSH tunnel 443:security-service.sapsailing.com:443
# with a local /etc/hosts entry mapping security-service.sapsailing.com to 127.0.0.1
REPLICATE_MASTER_QUEUE_HOST=localhost
REPLICATE_MASTER_QUEUE_PORT=5675
REPLICATE_MASTER_BEARER_TOKEN="***"
# Outbound replication to RabbitMQ through SSH tunnel with port forward on port 5673, regularly to RabbitMQ on localhost,
# can be re-mapped to the cloud RabbitMQ running on rabbit-eu-west-3.internal.sapsailing.com to make this the "primary" master
REPLICATION_HOST=localhost
REPLICATION_PORT=5673
USE_ENVIRONMENT=live-master-server
ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dpolardata.source.url=https://www.sapsailing.com:22443 -Dwindestimation.source.url=https://www.sapsailing.com:22443"
ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sailing.omegatiming.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sailing\.omegatiming\.com(:[0-9]*)?$"
# Place additional secrets here, e.g., from root@sapsailing.com:secrets
MANAGE2SAIL_ACCESS_TOKEN=...
IGTIMI_CLIENT_ID=...
IGTIMI_CLIENT_SECRET=...
GOOGLE_MAPS_AUTHENTICATION_PARAMS="..."
```


### Replicas

We plan to run with two master nodes on premise ("primary" vs. "failover"). So it is not considered a standard use case to run a replica on site. If an on-site replica is still desired on ``sap-p1-2`` it can be configured with a ``replica.conf`` file in ``/home/sailing/servers/replica``, using

```
	rm env.sh; cat replica.conf | ./refreshInstance auto-install-from-stdin
```

The file looks like this:

```
# Regular operations; sap-p1-2 replicates sap-p1-1 using the rabbit-eu-west-3.sapsailing.com RabbitMQ in the cloud through SSH tunnel.
# Outbound replication, though not expected to become active, goes to a local RabbitMQ
INSTALL_FROM_RELEASE=build-202306271444
SERVER_NAME=paris2024
MONGODB_URI="mongodb://localhost:10201,localhost:10202,localhost:10203/${SERVER_NAME}-replica?replicaSet=paris2024&retryWrites=true&readPreference=nearest"
# RabbitMQ in eu-west-3 is expected to be found locally on port 5673
REPLICATE_MASTER_SERVLET_HOST=sap-p1-1
REPLICATE_MASTER_SERVLET_PORT=8888
REPLICATE_MASTER_QUEUE_HOST=localhost
REPLICATE_MASTER_QUEUE_PORT=5673
REPLICATE_MASTER_BEARER_TOKEN="***"
# Outbound replication to RabbitMQ running locally on sap-p1-2
REPLICATION_HOST=localhost
REPLICATION_PORT=5672
REPLICATION_CHANNEL=${SERVER_NAME}-replica
USE_ENVIRONMENT=live-replica-server
ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sailing.omegatiming.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sailing\.omegatiming\.com(:[0-9]*)?$"
# Place additional secrets here, e.g., from root@sapsailing.com:secrets
GOOGLE_MAPS_AUTHENTICATION_PARAMS="..."
```

(Adjust the release accordingly, of course). (NOTE: During the first production days of the event we noticed that it was really a BAD IDEA to have all replicas use the same DB set-up, all writing to the MongoDB PRIMARY of the "live" replica set in eu-west-1. With tens of replicas running concurrently, this led to a massive block-up based on MongoDB not writing fast enough. This gave rise to a new application server AMI which now has a MongoDB set-up included, using "replica" as the MongoDB replica set name. Now, each replica hence can write into its own MongoDB instance, isolated from all others and scaling linearly.)

In the EC2 regions, instead an instance-local MongoDB is used for each replica, not interfering with each other or with other databases:

```
INSTALL_FROM_RELEASE=build-202306271444
SERVER_NAME=paris2024
MONGODB_URI="mongodb://localhost/paris2024-replica?replicaSet=replica&retryWrites=true&readPreference=nearest"
USE_ENVIRONMENT=live-replica-server
REPLICATION_CHANNEL=paris2024-replica
REPLICATION_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_SERVLET_HOST=paris-ssh.internal.sapsailing.com
REPLICATE_MASTER_SERVLET_PORT=8888
REPLICATE_MASTER_EXCHANGE_NAME=paris2024
REPLICATE_MASTER_QUEUE_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_BEARER_TOKEN="***"
ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sailing.omegatiming.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\.)?sailing\.omegatiming\.com(:[0-9]*)?$"
# Place additional secrets here, e.g., from root@sapsailing.com:secrets
GOOGLE_MAPS_AUTHENTICATION_PARAMS="..."
```

### Application Servers

``sap-p1-1`` normally is the master for the ``paris2024`` replica set. The application server directory is found under ``/home/sailing/servers/master``, and the master's HTTP port is 8888. It shall replicate the shared services, in particular ``SecurityServiceImpl``, from ``security-service.sapsailing.com``, like any normal server in our landscape, only that here we have to make sure we can target the default RabbitMQ in eu-west-1 and can see the ``security-service.sapsailing.com`` master directly or even better the load balancer.

SSH local port forwards (configured with the ``-L`` option) that use hostnames instead of IP addresses for the remote host specification are resolved each time a new connection is established through this forward. If the DNS entry resolves to multiple IPs or if the DNS entry changes over time, later connection requests through the port forward will honor the new host name's DNS resolution.

Furthermore, there is a configuration under ``/home/sailing/servers/security_service`` which can be fired up with port 8889, using the local ``security_service`` database that a script ``/usr/local/bin/clone-security-service-db`` on the jump host ``paris-ssh.sapsailing.com`` updates on an hourly basis as long as an Internet connection is available. This can be used as a replacement of the official ``security-service.sapsailing.com`` service. Both laptops have an ``/etc/hosts`` entry mapping ``security-service.sapsailing.com`` to ``127.0.0.1`` and work with flexible SSH port forwards to decide whether the official Internet-based or the local copy of the security service shall be used.

``sap-p1-2`` normally is a replica for the ``paris2024`` replica set, using the local RabbitMQ running on ``sap-p1-1``. Its outbound ``REPLICATION_CHANNEL`` will be ``paris2024-replica`` and uses the RabbitMQ running in ``eu-west-3``, using an SSH port forward with local port 5673 for the ``eu-west-3`` RabbitMQ (15673 for the web administration UI). A reverse port forward from ``eu-west-3`` to the application port 8888 on ``sap-p1-2`` has to be established which replicas running in ``eu-west-3`` will use to reach their master through HTTP. This way, adding more replicas on the AWS side in the cloud will not require any additional bandwidth between cloud and on-site network, except that the reverse HTTP channel, which uses only little traffic, will see additional traffic per replica whereas all outbound replication goes to the single exchange in the RabbitMQ node running in ``eu-west-3``.

## User Groups and Permissions

The general public shall not be allowed during the live event to browse the event through ``paris2024.sapsailing.com``. Instead, they are required to go through any of the so-called "Rights-Holding Broadcaster" (RHB) web sites. There, a "widget" will be embedded into their web sites which works with our REST API to display links to the regattas and races, in particular the RaceBoard.html pages displaying the live and replay races.

Moderators who need to comment on the races shall be given more elaborate permissions and shall be allowed to use the full-fledged functionality of ``paris2024.sapsailing.com``, in particular, browse through all aspects of the event, see flag statuses, postponements and so on.

To achieve this effect, the ``paris2024-server`` group has the ``sailing_viewer`` role assigned for all users, and all objects, except for the top-level ``Event`` object are owned by that group. This way, everything but the event are publicly visible.

The ``Event`` object is owned by ``paris2024-moderators``, and that group grants the ``sailing_viewer`` role only to its members, meaning only the members of that group are allowed to see the ``Event`` object.

## Landscape Upgrade Procedure

In the ``configuration/on-site-scripts/paris2024`` we have prepared a number of scripts intended to be useful for local and cloud landscape management. TL;DR:
```
	configuration/on-site-scripts/upgrade-landscape.sh -R {release-name} -b {replication-bearer-token}
```
will upgrade the entire landscape to the release ``{release-name}`` (e.g., build-202107210711). The ``{replication-bearer-token}`` must be provided such that the user authenticated by that token will have the permission to stop replication and to replicate the ``paris2024`` master.

The script will proceed in the following steps:
 - patch ``*.conf`` files in ``sap-p1-1:servers/[master|security_service]`` and ``sap-p1-2:servers/[secondary_master|replica|master|security_service]`` so
   their ``INSTALL_FROM_RELEASE`` points to the new ``${RELEASE}``
 - Install new releases to ``sap-p1-1:servers/[master|security_service]`` and ``sap-p1-2:servers/[secondary_master|replica|master|security_service]``
 - Update all launch configurations and auto-scaling groups in the cloud (``update-launch-configuration.sh``)
 - Tell all replicas in the cloud to stop replicating (``stop-all-cloud-replicas.sh``)
 - Tell ``sap-p1-2:servers/secondary_master`` to restart (./stop; ./start)
 - on ``sap-p1-1:servers/master`` run ``./stop; ./start`` to bring the master to the new release
 - wait until master is healthy
 - launch upgraded cloud replicas and replace old replicas in target group (``launch-replicas-in-all-regions.sh``)
 - terminate all instances named "SL Paris2024 (auto-replica)"; this should cause the auto-scaling group to launch new instances as required
 - manually inspect the health of everything and terminate the "SL Paris2024 (Upgrade Replica)" instances when enough new instances
   named "SL Paris2024 (auto-replica)" are available

The individual scripts will be described briefly in the following sub-sections. Many of them use as a common artifact the ``regions.txt`` file which contains the list of regions in which operations are executed. The ``eu-west-1`` region as our "legacy" or "primary" region requires special attention in some cases. In particular, it can use the ``live`` replica set for the replicas started in the region, also because the AMI used in this region is slightly different and in particular doesn't launch a MongoDB local replica set on each instance which the AMIs in all other regions supported do.

### clone-security-service-db-safe-exit

Creates a ``mongodump`` of "mongodb://mongo0.internal.sapsailing.com,mongo1.internal.sapsailing.com,dbserver.internal.sapsailing.com:10203/security_service?replicaSet=live&retryWrites=true&readPreference=nearest" on the ``paris-ssh.sapsailing.com`` host and packs it into a ``.tar.gz`` file. This archive is then transferred as the standard output of an SSH command to the host executing the script where it is unpacked into ``/tmp/dump``. The local "mongodb://localhost/security_service_bak?replicaSet=security_service&retryWrites=true&readPreference=nearest" backup copy is then dropped, the local ``security_service`` DB is moved to ``security_service_bak``, and the dump from ``/tmp/dump`` is then restored to ``security_service``. If this fails, the backup from ``security_service_bak`` is restored to ``security_service``, and there won't be a backup copy anymore in ``security_service_bak`` anymore.

The script is used as a CRON job for user ``sailing@sap-p1-1``.

### get-replica-ips

Lists the public IP addresses of all running replicas in the regions described in ``regions.txt`` on its standard output. Progress information will be sent to standard error. Example invocation:
<pre>
	$ ./get-replica-ips
	Region: eu-west-3
	Region: ap-northeast-1
	Region: ap-southeast-2
	Region: us-west-1
	Region: us-east-1
	 34.245.148.130 18.183.234.161 3.26.60.130 13.52.238.81 18.232.169.1
</pre>

### launch-replicas-in-all-regions.sh

Will launch as many new replicas in the regions listed in ``regions.txt`` with the release specified with ``-R`` as there are currently healthy auto-replicas registered with the ``S-paris2024`` target group in the region (at least one) which will register at the master proxy ``paris-ssh.internal.sapsailing.com:8888`` and RabbitMQ at ``rabbit-eu-west-3.sapsailing.com:5672``, then when healthy get added to target group ``S-paris2024`` in that region, with all auto-replicas registered before removed from the target group.

The script uses the ``launch-replicas-in-region.sh`` script for each region where replicas are to be launched.

Example invocation:
<pre>
	launch-replicas-in-all-regions.sh -R build-202107210711 -b 1234567890ABCDEFGH/+748397=
</pre>

Invoke without arguments to see a documentation of possible parameters.

### launch-replicas-in-region.sh

Will launch one or more (see ``-c``) new replicas in the AWS region specified with ``-g`` with the release specified with ``-R`` which will register at the master proxy ``paris-ssh.internal.sapsailing.com:8888`` and RabbitMQ at ``rabbit-eu-west-3.sapsailing.com:5672``, then when healthy get added to target group ``S-paris2024`` in that region, with all auto-replicas registered before removed from the target group. Specify ``-r`` and ``-p`` if you are launching in ``eu-west-1`` because it has a special non-default MongoDB environment.

Example invocation:
<pre>
	launch-replicas-in-region.sh -g us-east-1 -R build-202107210711 -b 1234567890ABCDEFGH/+748397=
</pre>

Invoke without arguments to see a documentation of possible parameters.

### stop-all-cloud-replicas.sh

Will tell all replicas in the cloud in those regions described by the ``regions.txt`` file to stop replicating. This works by invoking the ``get-replica-ips script`` and for each of them to stop replicating, using the ``stopReplicating.sh`` script in their ``/home/sailing/servers/paris2024`` directory, passing through the bearer token. Note: this will NOT stop replication on the local replica on ``sap-p1-2``!

The script must be invoked with the bearer token needed to authenticate a user with replication permission for the ``paris2024`` application replica set.

Example invocation:
<pre>
	stop-all-cloud-replicas.sh -b 1234567890ABCDEFGH/+748397=
</pre>

Invoke without arguments to see a documentation of possible parameters.

### update-launch-configuration.sh

Will upgrade the auto-scaling group ``paris2024*`` (such as ``paris2024-auto-replicas``) in the regions from ``regions.txt`` with a new launch configuration that will be derived from the existing launch configuration named ``paris2024-*`` by copying it to ``paris2024-{RELEASE_NAME}`` while updating the ``INSTALL_FROM_RELEASE`` parameter in the user data to the ``{RELEASE_NAME}`` provided in the ``-R`` parameter, and optionally adjusting the AMI, key pair name and instance type if specified by the respective parameters. Note: this will NOT terminate any instances in the target group!

Example invocation:
<pre>
	update-launch-configuration.sh -R build-202107210711
</pre>

Invoke without arguments to see a documentation of possible parameters.

### upgrade-landscape.sh

See the introduction of this main section. Synopsis:
<pre>
  ./upgrade-landscape.sh -R &lt;release-name&gt; -b &lt;replication-bearer-token&gt; \[-t &lt;instance-type&gt;\] \[-i &lt;ami-id&gt;\] \[-k &lt;key-pair-name&gt;\] \[-s\]<br>
	-b replication bearer token; mandatory
	-i Amazon Machine Image (AMI) ID to use to launch the instance; defaults to latest image tagged with image-type:sailing-analytics-server
	-k Key pair name, mapping to the --key-name parameter
	-R release name; must be provided to select the release, e.g., build-202106040947
	-t Instance type; defaults to
	-s Skip release download
</pre>

## Log File Analysis

Athena table definitions and queries have been provided in region ``eu-west-3`` (Paris) where we hosted our EU part during the event after a difficult start in ``eu-west-1`` with the single MongoDB live replica set not scaling well for all the replicas that were required in the region.

The key to the Athena set-up is to have a table definition per bucket, with a dedicated S3 bucket per region where ALB logs were recorded. An example of a query based on the many tables the looks like this:
<pre>
    with union_table AS 
        (select *
        from alb_logs_ap_northeast_1
        union all
        select *
        from alb_logs_ap_southeast_2
        union all
        select *
        from alb_logs_eu_west_3
        union all
        select *
        from alb_logs_us_east_1
        union all
        select *
        from alb_logs_us_west_1)
    select date_trunc('day', parse_datetime(time,'yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z')), count(distinct concat(client_ip,user_agent))
    from union_table
    where (parse_datetime(time,'yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z')
        between parse_datetime('2021-07-21-00:00:00','yyyy-MM-dd-HH:mm:ss')
            and parse_datetime('2021-08-08-02:00:00','yyyy-MM-dd-HH:mm:ss'))
    group by date_trunc('day', parse_datetime(time,'yyyy-MM-dd''T''HH:mm:ss.SSSSSS''Z'))
</pre>
It defines a ``union_table`` which unites all contents from all buckets scanned.

## Dismantling the Set-Up and Moving to the Cloud

The MongoDB on ``paris-ssh.sapsailing.com:10203`` needs to be made primary, setting its ``priority`` in the replica set configuration from 0 to 1:

```
  cfg=rs.config()
  cfg.members[0].priority=1
  rs.reconfig(cfg)
```

Then the local replicas need to be unregistered:

```
  rs.remove("localhost:10201")
  rs.remove("localhost:10202")
```

The MongoDB cloud replica should then become the sole instance and hence primary of the replica set.

From some instance in the ``eu-west-1`` region we can then do a ``mongodump`` of the ``paris2024`` database and subsequently a ``mongorestore`` into the ``live`` replica set at ``mongodb://dbserver.internal.sapsailing.com:10203,mongo0.internal.sapsailing.com,mongo1.internal.sapsailing.com/paris2024?replicaSet=live&retryWrites=true&readPreference=nearest``. Then, a new replica ``paris2024`` set can be launched using our automated tools. Use the dynamic load balancer set-up to go without a DNS entry. Once the set-up is up and running, remove the DNS entry for ``paris2024.sapsailing.com`` so the traffic no longer goes to the global accelerator but to the dynamic ALB in ``eu-west-1``. See how the global accelerator traffic drains and start removing regions from it one by one. Finally, disable and remove the entire global accelerator.

Clean up the regions, setting all Auto-Scaling Groups to 0 instances, terminate all instances, remove the ALBs in the regions, then remove the ``paris2024`` target groups in the regions.

Later you may want to use the regular "Archive Replica Set" functionality to move the content to the archive server.