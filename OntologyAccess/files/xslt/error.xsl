<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="no" />

	<xsl:template match="/">
		
			<xsl:apply-templates select="errorStatus" />
		
	</xsl:template>
	
	<xsl:template match="errorStatus">
		<error>
			<xsl:copy-of select="accessedResource"/>
			<xsl:copy-of select="shortMessage"/>
			<xsl:copy-of select="longMessage"/>
			<xsl:copy-of select="errorCode"/>
		</error>
	</xsl:template>
	
</xsl:stylesheet>

