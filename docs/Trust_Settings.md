# Trust Settings

During installation of new content, p2 performs security checks on artifacts (signatures) and authorities (update sites).
By default, if a check fails, the user is presented with a dialog to confirm or reject the operation.

The **Trust** preference pages allow users to change this behavior for the current profile.

### Artifact Trust (Signed/Unsigned/PGP)
p2 verifies artifact signatures during installation.
Artifacts signed by X509 certificates rooted in Java's trust store are trusted automatically.
Artifacts signed by PGP keys are trusted only if the key is listed in the preferences.
Unsigned artifacts are always treated as untrusted.
When verification fails, the **Trust Artifacts** dialog is shown.

- **Configure Default via**: `org.eclipse.equinox.p2.engine/trustAlways=true` in `plugin_customization.ini` — silently allows installation of unsigned or untrusted JARs.
- **Override via**: `-Declipse.p2.unsignedPolicy=allow` in `eclipse.ini` (after `-vmargs`).
- **UI setting**: `Preferences > Install/Update > Trust > Artifacts`.

### Authority Trust (Update Sites)
p2 tracks all external update sites contacted during installation and shows the **Trust Authorities** dialog so the user can approve the contacted sources.

- **Configure Default via**: `org.eclipse.equinox.p2.engine/trustAllAuthorities=true` in `plugin_customization.ini` — silently trusts host certificates.
- **Override via**: `-Dp2.trustAllAuthorities=true` in `eclipse.ini` (after `-vmargs`).
- **Pre-approve specific authorities**: `-Dp2.trustedAuthorities=<comma-or-space-separated list>` (defaults include `https://download.eclipse.org` and `https://archive.eclipse.org`).
- **UI setting**: `Preferences > Install/Update > Trust > Authorities`.

### Transport Protocol Rules
p2 enforces HTTPS for external update sites and artifacts.
The behavior is controlled by the system properties `p2.httpRule` and `p2.ftpRule`.
Each accepts `redirect` (http→https, ftp→ftps), `allow` (use the original protocol) or `block` (fail the connection).

### Storage
Settings are saved in the p2 **Profile Scope** (tied to the specific installation):
`p2/org.eclipse.equinox.p2.engine/profileRegistry/<id>.profile/.data/.settings/org.eclipse.equinox.p2.engine.prefs`

### References
- [p2 Trust reference (Eclipse platform help)](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.user/reference/ref-p2-trust.htm)
- [Eclipse 4.28 New & Noteworthy — Security](https://eclipse.dev/eclipse/news/news.html?file=4.28/platform.html#Security)
