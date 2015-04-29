<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="yes" />

	<xsl:template match="/success">
		 <xsl:apply-templates select="data/list"/>
	</xsl:template>

	<xsl:template match="list">
		<ontologyList>
			<ontologies>
				<xsl:for-each select="ontologyBean">
					<xsl:call-template name="ontologyBean" />
				</xsl:for-each>
			</ontologies>
		</ontologyList>
	</xsl:template>
	
	<xsl:template name="ontologyBean">
		<lightOntology>
			<xsl:copy-of select="id" />
			<xsl:copy-of select="ontologyId" />
			<xsl:copy-of select="versionNumber" />
			<xsl:copy-of select="displayLabel"/>
			<xsl:copy-of select="statusId" />
			<xsl:copy-of select="format" />
		</lightOntology>
	</xsl:template>
	
</xsl:stylesheet>
