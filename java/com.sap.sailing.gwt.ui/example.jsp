<%@page import="java.util.ArrayList"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
 
<html>
<head>
<title>Java Code Geeks Snippets - Sample JSP Page</title>
<meta>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
</meta>
</head>
 
<body>
    <c:out value="Jetty JSP Example, remove it when everything is set up."></c:out>
    <br /> 
    Current date is: <%=new java.util.Date()%>
    ${requestScope['org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern']}
</body>
</html>