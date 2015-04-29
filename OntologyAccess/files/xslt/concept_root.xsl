<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="no" />

    <xsl:include href="common.xsl" />

	<xsl:template match="/success">
		 <xsl:apply-templates select="data/classBean/relations/entry"/>
	</xsl:template>
	
	<xsl:template match="entry">
		<xsl:if test="string='SubClass'">
			<conceptList>
				<concepts>
					<xsl:apply-templates select="list"/>
				</concepts>
			</conceptList>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="list/classBean">
		<xsl:call-template name="lightConceptEntity"/>
	</xsl:template>
	
</xsl:stylesheet>
