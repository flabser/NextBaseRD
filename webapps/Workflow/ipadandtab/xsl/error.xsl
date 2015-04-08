<?xml version="1.0" encoding="windows-1251"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
	<xsl:output method="html" encoding="windows-1251"/>
	<xsl:template match="/content/error">
		<html>
			<head>
				<title>SmartDoc - ������</title>						
				<link rel="stylesheet" href="css/main.css" />	
				<link rel="stylesheet" href="css/actionbar.css" />			
				<script>
					<![CDATA[
						function CancelForm(){
			   	  			window.history.back();
						}
						function goToLogin(){
					 		window.location = "Provider?type=static&id=start";					
						}
					]]>
				</script>
			</head>
			<body>
				<font size="6">������</font>&#xA0;
				<br/>				
				<hr color="#6790b3"/>
				<br/>
				<font size="3">
					<xsl:choose>
						<xsl:when test="@type = 'authfailed'">������ ����������������</xsl:when>
					</xsl:choose>	
				</font>
				<br/>
				<div style="font-size:1.5em">	
					�������� ������
				</div>
				<div style="font-size:1em">	
					<xsl:value-of select="message"/>
				</div>
				<div style="font-size:1em;margin-top:20px;margin-bottom:20px">	
					<xsl:value-of select="stack"/>
				</div>
				<span style="border:1px dotted gray;width:auto; padding:3px; cursor:pointer">	
					<a>
						<xsl:choose>
							<xsl:when test="@type = 'authfailed'">
								<xsl:attribute name="href">javascript:goToLogin()</xsl:attribute>
							</xsl:when>
							<xsl:otherwise>
								<xsl:attribute name="href">javascript:CancelForm()</xsl:attribute>
							</xsl:otherwise>												
						</xsl:choose>	
						<img src="/SharedResources/img/classic/cancel.gif" style="border:0; "></img>
						<font  class="button">�������</font>
					</a>
				</span>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>