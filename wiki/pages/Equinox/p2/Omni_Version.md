# Introduction

This page describes the "Omni Version" - an implementation of Version
and VersionRange classes in Equinox p2 that enables p2 to handle other
versioning schemes than OSGi. See
[bug 233699](https://bugs.eclipse.org/bugs/show_bug.cgi?id=233699) for
discussion.

## Background

There are other versioning schemes in wide use that are not compatible
with OSGi version and version ranges. The problem is both syntactic and
semantic.

### Example of semantic issues

Many open source projects do their versioning in a fashion similar to
OSGi but with one very significant difference. For two versions that are
otherwise equal, a lack of qualifier signifies a higher version then
when a qualifier is present. I.e.

`1.0.0.alpha`
`1.0.0.beta`
`1.0.0.rc1`
`1.0.0`

The 1.0.0 is the final release. The qualifier happens to be in
alphabetical order here but that's not always true.

Mozilla Toolkit versioning has many rules and where each segment has 4
(optional slots; numeric, string, numeric, and string where each slot
has a default value of being 0 or "max string" if missing).

`1.2a3b.  // yes, a trailing . is allowed and means .0`
`1.a2`

Mozilla also allows bumping the version (using an older Mozilla scheme)

`1.0+`

This means 1.1pre in mozilla.

### Example of syntax issue

Here are some examples of versions used in Red Had Fedora distributions.

`KDE Admin version 7:4.0.3-3.fc9`
`Compat libstdc version 33-3.2.3-63`
`Automake 1.4p6-15.fc7`

And here are some mozilla toolkit versions:

`1.*.1`
`1.0+`
`1.-1  // yes, negative integer version numbers are allowed, the - is not a delimiter`
`1.2a3b.a`

These are not syntactically compatible with OSGi versions.

## Current implementation in p2 3.5M5

The current implementation in p2 uses the OSGi resolver to create the
final step of a provisioning plan. This means that versions that can not
be converted to OSGi will cause the planner to stop with an error. This
is expected to be fixed when a SAT4J based planner is used.

# Solution

## One implementation of Version and VersionRange

Equinox p2 has one implementation of Version and one of VersionRange
(refered to as OmniVersion, and OmniVersionRange to describe that they
are capable of capturing the semantics of various version formats). The
advantages over previous proposed implementations are that there is no
need to dynamically plugin new implementations, and new formats can be
more easily be introduced.

Even if the finished solution only requires a single implementation (the
OmniVersion discussed below), there are other factors to consider. The
current p2 SimplePlanner uses the OSGi planner, and it can only
understand OSGi versions. There is work being done on SAT4J to enable it
being used instead of the OSGi planner (work to handle "explanations"
could also be used to handle "attachments" (now being done with OSGi
planner).

See [bug 233699](https://bugs.eclipse.org/bugs/show_bug.cgi?id=233699)
for more information.

## One Canonical Format

The OmniVersion and OmniVersion range are "universal" - all instances of
Version should be comparable against each other with a fully defined
(non ambiguous) ordering. The API is (as today) based on a single string
fully describing a version or version range.

The canonical string format is called "raw" and it is explained in more
detail below. To ensure backwards compatibility, as well as providing
ease of use in an osgi environment, version strings that are not
prefixed with an OmniVersion keyword (e.g. "raw") have the same format
and semantics as the current osgi version format.

Ad an example the following two version strings are both valid input,
and express exactly the same version:

`1.0.0.r1234`
`raw:1.0.0.'r1234'`

## Implementation of Omni Version and VersionRange

### OmniVersion

The OmniVersion implementation uses an vektor to store version-segements
in order of descending significance. A segment is an instance of
Integer, String, Comparable\[\], MaxInteger, MaxString, or Min.

#### Comparison

Comparison is done by iterating over segments from 0 to n.

  - If segments are of different type the rule MaxInteger \> Integer \>
    Comparable\[\] \> MaxString \> String is used - the comparison is
    done and the version with the greater segment type is reported as
    greater.
  - If segments are of equal type - they are compared - if one is
    greater the comparison is done and the version with the greater
    segment is reported as greater.
  - All versions are by default padded with -M (absolute min segment)
    "to infinity". A version may have an explicit pad element which is
    used instead of the default.
  - A shorter version is compared to a longer by comparing the extra
    segments in the longer version against the shorter version's pad
    segment.
  - If all segments are equal up to end of the longest segment array,
    the pad segments are compared, and the version with the greater pad
    segment is reported as greater.
  - If pad segments are also equal the two versions are reported as
    equal.
  - As a consequence of not including delimiters in the canonical
    format; two versions are equal if they only differ on delimiters.

As an example - here is a comparison of versions (expressed in the raw
format introduced further on in the text - 'p' means that a pad element
follows, and -M the absolute min segment):

` 1p-M < 1.0.0 < 1.0.0p0 == 1p0 < 1.1 < 1.1.1 < 1p1 == 1.1p1 < 1pM`

#### Raw and Original Version String

The original version should be kept when the raw version format is used,
but it is not an absolute requirement as simple raw based forms such as
raw:1.2.3.4.5 could certainly be used by humans. Someone (who for some
reason does not want to use osgi or some other version scheme), could
elect to use the raw format as their native format.

A version string with raw and original is written on the form:

`  'raw' ':' raw-format-string '/' format(...):original-format-string`

The p2 Engine completely ignores the original part - only the raw part
is used, and the original format is only used for human consumption.

Example using a mozilla version string (as it has the most complex
format encountered to date).

`  raw:<1.m.0.m>.<20.'a'.3.'b'>p<0.m.0.m>/format((<n=0;?s=m;?n=0;?s=m;?>(.<n=0;?s=m;?n=0;?s=m;?>)*)=p<0.m.0.m>;):1.20a3b.a`

An original version string can be included with unknown format:

`  raw:<1.m.0.m>.<20.'a'.3.'b'>p<0.m.0.m>/:1.20a3b.a`

See below for full explanation of the raw format.

### OmniVersionRange

The OmniVersionRange holds two OmniVersion instances (lower and upper
bound). A version range string uses the delimiters '\[\]', '()' and ','.
If these characters are used in the lower or upper bound version
strings, these occurrences must be escaped with '\\' and occurrences of
'\\' must also be escaped.

The version range is either an osgi version range (if raw prefix is not
used), or a raw range. The format of the raw range is:

`  'raw' ':' ( '[' | '(' ) raw-format-string ',' raw-format-string ( ']' | ')' )`

The raw-range can be followed by the original range:

`  raw-range '/' 'format' '(' format-string ')' ':' ( '[' | '(' ) original-format-string ',' original-format-string ( ']' | ')' )`

An original version range can be included with unknown format:

`  raw: [<1.m.0.m>.<20.m.0.m>p<0.m.0.m>,<1.m.0.m>.<20.'a'.3.'b'>p<0.m.0.m>]/:[1.20,1.20a3b.a]`

The p2 Engine completely ignores the original part - only the raw part
is used, and the original format is only used for human consumption.

See below for full explanation of the raw format.

#### Other range formats

Note that some version schemes have range concepts where the notion of
inclusive or exclusive does not exist, and instead use symbolic markers
such as "next larger", "next smaller", or use wildcards to define
ranges. In these cases, the publisher of an IU must use discrete
versions and the inclusive/exclusive notation to define the same range.

Some range specifications allows the specification of union, or
exclusion of certain versions. This is not yet supported by p2. If
introduced it could be expressed as a series of ranges where a ^ before
a range negates it. Example \[0,1\]\[3,10\]^\[3.1,3.7) equivalent to
\[0,10\]^(1,3)^\[3.1,3.7)

## Format Specification

There are two basic formats *default osgi string format*, and *raw
canonical string format*. There are also two corresponding range formats
osgi-version-range, and raw-version-range.

The raw format is a string representation of the internally used format
- it consists of the keyword "raw", followed by a list of entries
separated by period. An entry can be numerical, quoted alphanumerical,
or a sub canonical list on the same format. A canonical version (and sub
canoncial version arrays) can be padded to infinity with a special
padding element. Special entries express the notion of 'max integer' and
'max string'.

The osgi string format is the well known format in current use.

**The raw format in BNF:**

```
   digit: [0-9];
   letter: [a-zA-Z];
   numeric : digit+;
   alpha : letter+;
   alpha-numeric : [0-9a-zA-Z]+;
   delimiter: [^0-9a-zA-Z];
   character: .;
   characters .+;
   quoted-string: ("[^"]*")|('[^']*');  // i.e a sequence of charactes quoted with " or ', where ' can be used in a " quoted string and vice versa
   range-safe-string:  TBD; // a sequence of any characters but with ',' ']', ')' and '\' escaped with '\';
   sq: ['];
   dq: ["];

   version :
      | osgi-version
      | raw-version
      ;
   osgi-version :
      | numeric
      | numeric '.' numeric
      | numeric '.' numeric '.' numeric
      | numeric '.' numeric '.' numeric '.' .+
      ;
   raw-version :
      | 'raw' ':' raw-segments optional-original-version
      ;
   optional-original-version :
      |
      | '/' original-version
      ;
   version-range :
      | osgi-version-range
      | raw-version-range
      ;
   rs : ('[' | '(') ;
   re : (']' | ')') ;

   osgi-version-range :
      | rs osgi-version ',' osgi-version re
      ;
   raw-version-range :
      | 'raw' ':' rs raw-segments ',' raw-segments re optional-original-range
      ;
   optional-original-range :
      |
      | '/' original-range
      ;

   raw-segments :
      | raw-elements optional-pad-element
      ;
   raw-elements :
      | raw-elements '.' raw-element
      | raw-element
      ;
   raw-element :
      | numeric
      | quoted-strings  // strings are concatenated
      | '<' raw-elements optional-pad-element '>'   // subvector of elements
      | 'm'   // symbolic 'maxs' == max string
      | 'M'   // symbolic 'absolute max' i.e. max > MAX_INT > maxs
      | '-M // symbolic 'absolute min' i.e. -M <  empty string < array <  int
      ;
   optional-pad-element :
      |
      | pad-element
      ;
   quoted-strings :
      | quoted-strings quoted-string
      | quoted-string
      ;
   pad-element :
      | 'p' raw-element
      ;

   original-version :
      | optional-format-definition ':' .*
      ;
   original-range :
      | optional-format-definition ':' rs range-safe-string ',' range-safe-string re
      ;
   optional-format-definition :
      |
      | format-definition
      ;
   format-definition :
      | 'format' '(' pattern ')'
      ;

   // Definition of parsing patterns
   //
   pattern :
      | pattern pattern-element
      | pattern-element
      ;
   pattern-element :
      | pelem optional-processing-rules optional-pattern-range
      | '[' pattern ']' processing-rules
      ;
   optional-processing-rules :
      | optional- processing-rules '=' processing-rule ';'
      | '=' processing-rule ';'
      |
      ;
   optional-pattern-range :
      | repeat-range
      |
      ;

   pelem
      | 'r' | 'd' | 'p' | 'a' | 's' | 'S' |  'n' | 'N' | 'q'
      | '(' pattern ')'
      | '<' pattern '>'
      | delimiter
      ;
   repeat-range :
      | '?' | '*' | '+'
      | '{' exact '}'
      | '{' at-least ',' '}'
      | '{' at-least ',' at-most '}'
      ;

   exact : at-least : at-most : numeric ;

   processing-rule :
      | raw-element
      | pad-element
      | '!'
      | '[' char-list ']'
      | '[' '^' char-list ']'
      | '{' exact '}'   // for character count
      | '{' at-least ',' '}'
      | '{' at-least ',' at-most '}'
      ;
   char-list: TBD ; // Sequence of any character but with '^', ']' and '\' escaped with '\'
   delimiter :
      | [!#$%&/=^,.;:-_ ] // Any non-alpha-num that has no special meaning
      | quoted-string
      | '\' .  // any escaped character
      ;
```

Examples:

  - OSGi 1.0.0.r1234 is expressed as raw:1.0.0.'r1234'
  - apache/triplet style 1.2.3 is expressed as raw:1.2.3.m
  - mozilla style 1a.2a3c. can be expressed as
    raw:\<1.'a'.0.m\>.\<2.'a'.3.'c'\>p\<0.m.0.m\> (mozilla is a complex
    format - see external links at the end of page).

## Format Pattern Explanation

Here are explanations for the rules in format(pattern).

<table>
<tbody>
<tr class="odd">
<td><p><strong>rule</strong></p></td>
<td><p><strong>description</strong></p></td>
</tr>
<tr class="even">
<td></td>
<td><p>raw - matches one <em>raw-element</em> as specified by the <code>raw</code> format. The 'r' rule does not match a pad element - use 'p' for this.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>matches a single character or sequence of characters - the matched result is not included in the resulting canonical vector (i.e. it is not a segment). A '\\' is needed to include a single '\'. The sequence of chars acts as one delimiter.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>matches any non alpha-numerical character (including space) - the matched result is not included in the canonical vector (i.e. it is not a segment). A non alphanumerical character acts as a delimiter. Special characters must be escaped when wanted as delimiters.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>auto - a sequence of digits creates a numeric segment, a sequence of alphabetical characters creates a string segment. Segments are delimited by any character not having the same character class as the first character in the sequence, or by the following delimiter. A numerical sequence ignores leading zeros.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>delimiter; matches any non alpha-numeric character. The matched result is not included in the resulting canonical vector (i.e. it is not a segment).</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>a string group matching only alpha characters (i.e. "letters"). Use processing rules =[]; or =[^] to define the set of allowed characters. It is possible to allow inclusion of delimiter chars, but not inclusion of digits.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>a string group matching any group of characters. Use processing rules =[]; or =[^] to define the set of allowed characters. Care must be taken to specify exclusion of a delimiter if elements are to follow the 'S'.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>a numeric (integer) group with value &gt;= 0. Leading zeros are ignored.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>a possibly negative value numeric (integer) group. Leading zeros are ignored.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>parses an explicit <em>pad-element</em> in the input string as defined by the raw format. To define an implicit pad as part of the pattern use the processing instruction <code>=p...;</code>. A pad element can only be last in the overall version string, or last in a sub array.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>smart quoted string - matches a quoted alphanumeric string where the quote is determined by the first character of the string segment. The quote must be a non alphanumeric character, and the string must be delimited by the same character except brackets and parenthesises (i.e. (), {}, [], &lt;&gt;) which are handled as pairs, thus 'q' matches "<andrea-doria>" and produces a single string segment with the text 'andrea-doria'. A non-quoted sequence of characters are not matched by 'q'.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>indicates a group</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>indicates a group, where the resulting elements of the group is placed in an array, and the array is one resulting element in the enclosing result</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>zero to one occurrence of the preceding rule</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>zero to many occurrences of the preceding rule</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>one to many occurrences of the preceding rule</p></td>
</tr>
<tr class="even">
<td><p>}</p></td>
<td><p>exactly n occurrences of the preceding rule</p></td>
</tr>
<tr class="odd">
<td><p>}</p></td>
<td><p>at least n occurrences of the preceding rule</p></td>
</tr>
<tr class="even">
<td><p>}</p></td>
<td><p>at least n occurrences of the preceding rule, but not more than m times</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>short hand notation for an opti</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>an additional processing rule is applied to the preceding rule. The <em>processing</em> part can be:</p>
<ul>
<li><em>raw-element</em> - use this <em>raw-element</em> (as defined by the raw format) as the default value if input is missing. The default value does not have to be of the same type (e.g. "s=123;?" produces an integer segment of value 123 if the optional s is not matched.</li>
<li><code>!</code> - if input is present do not turn it into a segment (i.e. ignore what was matched)</li>
<li><code>[</code><list of chars><code>]</code> - when applied to a 'd' defines the set of delimiters. The characters ], ^, and \ must be escaped with \ to be used in the list of chars. and Example d=[+-/]; One or several ranges of characters such as "a-z" can also be used. Example d=[a-zA-Z0-9_-];</li>
<li><code>[^</code><list of chars><code>]</code> - when applied to a 'd' defines the set of delimiters to be all non alpha numeric except the listed characters. The characters ], ^, and \ must be escaped with \ to be used in the list of chars. One or several ranges of characters such as "a-z" can also be used. Example d=[^$]</li>
<li><code>p</code><em>raw-element</em> - defines "padding to infinity with specified raw-element" when applied to an array, or a group enclosing the entire format. Example <code>format((n.s)=pM;)</code> The pad processing rule is only applied to a parsed array, not to a default value for an array. If padding is wanted in the default array value, it can be expressed explicitly in the default value.</li>
<li><code>{n} {n,} {n,m}</code> character ranges - with the same meaning as the rules with the same syntax, but limits the range in characters matched in the preceding 's', 'S', 'n', 'N', 'q', or 'a' rules. For 'q' the quotes does not count.</li>
</ul></td>
</tr>
<tr class="odd">
<td></td>
<td><p>escape removes the special meaning of a character and must be used if a special character is wanted as a delimiter. A '\\' is needed to include a '\'. Escaping a non special character is superflous but allowed.</p></td>
</tr>
</tbody>
</table>

