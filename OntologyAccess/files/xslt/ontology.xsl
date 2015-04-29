<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="no" />

	<xsl:template match="/success">
		 <xsl:apply-templates select="data/ontologyBean" /> 
	</xsl:template>
	
	<xsl:template match="ontologyBean">
		<fullOntology>
			<xsl:copy-of select="id" />
			<xsl:copy-of select="ontologyId" />
			<xsl:copy-of select="versionNumber" />
			<xsl:copy-of select="displayLabel"/>
			<xsl:copy-of select="statusId" />
			<xsl:copy-of select="format" />
			<xsl:copy-of select="oboFoundryId" />
			<xsl:copy-of select="contactName" />
			<xsl:copy-of select="contactEmail" />
			<xsl:copy-of select="homepage" />
			<xsl:copy-of select="documentation" />
		</fullOntology>
	</xsl:template>

</xsl:stylesheet>
