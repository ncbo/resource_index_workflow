<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="yes" />

	<xsl:template match="/success">
		 <xsl:apply-templates select="data/page/contents/searchResultList"/>
	</xsl:template>
	
	<xsl:template match="searchResultList">
		<conceptList>
			<concepts>
				<xsl:for-each select="searchBean">
					<xsl:call-template name="searchBeanEntity" />
				</xsl:for-each>
			</concepts>
		</conceptList>
	</xsl:template>
	
	<xsl:template name="searchBeanEntity">
		<lightConcept>
			<id>
				<xsl:copy-of select="conceptId/text()"/>
			</id>
			<label>
				<xsl:copy-of select="preferredName/text()"/>
			</label>
		</lightConcept>
	</xsl:template>
	
</xsl:stylesheet>