Additional rules:

  - if a rule produces a null segment, it is not placed in the result
    vector e.g. format(ndddn):10-/-12 =\> raw:10.12
  - Processing (i.e. default values) applied to a group has higher
    precedence than individual processing inside the group if the entire
    group was not successfully matched.
  - Parsing is greedy - format(n(.n)\*(.s)\*) will interpret 1.2.3.hello
    as raw:1.2.3.'hello' (as opposed to being reluctant which would
    produce raw:1.'2'.'3'.'hello')
  - When combining N with ={...}; and the input has a negative number,
    the "-" is included in the character count -
    "format(N{3}N{2}):-1234" results in "raw:-123.4"
  - When combining n or N with ={...} and input has leading zeros -
    these are included in the character count.
  - An empty version strings is always considered to be an error.
  - A format that produces no segments is always considered to be an
    error.

Note about white space in the raw format:

  - white space is accepted inside quoted strings - i.e. "1.'a string'"
    is allowed, but not "1. 2"
  - white space is accepted between version range delimiters and version
    strings - i.e. \[ 1.0, 2.0 \] is allowed.

**Note about timestamps** An earlier proposal had a 't' rule, but this
rule has been deprecated because of the complexity. Instead, the creator
of an IU should simply use 's' or 'n' and ensure comparability by using
a fixed number of characters when choosing 's' format.

