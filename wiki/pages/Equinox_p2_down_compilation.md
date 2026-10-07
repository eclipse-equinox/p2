Authoring code using Java 5 to target 1.4 VM

In order to be able to compile Java code using the 1.5 syntax to make it
run on 1.4 VM, special care has to be taken. This document describes how
to do this and how to setup projects to avoid problems.

## Compiling the code

ECJ and Javac support "down" compilation. To do this, you just need to
specify -target jsr14 on the command line. The bytecode produced will
still allow code written in 1.5 or 1.4 to be compiled successfully
against it.

## Restrictions on language features

Here are the language features that are are known to \*not\* work:

  - enums can't be used because it requires the java.lang.Enum type from
    the library, see below.
  - declaration of new annotations or reference to annotation types
    since they would cause the loading of the
    java.lang.annontation.Annotation type. However it is still possible
    to use annotations in type, method and field declarations as well as
    method parameters.

## Ensuring that the 1.4 subset of the API is being used

Another important part of the success of such an approach is to
guarantee that the API used by the down compiled code only refers to the
1.4 subset of the class library APIs. To catch API misusage, it is
recommended to:

  - setup your project to use the PDE API tooling
    (http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/api_tooling_setup.htm).
  - specify the J2SE-1.5 and J2SE-1.4 as execution environment (more can
    be specified).

In addition to that, it is recommended to have \*every\* method in
\*every\* class be invoked at least once during the automated tests
while run on the 1.4 VM. This will guarantee that no inappropriate
dependency slipped in when using reflection.

## Setting up PDE Build

For PDE Build to down compile the code, the build.properties need to
contain the following values:

  - javacTarget=jsr14
  - javacSource=1.5

[Category:Equinox p2](Category:Equinox_p2 "wikilink")