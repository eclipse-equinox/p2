# Eclipse Equinox p2 - Agent Instructions

Guidance for AI agents building, testing and changing this repository (Tycho/Maven based).

## Rules

* Always use Maven (`mvn`), online, with `package` or `verify`. Never `compile` only, never `-o`, no hand-rolled `javac`/`java -jar`.
* Use JDK 25 for Maven, as CI does (see "Java version").
* Always pass `-Dtycho.baseline.replace=none` when you want to see your own changes (see "Baseline replacement").
* Tests run in the `verify` phase (`eclipse-test-plugin`), not in `test`.
* Always list the reactor modules explicitly (`-pl <module> -am`), never build a single module without `-am` (see "Building single modules").
* Redirect Maven output to a file and grep it; a single run produces thousands of lines (e.g. `Resolving MavenDependencyRoots`).
  The ~240 lines of JVM flags at the top come from `.mvn/jvm.config` (`-XX:+PrintFlagsFinal`) and are noise.
* If your change has no effect: check `mvn -v`, that `replaced with the baseline` does not occur in the log, that the changed bundle and the module under test are in the `Reactor Summary`, and that a `Tests run:` line for your class exists.

## Quick start (commands)

```sh
export JAVA_HOME=<path to a JDK 25>        # confirm with the "Java version" line of `mvn -v`
# Package a bundle and everything it needs, no tests:
mvn -B -pl bundles/<bundle> -am package -DskipTests -Dtycho.baseline.replace=none
# Run one test class (tests are in bundles/org.eclipse.equinox.p2.tests):
mvn -B -pl bundles/org.eclipse.equinox.p2.tests -am verify \
    -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Dtycho.baseline.replace=none \
    > /tmp/build.log 2>&1; grep -n "Tests run:\|BUILD\|ERROR" /tmp/build.log | tail -30
# Did Tycho throw away my changes? Must print 0:
grep -c "replaced with the baseline" /tmp/build.log
```

## Java version

* Build with JDK 25, as CI does; Tycho 5.x itself needs at least JDK 21.
  Maven uses `JAVA_HOME` (when set) rather than the `java` on the `PATH`, so check `mvn -v`.
  If it points to an older JDK, look for installed ones (e.g. `/usr/lib/jvm`, `/opt/java`, SDKMAN) or ask the user.
* With an older JDK (e.g. 17) Maven fails immediately: `TychoFeatureMapping has been compiled by a more recent version of the Java Runtime (class file version 65.0)`.
* Bundles are compiled and tests are forked with the JDK running Maven.
  CI additionally passes `-Pbree-libs` (compile against the BREE JRE libraries via the machine-specific `~/.m2/toolchains.xml`); add it only when BREE-specific compile errors matter.

## `.mvn` configuration (picked up automatically from the repo root)