### Examples of Version Formats

Here are examples of various version formats expressed as using the
format pattern notation. The examples also show a proposed notation of
using aliases for formats. (See the section 'Tooling Support')

<table>
<tbody>
<tr class="odd">
<td><p><strong>type name</strong></p></td>
<td><p><strong>pattern</strong></p></td>
<td><p><strong>comment</strong></p></td>
</tr>
<tr class="even">
<td></td>
<td><p>n[.n=0;[.n=0;[.S=[a-zA-Z0-9_-];]]]</p></td>
<td><p>Example: the following are equivalent:</p>
<ul>
<li>format(n[.n=0;[.n=0;[.S=[a-zA-Z0-9_-];]]]):1.0.0.r1234</li>
<li>raw:1.0.0.'r1234'</li>
<li>osgi:1.0.0.r1234</li>
<li>1.0.0.r1234</li>
</ul></td>
</tr>
<tr class="odd">
<td></td>
<td><p>n[.n=0;[.n=0;]][d?S=M;]</p></td>
<td><p>A variation on OSGi, with the same syntax, but where the a lack of qualifier &gt; any qualifier, and the qualifier may contain any character. The following are all equivalent:</p>
<ul>
<li>format(n[.n=0;[.n=0;]][d?S=M;]):1.0.0</li>
<li>raw:1.0.0.M</li>
<li>triplet:1.0.0</li>
</ul></td>
</tr>
<tr class="even">
<td></td>
<td><p>n(.n=0;){0,3}[-S=m;]</p></td>
<td><p>As defined by JSR 277 - but is provisional and subject to change as it is expected that compatibility with OSGi will be solved (they are now incompatible because of the fourth numeric field with default value 0). The jsr277 format is similar to triplet, but with 4 numeric segments and a '-' separating the qualifier to allow input of "1-qualifier" to mean "1.0.0.0-qualifier". As in triplet the a lack of qualifier &gt; any qualifier. The following are all equivalent:</p>
<ul>
<li>format(n(.n=0;){1,3}[-S=m;]):1.0.0</li>
<li>raw:1.0.0.0.M</li>
<li>jsr277:1.0.0</li>
</ul></td>
</tr>
<tr class="odd">
<td></td>
<td><p>n[.n=0;[.n=0;[-n=M;.S=m;]]]</p></td>
<td><p>Format used when maven transforms versions like 1.2.3-SNAPSHOT into 1.2.3-<buildnumber>.<timestamp> ensuring that it is compatible with triplet format if missing <buildnumber>.<timestamp> at the end (format produces max, max-string if they are missing). Example: the following are equivalent:</p>
<ul>
<li>format(n[.n=0;[.n=0;[-n=M;.S=m;]]]):1.2.3-45.20081213:1233</li>
<li>raw:1.2.3.45.'20081213:1233'</li>
<li>tripletSnapshot:1.2.3-45.20081213:1233</li>
</ul></td>
</tr>
<tr class="even">
<td></td>
<td><p>&lt;[n:]a(d?a)*&gt;[-n[dS=!;]]</p></td>
<td><p>RPM format matches [EPOCH:]VERSION-STRING[-PACKAGE-VERSION], where epoch is optional and numeric, version-string is auto matched to arbitrary depth &gt;= 1, followed by a package-version, which consists of a buildnumber separated by any separator from trailing platform specification, or the string 'src' to indicate that the package is a souce package. This format allows the platform and src part to be included in the version string, but if present it is not used in the comparisons. The platform type vs source is expected to be encoded elsewhere in such an IU. Everything except the build-number is placed in an array as build number is only compared if there is a tie.</p>
<p>An example of equivalent expressions:</p>
<ul>
<li>format(&lt;[n:]a(d?a)*&gt;[-n[dS=!;]]):33:1.2.3a-23/i386</li>
<li>raw:&lt;33.1.2.3.'a'&gt;.23</li>
</ul></td>
</tr>
<tr class="odd">
<td></td>
<td><p>(&lt;n=0;?s=m;?n=0;?s=m;?&gt;(.&lt;n=0;?s=m;?n=0;?s=m;?&gt;)*)=p&lt;0.m.0.m&gt;;</p></td>
<td><p>Mozilla versions are somewhat complicated, it consists of 1 or more parts separated by period. Each part consists of 4 optional 'fragments' (numeric, string, numeric,string), where numeric fragments are 0 if missing, and string fragments are MAX-STRING if missing. The versions use padding so that 1 == 1.0 == 1.0.0 == 1.0.0.0 etc.</p></td>
</tr>
<tr class="even">
<td></td>
<td><p>S</p></td>
<td><p>Perhaps superflous, but makes this version format appear in a selectable list of formats.</p></td>
</tr>
<tr class="odd">
<td></td>
<td><p>a(d?a)*</p></td>
<td><p>Perhaps superflous, but makes this version format appear in a selectable list of formats, and it serves like a "catch all".</p></td>
</tr>
</tbody>
</table>

