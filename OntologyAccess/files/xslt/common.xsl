<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="no" indent="no" />

	<xsl:template name="lightConceptEntity">
		<lightConcept>
			<xsl:copy-of select="id"/>
			<xsl:copy-of select="label"/>
		</lightConcept>
	</xsl:template>
	
	<xsl:template name="fullConceptEntity">
		<fullConcept>
			<xsl:copy-of select="id"/>
			<xsl:copy-of select="label"/>
			<xsl:for-each select="relations/entry">
				<xsl:call-template name="relations">
					<xsl:with-param name="currentNode" select="."/>
				</xsl:call-template>
			</xsl:for-each>
		</fullConcept>
	</xsl:template>

<xsl:template name="relations">
	<xsl:param name="currentNode"/>
	<xsl:variable name='exactSynonym' select="'EXACT SYNONYM'"/>
	<xsl:variable name='narrowSynonym' select="'NARROW SYNONYM'"/>
	<xsl:variable name='broadSynonym' select="'BROAD SYNONYM'"/>
	<xsl:variable name='relatedSynonym' select="'RELATED SYNONYM'"/>
	<xsl:variable name='bpSynonym' select="'BP_Synonym'"/>
	<xsl:variable name='superClass' select="'SuperClass'"/>
	<xsl:variable name='subClass' select="'SubClass'"/>
	
	<xsl:if test="string/text() = $superClass">  
		<superClass>
			<xsl:for-each select="list/classBean">
				<xsl:call-template name="lightConceptEntity" />
			</xsl:for-each>
		</superClass>
   </xsl:if>
	
	<xsl:if test="string/text() = $subClass">  
		<subClass>
			<xsl:for-each select="list/classBean">
				<xsl:call-template name="lightConceptEntity" />
			</xsl:for-each>
		</subClass>
   </xsl:if>
	
    <xsl:if test="string/text() = $exactSynonym">  
		<exactSynonyms>
			<xsl:copy-of select="list/*"/>
		</exactSynonyms>
   </xsl:if>

	<xsl:if test="string/text() = $narrowSynonym">  
		<narrowSynonyms>
			<xsl:copy-of select="list/*"/>
		</narrowSynonyms>
   </xsl:if>

	<xsl:if test="string/text() = $broadSynonym">  
		<broadSynonyms>
			<xsl:copy-of select="list/*"/>
		</broadSynonyms>
   </xsl:if>

	<xsl:if test="string/text() = $relatedSynonym">  
		<relatedSynonyms>
			<xsl:copy-of select="list/*"/>
		</relatedSynonyms>
   </xsl:if>

	<xsl:if test="string/text() = $bpSynonym">  
		<bpSynonyms>
			<xsl:copy-of select="java.util.Collections_-UnmodifiableCollection/c/*"/>
		</bpSynonyms>
   </xsl:if>
		
</xsl:template>

</xsl:stylesheet>