* `.mvn/jvm.config`: `-XX:+PrintFlagsFinal -Xmx2g`.
* `.mvn/maven.config`:
  * `-Pbuild-individual-bundles` resolves dependencies not found in the reactor from the Eclipse I-build p2 repository (`eclipse-sdk-repo`, https://download.eclipse.org/eclipse/updates/4.42-I-builds/). It is already active; nothing else needs to be set up.
  * `-Dtycho.localArtifacts=ignore` makes Tycho ignore artifacts of this repo previously installed in `~/.m2`, so `mvn install` of module A followed by `mvn -pl B` does NOT make B see A. Both have to be in the same reactor invocation.
  * `-Dtycho.surefire.deleteWorkDir=true` wipes the test work dir after each run.
* Do not edit these files to work around a problem, pass `-D`/`-P` options on the command line instead.

## Building single modules

* This repository is built standalone (CI, see `Jenkinsfile`: `mvn clean verify -Pbree-libs -Papi-check -Pjavadoc`), not inside `eclipse.platform.releng.aggregator`, of which it is a submodule (`equinox.p2`).
  The `relativePath ../eclipse-platform-parent` of the root `pom.xml` does not exist here, so the parent POM comes from `~/.m2` or repo.eclipse.org and may be stale.
  Changes to the parent are only seen here after installing it into `~/.m2` from the aggregator; this is rarely needed for p2 work.
* Everything not in this repo is downloaded from the I-build repository and Maven Central, so the first build needs network access.
* `mvn -pl <module> -am ...` builds the module plus all upstream modules from this repo from source (reactor resolution).
* `mvn -pl bundles/org.eclipse.equinox.p2.tests verify` without `-am` fails with `Missing requirement: ... requires 'osgi.bundle; org.eclipse.equinox.frameworkadmin.test 1.0.0' but it could not be found`:
  bundles that exist only in this repo are not in the I-build repository, and `localArtifacts=ignore` hides `~/.m2`.
  Even where it resolves, a module built without `-am` is compiled and tested against the I-build binaries of its p2 dependencies, not your working tree.
* With `-am` and `-Dtest=X`, upstream modules without a matching test fail, so add `-Dsurefire.failIfNoSpecifiedTests=false`.
* Whole-repo `mvn clean verify -DskipTests` works (see `README`) but is much slower than `-pl ... -am`.

## Baseline replacement (the main trap)

Symptom: build is green but your change is not in the jar, the tests, or the product built from the features.

Cause: the parent POM configures `tycho-p2-plugin` with `baselineRepositories` (the I-build repository).
In the `package` phase (goal `p2-metadata-default`) Tycho looks up every bundle/feature by id and version there.
If the same `major.minor.micro` exists, the freshly built jar (and the `-sources` jar) is silently overwritten with the baseline jar.
Everything downstream consumes the replaced artifact: the tests, features that include the bundle, and the test product materialized from those features (`reconciler.product` in `org.eclipse.equinox.p2.tests`, built in `pre-integration-test`).
In a clean standalone run of `-pl bundles/org.eclipse.equinox.p2.tests -am` about 28 modules are replaced.

Log output (INFO level, no failure):

```
[WARNING] MavenProject: org.eclipse.platform:org.eclipse.equinox.p2.core:2.13.300-SNAPSHOT ...: baseline and build artifacts have same version but different contents
[INFO] MavenProject: ...
    The main artifact has been replaced with the baseline version.
```

Diagnose: `grep -n "replaced with the baseline\|No baseline version" /tmp/build.log`.

Fix, pick one:

1. `-Dtycho.baseline.replace=none` on the command line. No replacements happen; the warning about differing content stays and is harmless.
2. Bump the `Bundle-Version` in `META-INF/MANIFEST.MF` (bundles are pomless, so the manifest is the only place), e.g. p2.core 2.13.300 -> 2.13.400.
   Tycho then logs `No baseline version <id>:<version>` and keeps your build.
   Do this only if the bump is a legitimate part of the change (see "API and versions") and bump the including features when required; never only to make a test run.

Does NOT work: `-Dtycho.baseline=disable` (the parent POM hard-codes `<baselineMode>warnCommon</baselineMode>`, which wins over the CLI property), `-U`, `mvn clean` or wiping `~/.m2` (it is not a stale cache).
`-Pjavac` disables the baseline in the parent but also switches the compiler; do not use it for this purpose.

Never commit `-Dtycho.baseline.replace=none` into a POM or `.mvn` file; CI relies on the baseline.

## Running tests

* Main test bundle: `bundles/org.eclipse.equinox.p2.tests` (`eclipse-test-plugin`, so `verify` is required).
  Other test modules (`*.tests.ui`, `*.tests.discovery`, `*.tests.verifier`, `org.eclipse.equinox.frameworkadmin.test`) are pomless.
* Single class: `-Dtest=StringHelperTest` (simple class name); the default is `testClass=org.eclipse.equinox.p2.tests.AutomatedTests`, which takes very long, avoid it unless asked.
* A successful targeted run takes about 40-70 seconds with `-am`; the log line `Tests run: N, Failures: 0, ...` is the proof that tests ran. Reports are in `<bundle>/target/surefire-reports`.
* UI tests need a display (CI uses `xvnc`).
* Network-related tests may fail without internet; judge by whether the failure relates to your change.

## API and versions

* This repo uses PDE API tooling. Do not break public API (packages not `x-internal`); new public API needs `@since` with the next bundle version. Prefer changing `internal` packages.
* Do not bump bundle versions drive-by; version bumps are done per release stream (`Version bump(s) for ...` commits) or when API changes require it.

## Code and commit style

Follow the surrounding code first; keep every change small and focused on the task.

* **Commits:**
  * Do not commit on your own initiative. Finish the task and suggest a commit message (subject and body) to the user instead.
  * If asked to commit, show the user the commit message and let them review it before committing, unless they explicitly said to go ahead.
  * Subject: imperative, <= ~72 characters, no trailing period, no prefixes (e.g. `Read p2 trust preferences without IPreferencesService`).
  * Body: always include one, even for trivial changes. Explain *why* (problem, motivation), not *what* the diff does; wrap at ~72 characters; reference issues as `Fixes: https://github.com/eclipse-equinox/p2/issues/<n>`.
  * One logical change per commit, no unrelated formatting or cleanup mixed in.
  * AI disclosure: end the message with an `Assisted-by: <Provider> <Model-Family> (<Version/ID>)` trailer naming yourself, e.g. `Assisted-by: GitHub Copilot (GPT-4o-2024-08-06)`; give as much of it as you know. Add further trailers such as `Co-authored-by:` only if the user or your tooling requires it.
* **Java:** tabs for indentation, keep the existing license header (EPL-2.0 with `SPDX-License-Identifier`) and update the copyright year range of files you modify; do not reformat untouched code.
  Use public or package-private top-level types instead of inner classes/interfaces/records (project preference).
* **Javadoc and comments:** short; one or two sentences on what a method does and anything non-obvious (units, null handling, threading, exceptions).
  No Javadoc for trivial getters/overrides; comment *why*, never *what*; no commented-out code.
* **Tests:** add or adjust a focused JUnit test for behaviour changes in `bundles/org.eclipse.equinox.p2.tests`, preferably in an existing test class; do not weaken or delete existing tests to make a build pass.
* **Hygiene:** `target/` and `.tycho-consumer-pom.xml` are git-ignored; do not commit `bin/`, build logs or other stray files, check `git status` before finishing.
* **Markdown:** start each sentence on a new line; pad tables so they are readable as plain text.

## Generative AI (Eclipse Foundation guidelines)

This project follows the [Eclipse Foundation Generative AI usage guidelines](https://www.eclipse.org/projects/handbook/#genai).
The human committer stays responsible for everything that is contributed; make that easy for them:

* **Transparency:** disclose AI involvement with the `Assisted-by:` commit trailer (format above).
  For new files that are largely or entirely AI-generated, ask the user whether the handbook's header with the "AI Disclosure" note (`SPDX-License-Identifier: EPL-2.0 and CC0-1.0`) should be used; the copyright holder is the responsible human, never invent a name.
* **Verification:** the output must be reviewable and verified. Run the relevant tests (see above), do not claim success without evidence, and point out what you did not verify.
* **IP cleanliness:** do not copy code from other projects or from memory of other sources; write it yourself or stop and ask. Do not add new third-party dependencies, bundled libraries or binaries without the user's explicit agreement, they require IP due diligence.
* **Confidentiality:** do not put secrets, credentials, personal data or other confidential content into code, commits, logs or issues.
* **No autonomous publishing:** do not push, open or merge pull requests, or write to issue trackers on your own; a human reviews and submits.