## Tooling Support

The OmniVersion is not designed to be extended. Earlier we proposed that
it should be possible to define named aliases for common formats and
that these formats should be parse-able by the OmniVersion parser. The
reasons for introducing alias was to make it possible for users to enter
something like "triplet:1.0.0" instead of entering the more complicated
format. This did however raise a lot of questions: Who can define an
alias, what if the definition of the alias is changed, where are the
alias definitions found. Is it possible to work at all with a version
that is using only an alias - what if I want to modify a range and do
not have access to the alias?

We instead propose that alias handling is a tooling concern. Tooling
should keep a registry of known formats. When a version is to be
presented, the format string is "reverse looked up" in the registry -
and the alias name can be presented instead of the actual format. This
way, the version is always self describing. There is still the need to
get "well known formats" and make them available in order to make it
easier to use non OSGi versions in publishing tools - but there is no
absolute requirement to support this in all publishing tools (some may
even operate in a domain where version format is implied by the domain)
- and there is no "breakage" because an alias is missing.

Tooling support can be as simple as just having preferences where
formats are associated with names - the user can enter new formats and
aliases. Some import mechanism is probably also nice to have. Further
ideas could be that aliases can be published as IU's and installed (i.e
install a preference).

Existing Tooling should naturally use the new OmniVersion implementation
to parse strings - thus enabling a user to enter a version in raw or
format() form. An implementation can choose to present the full version
string (i.e. OmniVersion.toString()), or only the original version.

