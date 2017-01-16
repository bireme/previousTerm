<%-- 
    Document   : index
    Created on : 29/11/2012, 12:24:00
    Author     : Heitor Barbieri
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Previous Term Servlet</title>
    </head>
    <body>
        <h1>Previous Term Servlet</h1>
        
        <p>Retrives n (next/previous) terms from a Lucene Index starting from a
        given term.</p>
        <p>Get parameters are:</p>
        <p>index=&lt;Lucene index name&gt; <br/>init=&lt;initial key&gt; <br/>fields=&lt;field1,field2,...&gt; <br/>[direction=&lt;'next' or 'previous'&gt;] <br/>[maxTerms=&lt;max returned keys&gt;]</p>
        <p>Example:</p>
        <p>/PreviousTermServlet?index=lil&init=baar&direction=previous&fields=tit,abs</p>
    </body>
</html>
