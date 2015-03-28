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
out.print((String)request.getAttribute("siteMsg"));
%>
</h1>
<hr/>

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
out.println("Server executing the client request: " 
		+ (String)request.getAttribute("siteSvrIDRequest") + "<br>");
out.println("Session data is found in: " + (String)request.getAttribute("siteSvrIDFound") 
		+ " , this is " + (String)request.getAttribute("siteSvrIDFoundPB") + "<br><br>");

out.println("Session Information" + "<br>");
out.println("SvrID_primary: " + (String)request.getAttribute("siteSvrIDPrimary") + "<br>");
out.println("SvrID_backup: " + (String)request.getAttribute("siteSvrIDBackup") + "<br>");
out.println("Expiration Time: " + (String)request.getAttribute("siteExpirationTime") + "<br>");
out.println("Discard Time: " + (String)request.getAttribute("siteDiscardTime") + "<br><br>");

out.println("View Information" + "<br>");
out.println((String)request.getAttribute("siteView"));
%>
</p>

</body>
</html>