## More examples using 'format'

A version range with format equivalent to OSGi

`format(n[.n=0;[.n=0;[.S=[a-zA-Z0-9_-];]]]):[1.0.0.r12345, 2.0.0]`

At least one string, and max 5 strings

`format(S=[^.][.S=[^.];[.S=[^.][.S=[^.][.S=[^.]]]]]):vivaldi.opus.spring.bar5`
`format(S=[^.](.S=[^.]){0,4}):vivaldi.opus.spring.bar5  => 'vivaldi'.'opus'.'spring'.'bar5'`

At least one alpha or numerical with auto format and delimiter

`format(a(d?a)*):vivaldi:opus23-spring.bar5  => 'vivaldi'.'opus'.23.'spring'.'bar'.5`

The texts 'opus' and 'bar' should not be included:

`format(s[.'opus'n[.'bar'n]]):vivaldi.opus23.bar8   => 'vivaldi'.23.8`

The first string segment should be ignored - it is a marketing name:

`format(s=!;.n(.n)*):vivaldi.1.5.3`

Classic SCCS/RCS style:

`format(n(.n)*):1.1.1.1.1.1.1.4.5.6.7.8`

Max depth 8 of numerical segments (limited classic SCCS/RCS type
versions):

`format(n(.n){0,7}):1.1.1.1.1.1.1.4`

