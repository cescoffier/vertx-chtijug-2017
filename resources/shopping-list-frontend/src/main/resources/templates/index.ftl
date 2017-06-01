<html>
<head>
    <title>Welcome!</title>
</head>
<body>
<h1>Welcome!</h1>
<p>Shopping list:
<ul>
    <#list list as key, value>
        <li>${key} - ${value}</li>
    </#list>
</ul>
</body>
</html>