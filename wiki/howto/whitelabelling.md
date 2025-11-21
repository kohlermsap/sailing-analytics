Changes regarding whitellabelling are collected here:


Runtime configuration of debranding:

1. The activation of the debranding could be done by providing a system property during startup of the server side application, e.g. -Dcom.sap.sse.branding=SAP

2. The debranding switch is delivered to the clients within the startup pages.
Therefore we can put placeholders inside the existing static pages and deliver them through servlets to the client. The servlets will process the placeholders in the pages. The debranding switch will show up in the page. The GWT client can read this information during startup in the browser. The URLs for loading the pages are unchanged.