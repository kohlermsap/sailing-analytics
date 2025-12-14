# Key tools and terms...
A basic rundown of *what's what*, so you can dive into the other documentation more easily. 

## The components of the project
### Tools
- Apache: This is the web server technology that acts as the reverse proxy and serves the content. You can serve multiple sites from the same Apache instance. It is configured using the `httpd.conf` file which can be found for this project in `${GIT_HOME}/configuration/httpd/conf`. Sometimes `.htaccess` files are used in a directory to affect the configuration; however, if you have sufficient permissions, try to place these in the main config file.  Apache works by using directives, which are commands to control access, security, sources and redirect traffic, to name but a few. Apache is modular and you can view all the modules compiled using `httpd -l`. The extra modules can be configured in conf.d (subdir of the configuration directory). In that directory, you can find the macros used, including those used for archiving.
- EC2: This is the Amazon Web Service that hosts the instances that the servers run on. We use load balancers to direct traffic, specifically Application Load Balancers, which routes at the HTTP/S level, making them very powerful. You might also want to read up on security groups, target groups and VPCs (virtual private clouds).
- Expedition Connector: A software package that allows us to receive wind sensor data and forward it on for processing.
- GWT: This is the product that handles the front end. It was originally Python, but we switched because it allowed for one language across the codebase, one editor (Eclipse) and dynamic sites. It is very modular and the details of each module are defined in the `.gwt.xml` file. Note: I had some trouble getting this working with Eclipse initially. I used a helpful tutorial called *Modern GWT, first steps*.
- Hudson: This is a continuous integration tool, written in Java, that can execute Maven, Ant, Unix and Windows commands. The general community now uses Jenkins and support has stopped.
- MongoDB: A NoSQL database (so there are no tables) that uses a Document model: within each database, you can have multiple collections and within those are documents... Each document has a `_id` which must be unique. You can access MongoDB from the terminal, using Mongosh, or you can use the GUI Compass. MongoDB scales horizontally by sharding (a bit like partitioning) the documents: this is done using a field or fields to order the documents. We use MongoDB for recovery. Note: As of writing, the pacing of their own tutorials is quite slow and they abstract a lot of detail.
- OSGI: A specification for component and module-based programming in Java. Equinox is an implementation of this specification, that we use. There is a MANIFEST.MF for each independent group of classes, known as a bundle, which details the requirements and any exports. These can be added, removed or updated for an application whilst it is running. We use OSGI web bundles which include a `web.xml` descriptor in the target WEB-INF, which can store the static content and any servlets.
- RabbitMQ: This is a message-passing service. Note: Their tutorials are great and there seems to be good documentation. Pika Python is good for getting familiar with the concepts.
- Selenium: It is a project that includes the tool we use for automated testing.
- Shiro: A Java security framework for permissions, authorization and authentication.
### Companies
- SwissTiming: They provide the timing solution at events but ultimately decided to outsource sailing timings to us and TracTrac. They have backup wind sensors if ours fail.
- TracTrac: The tracking gear used and the client used to interact with the backend. The Domain Factory maps the TracTrac domain to ours. The client runs a thread for receiving the race course definition, the list of competitors, the raw competitor GPS fixes, the mark positions, start/finish times and the mark-rounding times.


## Some basic sailing terminology
- A **flight** or **fleet** is a subset of all the competitors. The competitors are broken up into fleets if there are too many or if there are special starting and finishing provisions. On the sapsailing.com leaderboard, the fleets are colour-coded.
- A **jibe** or **gybe** is a manoeuvre whereby the boat points downwind (with the wind) during the turn.
- **kt** is knots.
- A **mark** is any position a boat must pass on a required side.
- A **tack** is a manoeuvre where the boat turns into the headwind (pointing into the wind) and then through.
- **Velocity made good** is speed towards or from the wind direction.
- Races, within a regatta, may be **discarded**. This is completely different to a penalty. If there is a discard, then each team's worst score is removed (if a team has two races with identical scores, the leaderboard will remove the earliest score). This typically only happens if there are 4 or more races. In the SAP Admin Console, in the Leaderboards panel, you can choose how many discards are allowed. We allow up to 15 discards! The Administrator types into the box next to the discard number, how many races are required to trigger the discard. This must increase for each discard.


## SuperDevMode vs DevMode (for GWT)
Previously there was a browser called Netscape, they developed a technology called Netscape Plugin Application Programming Interface (NPAPI), which was an API, allowing plugin integration. When the browser couldn't handle content, it utilised plugins. In GWT DevMode, we use this feature and have a plugin that acts as an in-between for the JavaScript and Java. However, all requests first make a call to the Java routine, which then does a roundtrip (usually) to set anything in the page, and finally returns. This is quite slow and the NPAPI stuff has been deprecated. Instead, we use SuperDevMode now. You still have a HTTP server which serves the content initially, is the back-end and connects with the AWS instances; this triggers a request for the nocache.js, to the SuperDev server (typically running on port 9876 with Windows), which transpiles the code into "draft" JavaScript (which is unoptimized), utilising source maps, which enable us to map the JavaScript back to readable Java code (as it is often optimised to the point of confusion). The AJAX response is then used to fill the site.
This new method allows debugging on more platforms and is fast; however, it does make debugging more difficult because  it can be unclear where the breakpoints map to.