Numeric to optional depth 8, where missing input is set to 0, followed
by optional string where 'emtpy \> any'

`format(n(d?n=0;){0,7}[a=M;]):1.1.1.4:beta   => 1.1.1.4.0.0.0.0.'beta'`
`format(n(d?n=0;){0,7}[a=M;]):1.1.1.4   => 1.1.1.4.0.0.0.0.M`

Single string range

`format(S):[andrea doria,titanic]`

## Range examples

Examples:

  - raw:\[1.2.3.'r1234',2.0.0\]
  - \[1.2.3.r1234,2.0.0\]
  - format(a+):\[monkey.fred.ate.5.bananas,monkey.fred.ate.10.oranges\]
  - \[1.0.0,2.0.0\] equal to osgi:\[1.0.0,2.0.0\]
  - format(S):\[andrea doria,titanic\]
  - rpm:\[7:4.0.3-3.fc9,8:1\] - an example KDE Admin version
    7:4.0.3-3.fc9 to 8:1
  - triplet:\[1.0.0.RC1,1.0.0\]

## Internationalization

Alphanumerical segments use vanilla string comparison as
internationalization (lexical ordering/collation) would produce
different results for different users.

# Applicability

The generalization of version type applies to objects that by nature may
have a different versioning scheme than OSGi. This includes:

  - Installable Unit
  - Provided Capability
  - Required Capability
  - Artifact key

