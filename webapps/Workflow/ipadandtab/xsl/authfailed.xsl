<?xml version="1.0" encoding="windows-1251"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
	<xsl:output method="html" encoding="windows-1251"/>
	<xsl:template match="/content">
		<html>
			<head>
				<title>SmartDoc - ������</title>						
				<link rel="stylesheet" href="sdcss/main.css" />	
				<link rel="stylesheet" href="sdcss/actionbar.css" />			
			</head>
			<body>
				<font size="6">������</font>&#xA0;
				<br/>				
				<hr color="#6790b3"/>
				<br/>
				<font size="3">
				<xsl:choose>
					<xsl:when test="@type = 'authfailed'">������ �����������</xsl:when>
				</xsl:choose>	
				</font>
				<br/>
				<div style="font-size:1.5em">	
					������������ �� ������ � ��������� �����������
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>