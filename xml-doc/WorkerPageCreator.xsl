<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet href="../../manual-styles.css" type="text/css"?>

<!--
    Document   : WorkerPageCreator.xsl
    Created on : October 15, 2011, 4:04 PM
    Author     : gimli
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

	<xsl:template match="p">
		<xsl:copy-of select="."/>
	</xsl:template>

	<xsl:template match="worker">
		<html>
			<head>
				<title><xsl:value-of select="@name"/></title>

			</head>
			<body>
				<div class="mainMenu">
					<xsl:copy-of select="document('main-menu.xml')"/>
				</div>
				<div class="content-frame">
					<h1><xsl:value-of select="@name"/></h1>
					<xsl:call-template name="description"/>
					<xsl:call-template name="usage"/>
					<xsl:call-template name="sources"/>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="description">
		<div border="1">
			<h4>Description of algorithm:</h4>
			<xsl:apply-templates select="description/p"/>
		</div>
	</xsl:template>

	<xsl:template name="usage">
		<div border="1">
			<h4>Examples:</h4>
			<xsl:apply-templates select="usage/p"/>
		</div>
	</xsl:template>

	<xsl:template name="sources">
		<h4>Sources of theory:</h4>
		<ol type="i">
			<xsl:for-each select="sources/source">
				<li><a><xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute><xsl:value-of select="."/></a></li>
			</xsl:for-each>
		</ol>
	</xsl:template>
</xsl:stylesheet>
