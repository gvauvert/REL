# REL, a Regular Expression composition Library

REL is a small utility Scala library for people dealing with complex, modular regular expressions. It defines a DSL with most of the operators you already know and love. This allows you to isolate portions of your regex for easier testing and reuse.

Consider the following YYYY-MM-DD date regex: `^(?:19|20)\d\d([- /.])(?:0[1-9]|1[012])\1(?:0[1-9]|[12]\d|3[01])$`. It is a bit more readable and reusable expressed like this:

```scala
import fr.splayce.rel._
import Implicits._

val sep     = "[- /.]" \ "sep"            // group named "sep"
val year    = ("19" | "20") ~ """\d\d"""  // ~ is concatenation
val month   = "0[1-9]" | "1[012]"
val day     = "0[1-9]" | "[12]\\d" | "3[01]"
val dateYMD = ^ ~ year  ~ sep ~ month ~ !sep ~ day  ~ $
val dateMDY = ^ ~ month ~ sep ~ day   ~ !sep ~ year ~ $
```

These value are `RE` objects (also named _terms_ or _trees_/_subtrees_), which can be converted to `scala.util.matching.Regex` instances either implicitly (by importing `rel.Implicits._`) or explicitly (via the `.r` method).

The embedded [Date regexes](https://github.com/Imaginatio/REL/blob/master/src/main/scala/matchers/Date.scala) and [extractors](https://github.com/Imaginatio/REL/blob/master/src/main/scala/matchers/DateExtractor.scala) will give you more complete examples, matching several date formats at once with little prior knowledge.

### Syntax

> Examples are noted `DSL expression` → `resulting regex`. They assume:
> ```scala
> import fr.splayce.rel._
> import Implicits._
> val a = RE("aa")
> val b = RE("bb")
> ```

- Concatenation:
    - Protected:   `a ~ b` → `(?:aa)(?:bb)`
    - Unprotected: `a - b` → `aabb`
- Alternative: `a | b` → `aa|bb`
- Option:
    - [greedy](http://www.regular-expressions.info/repeat.html#greedy) `a.?` → `(?:aa)?` ; you can also skip the dot `a ?` but the former has clearer priority in a complex expression
    - [reluctant / lazy](http://www.regular-expressions.info/repeat.html#lazy): `a.??` → `(?:aa)??`
    - [possessive](http://www.regular-expressions.info/possessive.html): `a.?+` → `(?:aa)?+`
- Repeat:
    - At least one:
        - greedy:     `a.+`  → `(?:aa)+`
        - reluctant:  `a.+?` → `(?:aa)+?`
        - possessive: `a.++` → `(?:aa)++`
    - Any number:
        - greedy:     `a.*`  → `(?:aa)*`
        - reluctant:  `a.*?` → `(?:aa)*?`
        - possessive: `a.*+` → `(?:aa)*+`
    - In range:
        - greedy:     `a(1,3)` or `a(1 to 3)` or `a(1 -> 3)` → `(?:aa){1,3}`
        - reluctant:  `a(1, 3, Reluctant)`  → `(?:aa){1,3}?`
        - possessive: `a(1, 3, Possessive)` → `(?:aa){1,3}+`
    - At most:
        - greedy:     `a < 3`   → `(?:aa){0,3}`
        - reluctant:  `a.<?(3)` → `(?:aa){0,3}?` (dotted form `a.<?(3)` is mandatory, standalone `<?` being syntactically significant in Scala: `XMLSTART`)
        - possessive: `a <+ 3`  → `(?:aa){0,3}+`
    - At least:
        - greedy:     `a > 3`  → `(?:aa){3,}`
        - reluctant:  `a >? 3` → `(?:aa){3,}?`
        - possessive: `a >+ 3` → `(?:aa){3,}+`
    - Exactly: `a{3}` or `a(3)` → `(?:aa){3}`
- Lookaround:
    - Lookahead:           `?=(a)`  or `a.?=`  → `(?=aa)`
    - Lookbehind:          `?<=(a)` or `a.?<=` → `(?<=aa)`
    - Negative lookahead:  `?!(a)`  or `a.?!`  → `(?!aa)`
    - Negative lookbehind: `?<!(a)` or `a.?<!` → `(?<!aa)`
- Grouping:
    - Named: `a \ "group_a"` → `(aa)`; the name `group_a` will be passed to the `Regex` constructor,  queryable on corresponding `Match`es
    - Unnamed: `a.g` → `(aa)` (a unique group name is generated internally)
    - Non-capturing: `a.ncg` → `(?:aa)` or the short syntax `a.%`; will try not to uselessly wrap non-breaking entities (i.e. single letters like `a` or `\u00F0` and character classes like `\w`, `[^a-z]` or `\p{Lu}`) to produce ever-so-slightly less unreadable output
    - Non-capturing, with `idmsux-idmsux` flags: `a.ncg("i-d")` → `(?i-d:aa)` or infix syntax `"i" ?: a` → `(?i:aa)`; will also try to combine nested flags, deepest wins: `a.ncg("-d").ncg("di")` → `(?i-d:aa)`
    - [Atomic](http://www.regular-expressions.info/atomic.html): `a.ag` → `(?>aa)` or the short syntaxes `?>(a)` and `a.?>`
- Back-reference: `!g` will insert a back-reference on group `g`; e.g. `val g = (a|b).g; g - a - !g` → `(aa|bb)aa\1`

### Constants

A few "constants" (expression terms with no repetitions, capturing groups, or unprotected alternatives) are also pre-defined. Some of them have a UTF-8 Greek symbol alias for conciseness (import `rel.Symbols._` to use them), uppercase for negation. You can add your own by instancing case class `RECst(expr)`

- `Epsilon` or `ε` → empty string
- `Dot` or `τ` → `.`,          `LineTerminator` or `Τ`* → `(?:\r\n?|[\n\u0085\u2028\u2029])` ([line terminators](http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#lt))
- `MLDot` or `ττ` → `[\s\S]` (will match any char, including line terminators, even when the [`DOTALL`](http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#DOTALL) or [`MULTILINE`](http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#MULTILINE) modes are disabled)
- `AlphaLower` → `[a-z]`,      `AlphaUpper` → `[A-Z]`
- `Alpha` or `α` → `[a-zA-Z]`, `NotAlpha` or `Α`* → `[^a-zA-Z]`
- `LetterLower` → `\p{Ll}`,    `LetterUpper` → `\p{Lu}` (unicode letters, including diacritics)
- `Letter` or `λ` → `\p{L}`,   `NotLetter` or `Λ` → `\P{L}`
- `Digit` or `δ` → `\d`,       `NotDigit` or `Δ` → `\D`
- `WhiteSpace` or `σ` → `\s`,  `NotWhiteSpace` or `Σ` → `\S`
- `Word` or `μ` → `\w` (`Alpha` or `_`), `NotWord` or `Μ`* → `\W`
- `WordBoundary` or `ß` → `\b`, `NotWordBoundary` or `Β`* → `\B`
- `LineBegin` or `^` → `^`,     `LineEnd` or `$` → `$`
- `InputBegin` or `^^` → `\A`,  `InputEnd` or `$$` → `\z`

_\* Those are uppercase `α`/`ß`/`μ`/`τ`, not latin `A`/`B`/`M`/`T`_

### Exporting regexes (and other regex flavors)

The `.r` method on any `RE` term returns a compiled `scala.util.matching.Regex`. The `.toString` method returns the source pattern (equivalent to `.r.toString`, so the pattern is verified).

For other regex flavors, a translation mechanism is provided: you may instantiate a [`Flavor`](https://github.com/Imaginatio/REL/blob/master/src/main/scala/util/Flavor.scala), which exposes two methods: `.express(re: RE)` and `.translate(re: RE)`. The first one returns a `Tuple2[String, List[String]]`, whose first element is the translated regex string and whose second is a list of the group names (in order of appearance) allowing you to perform a mapping to capturing group indexes (like Scala does) if needed. The second method only performs the translation of a `RE` term into another.

An example of translation into [.NET-flavored regex](http://www.regular-expressions.info/dotnet.html) is provided. [`DotNETFlavor`](https://github.com/Imaginatio/REL/blob/master/src/main/scala/flavors/DotNETFlavor.scala):

- changes `\w` to `[a-zA-Z0-9_]` (when used with `Word`/`μ`), since .NET's `\w` covers UTF-8 letters including accented, while Java's covers only ASCII
- turns any possessive quantifier into a greedy quantifier wrapped in an atomic group (which is a longer equivalent)
- inlines named groups and their references into the .NET `(?<name>expr)` syntax

Another example is the [`JavaScriptFlavor`](https://github.com/Imaginatio/REL/blob/master/src/main/scala/flavors/JavaScriptFlavor.scala), which will:

- throw an exception when you try to translate a `RE` term that is not supported in the [JavaScript regex flavor](http://www.regular-expressions.info/javascript.html) (e.g. LookBehind)
- transform possessive quantifiers and atomic groups (also not supported in JavaScript) into LookAhead with capturing group, immediately referenced afterwards: `(?>a|b)` becomes `(?=(a|b))\1`, exposing the same behavior as possessive quantifiers and atomic groups

The [`LegacyRubyFlavor`](https://github.com/Imaginatio/REL/blob/master/src/main/scala/flavors/LegacyRubyFlavor.scala) works similarly for the [Ruby 1.8 regex flavor](http://www.regular-expressions.info/ruby.html) which [does not support Unicode](http://www.regular-expressions.info/unicode8bit.html), unlike [Oniguruma](http://www.geocities.jp/kosako3/oniguruma/) when the `/u` flag is set. You shouldn't need to translate a REL regex to use it with Oniguruma, which also fully supports LookBehind and possessive quantifiers, and is the default regex implementation in Ruby 1.9.

[Regular-expression.info](http://www.regular-expressions.info)'s [regex flavors comparison chart](http://www.regular-expressions.info/refflavors.html) may be of use when writing a translation.

### Capturing Groups

Since a REL term is a tree, it can compute the resulting capturing groups tree with the `matchGroup` val, containing a tree of `MatchGroup`s. The top group corresponds to the entire match: it is unnamed, contains the matched content and has the first-level capturing groups nested as subgroups. When applied to a `Match`, it returns a copy of the capturing groups tree with the content filled for each group that matched. Thus, you can use pattern matching with nested groups to extract any group at several levels of imbrication with little code.

For example, let's say we want to match simple usernames that have the form `user@machine` where both part have only alphabetic characters. We can define the regex:

```scala
val user     = α.+ \ "user"
val at       = "@"
val machine  = α.+ \ "machine"
val username = (user - at - machine) \ "username"
```

And make a simple extractor that yields a tuple of Strings:

```scala
val userMatcher: PartialFunction[MatchGroup, (String, String)] = {
  case MatchGroup(None, Some(_), List(              // this is the full match ($0)
      MatchGroup(Some("username"), Some(_), List(   // $1 / username
        MatchGroup(Some("user"),    Some(u), Nil),  // $2 / user
        MatchGroup(Some("machine"), Some(m), Nil)   // $3 / machine
      ))
    )) => (u, m)
}
```

Extraction in a String can be done like this:

```scala
import ByOptionExtractor._                    // lift (and toPM on further examples)
val userExtractor = username << lift(userMatcher)
val users = userExtractor("me@dev, you@dev")  // Iterator[(String, String)]
users.toList.toString === "List((me,dev), (you,dev))"
```

Java < 7 does not support named capturing groups, and Scala only emulates them, mapping a list of names given at the compilation of the Regex against the indexes of the capturing groups. Thus, it is risky to have multiple instances of the same group name. In practice, using `myMatch.group("myGroup")` seems to always refer to the last occurrence of the `myGroup`.

On the other hand, the `Match` object carries the full list of group names (in its eponymous `groupNames` val), and REL uses it to compute the group tree. Thus, you _can_ reuse the same group name in a single expression.

Say we want to extract items formatted with `username->username`:

```scala
val interaction = username - "->" - username
val iaMatcher: PartialFunction[MatchGroup, (String, String)] = {
  case MatchGroup(None, Some(_), List(
      MatchGroup(Some("username"), Some(un1), _),
      MatchGroup(Some("username"), Some(un2), _)
    )) => (un1, un2)
}
val iaExtractor = interaction << lift(iaMatcher)
val interactions = iaExtractor("me@dev->you@dev, you@dev->me@dev")
interactions.toList.toString === "List((me@dev,you@dev), (you@dev,me@dev))"
```

You can of course reuse the same extractor, which can directly provide the extracted object. This requires us to place the extractor one level deeper to avoid the `$0` group:

```scala
val userMatcher2: PartialFunction[MatchGroup, (String, String)] = {
  case MatchGroup(Some("username"), Some(_), List(
        MatchGroup(Some("user"),    Some(u), Nil),
        MatchGroup(Some("machine"), Some(m), Nil)
    )) => (u, m)
}
val userPattern = toPM(lift(userMatcher2))
val iaMatcher2: PartialFunction[MatchGroup, (String, String, String, String)] = {
  case MatchGroup(None, Some(_), List(
      userPattern(u1, m1),
      userPattern(u2, m2)
    )) => (u1, m1, u2, m2)
}
val iaExtractor2 = interaction << lift(iaMatcher2)
val interactions2 = iaExtractor2("me@dev->you@dev, you@dev->me@dev")
interactions2.toList.toString === "List((me,dev,you,dev), (you,dev,me,dev))"
```

### Pre-match cleanup

Cleaners are mostly `String => String` functions. They aim to provide a composable way to perform some cleaning before matching a `String` against a Regex. For instance, when matching against multiple case-insensitive regex, it can be more efficient to make those regex case-sensitive (say lowercase only) and downcase the input beforehand. You may also want to reduce the combinations in your regexes to keep them simple and focused, or just keep the input from being too exotic.

Some `Cleaners` are already provided:

- `LowerCaseFilter`
- `TrimFilter`
- `LineSeparatorNormalizer` normalizes line breaks to `\n` (from `\r\n` but also Unicode paragraph separator and mandatory breaks)
- `WhiteSpaceNormalizer` normalizes all Unicode spaces to ASCII spaces
- `WhiteSpaceCleaner` transforms `\s+` to a single space and `AllWhiteSpaceCleaner` does the same with all Unicode spaces and line breaks
- `CamelCaseSplitFilter` splits camel case `oneTwo` into `one Two` (but not `iOS` – requires lower-UPPER-lower) 
- `SingleQuoteNormalizer` normalizes Unicode single quotes like `‘’′` to ASCII straight quote `'`; `DoubleQuoteNormalizer` does the same with double quotes and `QuoteNormalizer` with both
- `DiacriticCleaner` converts most common accented and combined letters to ASCII: `é` to `e`, `œ` to `oe`, `ﬀ` to `ff`, etc.
- `FullwidthNormalizer` normalizes CJK Fullwidth characters to their ASCII equivalents

You can implement you own `Cleaner`. If it can be a regex replacement, it is recommended to use `Cleaner.regexReplaceAll` or `Cleaner.regexReplaceFirst`.

Chaining Cleaners unix-style: `CamelCaseSplitFilter | LowerCaseFilter | DiacriticCleaner` (or with `compose`/`andThen`), yields a new compound Cleaner.

Those alterations can make it harder to retrieve the original matched text and its position in the original String. If you have such a need, you can use wrap your `String` in a [`TrackString`](https://github.com/Imaginatio/REL/blob/master/src/main/scala/util/TrackString.scala) and then run it through the Cleaners. Its `srcPos(oStart, oEnd)`, when given the positions in the cleaned String, returns the corresponding `Interval(iStart, iEnd)` in the original String. If you implement you own `Cleaner` and if it may change the length of the `String`, you will need to implement the corresponding `TrackString => TrackString` (except if you use `Cleaner.regexReplaceAll/First`).


## TODO

- Core
    - Add character range support (at DSL level), with inversion (`[^...]`)
    - Compatibility with Scala Parsers?
    - Consider using `'symbols` for group names
    - Java 6/7 flavors: detect & fail on unbounded repeats in LookBehind ?
    - Parse \[and limit] regex strings inputted to REL, producing REL-only expression trees, thus eliminating some known issues (see below) and opening some possibilities (e.g. generating sample matching strings)
- Matchers
    - date: consider extracting incorrect dates (like feb. 31st) with some flag
- Utils
    - Generate sample strings that match a regex (e.g. with [Xeger](http://code.google.com/p/xeger/))
    - Source generation or compiler plugin to enable REL independence \[at runtime]
    - Binary tool that would take a REL file, compile it and produce regexes in several flavors / programming languages
- Documentation
    - Document cleaners, extractors, matchers, flavors
    - Make the present document a simple description and split the documentation part into several linked pages: syntax, matchers, extractors, flavors…


## Known issues

### Versioning

REL version number follows the [Semantic Versioning 2.0 Specification](http://semver.org/). In the current early stage of development, the API is still unstable and backward compatibility may break.
As an additional rule, in version `0.Y.Z`, a `Z`-only version change is expected to be backward compatible with previous `0.Y.*` versions. But a `Y` version change potentially breaks backward compatibility.

### String primitives

The string primitives are not parsed (use `esc(str)` to escape a string that should be matched literally). Hence:

- Any capturing group you pass inside those strings won't be taken into account by REL when the final regex is generated. The following groups and back-references will be shifted so the resulting regex will most probably be incorrect.
- You still need to escape your expressions to match literally characters that are regex-significant like `+`, `?` or `(`, even in `RECst`. Use `esc(str)` to escape the whole string.
- Any regex you pass as a string will be kept as-is when translated into different flavors. For instance, the `\w` passed in a string (as opposed to used with `Word`/`μ`) will not be translated by the `DotNETFlavor`.

### Flavors

JavaScript regexes are very limited and work a bit differently. In [JavaScript flavor](https://github.com/Imaginatio/REL/blob/master/src/main/scala/flavors/JavaScriptFlavor.scala)

- `WordBoundary`/`\b` is kept as-is, but will not have exactly the same semantic because of the lack of Unicode support in JavaScript regex flavor. For instance, in `"fiancé"`, Javascript sees `"\bfianc\bé"` where most other flavors see `"\bfiancé\b"`. Same goes for `NotWordBoundary`/`\B`.
- `InputBegin` (`^^`) and `InputEnd` (`$$`) are translated to `LineBegin` (`^`) and `LineEnd` (`$`), but this is only correct if the `m` (multiline) flag is off.

### TrackString

Regex replacement do not support Java 7 embedded group names, which are not accessible in Scala's `Match` yet. It will use Scala group names instead (inconsistent with `String#replaceAll`).

`TrackString` cannot track intertwined/reordered replacements, i.e. you can only track `abc` => `bca` as a single group (as opposed to three reordered groups). If out-of-order `Repl`/`Subst` are introduced, `srcPos` will most probably yield incorrect results.


## Usage and downloads

- download the [source from github](https://github.com/Imaginatio/REL) and build the library with SBT
- download the [latest binary release](https://github.com/Imaginatio/Maven-repository/tree/master/fr/splayce/)
- use [our public Maven repository](https://github.com/Imaginatio/Maven-repository/)


## License

Copyright &copy; 2012 Imaginatio SAS

REL is released under the [MIT License](http://www.opensource.org/licenses/MIT)


## Authors

REL was developed by [Adrien Lavoillotte](http://instanceof.me/) ([@streetpc](https://github.com/streetpc)), Julien Martin and Guillaume Vauvert ([@gvauvert](https://github.com/gvauvert)) for project [Splayce](http://splayce.com) at [Imaginatio](http://imaginatio.fr)
