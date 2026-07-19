What exactly is inside a particular p2 repository? It's sometimes useful
to be able to browse to a repository with your web browser and see
what's inside it. Like with [this](http://net4j.sourceforge.net/update/)
one, which is created for old-style update sites.

This page describes a mechanism for adding a readable HTML outline to
your repository. Here is an example of a repository with such an
outline:

![Image:p2Listing.png](images/p2Listing.png "Image:p2Listing.png")

You can provide something similar for your public p2 repository. Just
add this Ant
[markup](http://git.eclipse.org/c/cdo/cdo.git/tree/plugins/org.eclipse.emf.cdo.releng/build.xml)
at the end of the site.p2 generation target:

``` xml
<echo message="Creating human readable index.html" />
<unzip src="${site.p2.dir}/content.jar" dest="${site.p2.dir}" />
<xslt style="xsl/content2html.xsl" in="${site.p2.dir}/content.xml" out="${site.p2.dir}/index.html" />
<delete file="${site.p2.dir}/content.xml" />
```

The
[content2html.xsl](http://git.eclipse.org/c/cdo/cdo.git/tree/plugins/org.eclipse.emf.cdo.releng/xsl/content2html.xsl)
looks like:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" omit-xml-declaration="yes" indent="yes"/>
  <xsl:strip-space elements="*"/>

  <xsl:template match="/">
    <html ns="http://www.w3.org/1999/xhtml">
      <xsl:apply-templates select="repository"/>
    </html>
  </xsl:template>

  <xsl:template match="repository">
    <head>
      <title>
        <xsl:value-of select="@name"/>
      </title>
    </head>
    <body>
      <h1>
        <xsl:value-of select="@name"/>
      </h1>
      <p>
        <em>For information about installing or updating software, see the
          <a
            href="http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/tasks-124.htm">
            Eclipse Platform Help</a>.
          <br/> Some plugins require third party drivers from
          <a href="http://net4j.sourceforge.net/update/">Net4j and CDO Plus</a>. </em>

      </p>
      <table border="0">
        <tr>
          <td colspan="2">
            <hr/>
            <h2>Features</h2>
          </td>
        </tr>
        <xsl:apply-templates select="//provided[@namespace='org.eclipse.update.feature']">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
        <tr>
          <td colspan="2">
            <hr/>
            <h2>Plugins</h2>
          </td>
        </tr>
        <xsl:apply-templates select="//provided[@namespace='osgi.bundle']">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
      </table>
    </body>
  </xsl:template>

  <xsl:template match="provided">
    <tr>
      <td>
        <xsl:value-of select="@name"/>
      </td>
      <td>
        <xsl:value-of select="@version"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
```

*Original article: [What exactly is inside that p2
repository?](http://thegordian.blogspot.com/2010/05/what-exactly-is-inside-that-p2.html)
(by Eike Stepper)*

[Browsable Repository Index](Category:Equinox_p2 "wikilink")