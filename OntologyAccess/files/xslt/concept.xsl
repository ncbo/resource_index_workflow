<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="no" />

	 <xsl:include href="common.xsl" />
	
	<xsl:template match="/success">
		 <xsl:apply-templates select="data/classBean" /> 
	</xsl:template>
	
	<xsl:template match="classBean">
		<xsl:call-template name="fullConceptEntity" /> 
	</xsl:template>

</xsl:stylesheet>
