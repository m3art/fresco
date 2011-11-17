<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : MenuFormatter.xsl
    Created on : October 16, 2011, 11:22 AM
    Author     : gimli
    Description:
        Creates links in left menu
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:template match="folder">
		<ul>
			<xsl:value-of select="@name"/><br/>
			<xsl:for-each select="a">
				<li>
					<a>
						<xsl:attribute name="href">
							<xsl:value-of select="@href"/>
						</xsl:attribute>
						<xsl:value-of select="."/>
					</a>
					<br />
				</li>
			</xsl:for-each>
		</ul>
	</xsl:template>

	<xsl:template match="title">
		<html><body>
		<h4 class="menu-title">
			<xsl:value-of select="."/>
		</h4>
		<xsl:apply-templates select="folder"/>
		</body></html>
	</xsl:template>

</xsl:stylesheet>