These does not need to be generalized:

  - File format version numbers (content.xml, artifact.xml, etc)
  - Update Descriptor
  - Touchpoint version numbers and touchpoint action versions
  - Publisher advice versions

# FAQ

**Will users just using Eclipse and OSGi bundles be affected?**
No, users that only deal within the OSGi domain can continue to use
version strings like before, there is no need to specify version
formats.

**How does a user of something know which version type to use? This
seems very complicated...**
To use some non-osgi component with p2, that component must have been
made available in a p2 repository. When it was made available, the
publisher must have made it available with a specified version format.
The publisher must understand the component's version semantics. A
consumer that only wants to install the component does not really need
to understand the format, and the original version string is probably
sufficient. In scenarios where the consumer needs to know more - what to
present is domain specific - some tool could show all non osgi version
strings as "non-osgi" or "formatted" with drill down into the actual
pattern (or if there is an alias registry available, it could reverse
lookup the format).

**Will open (osgi) ranges produce lots of false positives?**
Very unlikely. One decision to minimize the risk was to specify that
integer segments are considered to be later than array and string
segments. (We also felt that version segments specified with integers
are more "precise"). Note that to be included in the range, the required
capability would still need to be in a matching name space, and have a
matching name. To introduce a false positive, the publisher of the false
positive would need to a) publish something already known to others
(namespace and name) b) misinterpret how its versioning scheme works,
and publishing it with a format of n.n.n.n (or n.n.n.s.<something>), c)
having first learned how to actually specify such a format and how to
publish it to a p2 repository and d) then persuaded users to use the
repository.

**What happens when a capability is available with several versioning
schemes?**
A typical case would be some java package that is versioned at the
source using triplet notation, and the same package is also made
available using osgi notation (which btw. is a mistake).

As an example, the following capabilities are found:

  - org.demo.ships triplet:2.0.0
  - org.demo.ships triplet:2.0.0.RC1
  - org.demo.ships osgi:2.0.0
  - org.demo.ships osgi:2.0.0.RC1

(Reminder: in triplet notation 2.0.0.RC1 is *older* than 2.0.0).

The raw versions will then look like this:

  - 2.0.0.m
  - 2.0.0.'RC1'
  - 2.0.0
  - 2.0.0.'RC1'

And the newest is 2.0.0.m (which is correct for both OSGi, and triplet).
When specifying a range, the outcome may depend on if the range is
specified with osgi or triplet notation.

  - osgi:\[1.0.0,2.0.0\] == raw:\[1.0.0, 2.0.0\] =\> matches the
    osgi:2.0.0 version only
  - triplet:\[1.0.0,2.0.0\] == raw:\[1.0.0.m,2.0.0.m\] =\> matches all
    the versions, and picks 2.0.0.m as it is the latest.

i.e. result is correct (assuming the bits are identical as different
artifacts would be picked)

