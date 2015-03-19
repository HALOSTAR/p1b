<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>UI</title>
</head>

<body>
<h1>
<%
out.print(((String)request.getAttribute("msg")).toString());
%>
</h1>
<hr/>

<%
if (false == ((Boolean)request.getAttribute("isLogout")).booleanValue()) {
%>

<form name = "response" action = "EnterServlet" method = "post">
<input type = "hidden" name = "pagename" value = "response"/>
<table cellpadding = "5" cellspacing = "5">
<tr>
	<td><button type = "submit" name = "button" value = "buttonReplace">Replace</button></td>
	<td><input type = "text" name = "txtReplace"/></td>
</tr>
<tr>
	<td><button type = "submit" name = "button" value = "buttonRefresh">Refresh</button></td>
</tr>
<tr>
	<td><button type = "submit" name = "button" value = "buttonLogout">Logout</button></td>
</tr>
</table>
</form>	

<p>
<%
out.println("Session Expiration Time: " + ((String)request.getAttribute("curSessionExpiration")).toString());
%>
</p>
<p> 
<%
out.println(((String)request.getAttribute("curCookieInfo")).toString());
%>
</p>

<%
}
%>

</body>
</html>