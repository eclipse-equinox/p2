# AGENTS.md

Guidance for AI agents building and testing this repository (Eclipse Equinox p2, Tycho/Maven based).
Everything here was verified with Tycho 5.0.4 and the parent `eclipse-platform-parent` 4.42.0-SNAPSHOT.

## TL;DR

```sh
export JAVA_HOME=<path to a JDK 21+>        # CI uses 25; confirm with `mvn -v` ("Java version" line)
# Compile/package a bundle and everything it needs, no tests:
mvn -B -pl bundles/<bundle> -am package -DskipTests -Dtycho.baseline.replace=none
# Run one test class (tests are in bundles/org.eclipse.equinox.p2.tests):
mvn -B -pl bundles/org.eclipse.equinox.p2.tests -am verify \
    -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Dtycho.baseline.replace=none \
    > /tmp/build.log 2>&1; grep -n "Tests run:\|BUILD\|ERROR" /tmp/build.log | tail -30
```

After every run check that your changes were not discarded: `grep -c "replaced with the baseline" /tmp/build.log` must print `0`.

Rules of thumb:

* Always use Maven (`mvn`), online, with `package` or `verify`. Never `compile` only, never `-o`, never hand-rolled `javac`/`java -jar` invocations.
* Always pass `-Dtycho.baseline.replace=none` when you want to see your own changes (see "Baseline replacement").
* Tests run in the `verify` phase (`eclipse-test-plugin`), not in `test`.
* Always list the reactor modules explicitly (`-pl <module> -am`), never build a single bundle in isolation (see "Building outside the aggregator").
* Redirect Maven output into a file and grep it, the output is very long (see ".mvn configuration").

## Java versions

* `java` on the `PATH` is not necessarily what Maven uses: Maven uses `$JAVA_HOME`, which may point to an older JDK than the one `java -version` reports.
* Tycho 5.x needs Java 21+. Running Maven on an older JDK (e.g. 17) fails immediately with
  `TychoFeatureMapping has been compiled by a more recent version of the Java Runtime (class file version 65.0)`
  and `Unable to provision ... Type TychoFeatureMapping not present`.
  Fix: point `JAVA_HOME` to an installed JDK 21+ (look in the usual places, e.g. `/usr/lib/jvm`, `/opt/java`, SDKMAN, or ask the user) and check with `mvn -v`.
* Without `-Pbree-libs` bundles compile with the JDK running Maven; CI additionally passes `-Pbree-libs` (compile against the BREE JRE libraries via toolchains). The toolchains (`~/.m2/toolchains.xml`) are machine-specific. Add `-Pbree-libs` only when BREE-specific compile errors matter.
* Tests are forked with the JDK running Maven (see `Command line: .../java` in the log).

## `.mvn` configuration (picked up automatically from the repo root)

* `.mvn/jvm.config`: `-XX:+PrintFlagsFinal -Xmx2g`.
  `PrintFlagsFinal` dumps ~240 lines of JVM flags at the top of every Maven run. It is noise, ignore it, do not try to "fix" it, do not read it.
