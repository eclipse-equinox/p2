Hosting a p2 repository on a web server does not require any specific
capabilities from the server: no servlet, no PHP, nothing. Just the
ability to serve files,.

In order to make a repository available on a server, simply copy the
files produced by the export/build of the feature or the product on the
server. Typically a repository looks like this, but there could also be
other files.

`    content.xml (or content.jar)`
`    artifacts.xml (or artifacts.jar)`
`    plugins/`
`      `<files>
`    features/`
`      `<files>
`    binary/`
`      `<files>

Producers of repositories may be interested in reading the
recommendations on [repository retention
policies](Repository_retention_policy "wikilink").

[Category:Equinox_p2](Category:Equinox_p2 "wikilink")