Now look at the lower boundary, and assume that the following versions
are the (only) available:

  - org.demo.ships triplet: 1.0.0 == raw: 1.0.0.m
  - org.demo.ships triplet: 1.0.0.RC1 == raw:1.0.0.'RC1'
  - org.demo.ships osgi: 1.0.0 == raw:1.0.0
  - org.demo.ships osgi:1.0.0.RC1 == raw:1.0.0.'RC1'

When specifying ranges:

  - osgi:\[1.0.0,2.0.0\] == raw:\[1.0.0, 2.0.0\] =\> matches all the
    version, and picks 1.0.0.maxs as this is the newest
  - triplet:\[1.0.0,2.0.0\] == raw:\[1.0.0.m,2.0.0.m\] results in
    1.0.0.m as it is the only available version that matches.

i.e. the result is correct and here the exact same version is picked.

The "worst osgi/triplet crime" that can be committed is publishing an
unqualified triplet version as an osgi version (if the same version is
not also available as a triplet) as this would make that version older
than what it is even when queried using a triplet range.

**What if the publisher of a component changes versioning scheme - what
happens to ranges?**
The order among the versions will be correct as long as the versions are
published using the correct notation. The only implication is that users
must understand that a query for triplet:1.2.3 means raw:1.2.3.m - e.g.
osgi:\[1.0.0,2.0.0\] \!= triplet:\[1.0.0,2.0.0\] (osgi upper range of
2.0.0 would not match triplet published 2.0.0, and triplet lower range
of 1.0.0 would not match osgi published 1.0.0).

**Why not use regexp instead of the special pattern format?**
This was first considered, and would certainly work if the pattern
notation was augmented with processing instructions, or if the regexp is
specified as a substitution that produces the raw format. Such
specifications would typically be much longer and more difficult for
humans to read than the proposed format, except possibly for regexp
experts :). Another immediate problem is that regexp breaks the current
API requirement. It is not included in execution environment
CDC-1.1/Foundation-1.1 required by p2.

**Pattern parsing looks like it could have performance implications -
what are the expectations here?**
The intention is to use a mechanism similar to reqular expressions -
when a format is first seen it is compiled to an internal structure. The
compiled structure is cached and reused for all subsequent occurrences
of the same format. A test will be performed to compare current parsing
of an OSGi version string with the pattern based parsing. Once parsed,
all comparisons are made using the raw vector, which should be
comparable in speed to the current implementation.

Also note that the Engine does not have to parse and apply the format to
the original string unless code explicetly asks for it, and this is not
the normal case during provisioning.

**Why not just let the publisher deal with transforming the version into
canonical form?**
The proposal allows this - the publisher is not required to make the
format available. We think this is reasonable in domains where humans
are not involved in the authoring (or the consumption).

There are several reasons why it is a good idea to include the original
version string as well as the format:

  - the original version strings needs to be kept as users would
    probably not understand the canonical representation in many cases.
  - if the transformation pattern is not available a user would not be
    able to create a request without hand coding the canonical form
  - making the transformation logic used by one publisher available to
    others would mean that all publishers must have extensions that
    allow plugging in such logic, and the plugins must be made available

**Would it be possible to use the current OSGi version as the canonical
form?**
The long answer is: To be general, the encoding would need to be made in
the qualifier string part of the OSGi version. An upper length for
segments must be imposed, numerical sections must be left padded with
"0" to that length, and string segments must be right padded with space
(else string segment parts may overlap integer segments parts). The
selected segment length would need to be big enough to allow the longest
anticipated string segment. A fixed length string representation of MAX
must be invented. A different implementation would still be needed to be
able to keep the original version strings.
The short answer is: no.

**Why not use an escape in string segments to be able to have strings
with a mix of quotes?** There are several reasons:

  - this would mean that the version string would need to be
    preprocessed as it would not have \\ embedded from the start
  - all version strings that use \\ as a delimiter would need to be
    pre-processed to escape the \\
  - to date, authors of this proposal have not seen a version format
    that requires a mix of quotes
  - In the unlikely event that such strings are present it is possible
    to concatenate several strings in the raw format.
  - parsing performance is affected

**Which format should I use?** If you have the opportunity to select a
versioning scheme - stick with OSGi.

# External Links

  - [mozilla toolkit version
    format](https://developer.mozilla.org/En/Toolkit_version_format)
  - [rpm version
    comparison](http://linux.duke.edu/~mstenner/docs/rpm-version-cmp)
  - [sun spec version
    format](http://java.sun.com/j2se/1.5.0/docs/guide/deployment/deployment-guide/version-format.html)

[Version Type](Category:Equinox_p2 "wikilink")