* `.mvn/maven.config`:
  * `-Pbuild-individual-bundles` resolves dependencies not found in the reactor from the Eclipse I-build p2 repository (`eclipse-sdk-repo`, https://download.eclipse.org/eclipse/updates/4.42-I-builds/).
  * `-Dtycho.localArtifacts=ignore` makes Tycho ignore artifacts of this repo previously installed in `~/.m2`.
    Consequence: `mvn install` of module A followed by `mvn -pl B` does NOT make B see A. Both have to be in the same reactor invocation.
  * `-Dtycho.surefire.deleteWorkDir=true` wipes the test work dir after each run.
* Do not edit these files to work around a problem, pass `-D`/`-P` options on the command line instead.
* Always redirect the Maven output to a log file and inspect it with `grep`; a single run produces thousands of lines (e.g. `Resolving MavenDependencyRoots` lines).

## Building outside the aggregator (what works, what does not)

This repository is built standalone (as in CI, see `Jenkinsfile`: `mvn clean verify -Pbree-libs -Papi-check -Pjavadoc`), not inside `eclipse.platform.releng.aggregator`.
It is a submodule `equinox.p2` of the aggregator, but the `relativePath ../eclipse-platform-parent` of the root `pom.xml` does not exist here.
The parent POM is therefore taken from `~/.m2` (or repo.eclipse.org) and may be stale.
Changes to the parent (aggregator `eclipse-platform-parent`) are only seen here after installing it into `~/.m2` from the aggregator; this is rarely needed for p2 work.

What works:

* `-Pbuild-individual-bundles` is already active (`maven.config`, and also by default because `aggregatorBuild` is not set). Do not add it again, do not look for another switch, there is nothing else to set up. Everything not in this repo is downloaded from the I-build repository and Maven Central, so the first build needs network access.
* `mvn -pl <module> -am ...` builds the module plus all upstream modules from this repo from source (reactor resolution).
* A whole-repo `mvn clean verify -DskipTests` also works (README), but is much slower than `-pl ... -am`.

What does NOT work, and why:

* `mvn -pl bundles/org.eclipse.equinox.p2.tests verify` (no `-am`): fails with
  `Missing requirement: ... requires 'osgi.bundle; org.eclipse.equinox.frameworkadmin.test 1.0.0' but it could not be found`.
  Bundles that exist only in this repo are not in the I-build repository, and `localArtifacts=ignore` hides `~/.m2`, so a previous `mvn install` does not help either.
  Fix: add `-am`.
* Even if it resolves, building a module without `-am` compiles/tests it against the *I-build binaries* of its p2 dependencies, not your working tree.
* Running `-Dtest=X` together with `-am` fails in upstream modules without matching tests unless `-Dsurefire.failIfNoSpecifiedTests=false` is set.
* Pre-built artifacts from `~/.m2` are never a fix; do not run `mvn install` hoping to make later runs see them.

## Baseline replacement (the main trap)

This is the most common reason why "my change is not picked up by the tests".

What happens: the parent POM configures `tycho-p2-plugin` with `baselineRepositories` = the I-build repository.
In the `package` phase (goal `p2-metadata-default`) Tycho looks up every bundle/feature by id and version in that repository.
If the same artifact id and version (in practice `major.minor.micro` of an unchanged bundle, since the I-build contains it) exists, then the freshly built jar (and the `-sources` jar) is overwritten with the baseline jar.
Your compiled changes are silently thrown away, while the build is green and the log says (only at INFO level):

```
[WARNING] MavenProject: org.eclipse.platform:org.eclipse.equinox.p2.core:2.13.300-SNAPSHOT ...: baseline and build artifacts have same version but different contents
[INFO] MavenProject: ...
    The main artifact has been replaced with the baseline version.
```

Everything downstream consumes the replaced artifact: the tests, features that include the bundle, and the test product that is materialized from those features (`reconciler.product` in `org.eclipse.equinox.p2.tests`, built in `pre-integration-test`).
So an edit to a bundle's source, to a feature, or to a test that goes through a bundle -> feature -> product indirection can all look "not picked up", although the build did compile it, because each step is overwritten right away.
It is not a stale Maven cache; deleting `~/.m2` content, `-U` or `mvn clean` do not help, do not go down that road.

Diagnose: `grep -n "replaced with the baseline\|No baseline version" /tmp/build.log`.
In a clean standalone run of `-pl bundles/org.eclipse.equinox.p2.tests -am` about 28 modules are replaced.

Fix, pick one:

1. Command line, preferred for local work: `-Dtycho.baseline.replace=none`.
   Verified: no replacements, the jar contains the new content, the warning about differing content stays (harmless).
2. Bump the version of the changed bundle (the `Bundle-Version` in `META-INF/MANIFEST.MF`, bundles are pomless so the manifest is the only place; verified with p2.core 2.13.300 -> 2.13.400).
   Then no baseline version exists and Tycho logs `No baseline version <id>:<version>` and keeps your build.
   Do this only if the version bump is a legitimate part of the change (it normally is, once per release cycle for changed bundles) and bump the including features accordingly when required. Do not bump versions only to make a test run.

Does NOT work:

* `-Dtycho.baseline=disable`: the parent POM hard-codes `<baselineMode>warnCommon</baselineMode>`, which wins over the CLI property, replacement still happens (verified).
* `-Pjavac` disables baseline in the parent, but also switches the compiler; do not use it for this purpose.

Never commit `-Dtycho.baseline.replace=none` into any POM or `.mvn` file; CI relies on the baseline.

## Running tests

* Main test bundle: `bundles/org.eclipse.equinox.p2.tests` (`eclipse-test-plugin`, so `verify` is required). Other test modules (`*.tests.ui`, `*.tests.discovery`, `*.tests.verifier`, `org.eclipse.equinox.frameworkadmin.test`) are pomless; `frameworkadmin.test` was run by surefire in the reactor above, check the others individually.
* Single class: `-Dtest=StringHelperTest`; the full suite is `testClass=org.eclipse.equinox.p2.tests.AutomatedTests` (default, takes very long, avoid unless asked).
* A successful targeted run takes about 40-70 seconds with `-am`; the log line `Tests run: N, Failures: 0, ...` is the proof that tests ran. Reports are in `<bundle>/target/surefire-reports`.
* UI tests need a display (CI uses `xvnc`).
* Network-related tests may fail without internet; judge by whether the failure relates to your change.

## Checklist when "my change has no effect"

1. `mvn -v` shows Java 21+?
2. `-Dtycho.baseline.replace=none` set? (`grep "replaced with the baseline"` in the log must be empty)
3. The module under test and the changed bundle are both in the reactor (`-pl a,b` or `-am`)? Check the `Reactor Summary`.
4. `Tests run:` line present for the class you expected? (for `-Dtest`, the class name must match the simple name)
5. Only after that suspect something else. Do not clear `~/.m2` or `-U`.

## Code and commit style

Follow the surrounding code first; keep every change small and focused on the task.

* **Commits:**
  * Do not commit on your own initiative. Finish the task and suggest a commit message (subject and body) to the user instead.
  * If asked to commit, show the user the commit message and let them review it before committing, unless they explicitly said to go ahead.
  * Subject: imperative, <= ~72 characters, no trailing period, no prefixes (e.g. `Read p2 trust preferences without IPreferencesService`).
  * Body: always include one, even for trivial changes. Explain *why* the change is made (problem, motivation), not *what* the diff does; wrap at ~72 characters; reference issues as `Fixes: https://github.com/eclipse-equinox/p2/issues/<n>`.
  * One logical change per commit, no unrelated formatting or cleanup mixed in.
  * AI disclosure: end the message with an `Assisted-by: <Provider> <Model-Family> (<Version/ID>)` trailer naming yourself, e.g. `Assisted-by: GitHub Copilot (GPT-4o-2024-08-06)`; give as much of it as you know (see "Generative AI" below). Add further trailers such as `Co-authored-by:` only if the user or your tooling requires it.
* **Javadoc and comments:** short and focused.
  * One or two sentences saying what the method does and anything non-obvious (units, null handling, threading, thrown exceptions); do not restate the signature or the code.
  * No Javadoc for trivial getters/overrides; add `@param`/`@return` only when they add information.
  * Comment *why*, never *what*. Do not leave commented-out code or change-log style comments.
* **API:** this repo uses PDE API tooling. Do not break public API (packages not `x-internal`); new public API needs `@since` with the next bundle version. Prefer changing `internal` packages when possible.
* **Versions:** do not bump bundle versions in a drive-by manner; version bumps are done per release stream (`Version bump(s) for ...` commits) or when API changes require it.
* **Formatting:** tabs for indentation in Java, keep the existing license header (EPL-2.0 with `SPDX-License-Identifier`) and update the copyright year range of files you modify; do not reformat untouched code.
* **Tests:** add or adjust a focused JUnit test for behaviour changes in `bundles/org.eclipse.equinox.p2.tests`, preferably in an existing test class; do not weaken or delete existing tests to make a build pass.
* **Generated files:** `target/` and `.tycho-consumer-pom.xml` are git-ignored; do not commit `bin/`, build logs or other stray files, check `git status` before finishing.

## Generative AI (Eclipse Foundation guidelines)

This project follows the [Eclipse Foundation Generative AI usage guidelines](https://www.eclipse.org/projects/handbook/#genai).
The human committer stays responsible for everything that is contributed; make that easy for them:

* **Transparency:** disclose AI involvement with the `Assisted-by:` commit trailer (format above).
  For new files that are largely or entirely AI-generated, ask the user whether the handbook's header with the "AI Disclosure" note (`SPDX-License-Identifier: EPL-2.0 and CC0-1.0`) should be used; the copyright holder is the responsible human, never invent a name.
* **Verification:** the output must be reviewable and verified. Run the relevant tests (see above), do not claim success without evidence, and point out what you did not verify.
* **IP cleanliness:** do not copy code from other projects or from memory of other sources; write it yourself or stop and ask. Do not add new third-party dependencies, bundled libraries or binaries without the user's explicit agreement, they require IP due diligence.
* **Confidentiality:** do not put secrets, credentials, personal data or other confidential content into code, commits, logs or issues.
* **No autonomous publishing:** do not push, open or merge pull requests, or write to issue trackers on your own; a human reviews and submits.
