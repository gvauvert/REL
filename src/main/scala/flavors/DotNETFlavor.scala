package fr.splayce.rel.flavors

import fr.splayce.rel._
import util.{Flavor, Rewriter}


/**  ․NET flavor
 *
 *  This flavor:
 *  - embedds group names in regex (`(?<name>expr)` / `\k<name>` syntax)
 *  - convert possessive quantifiers to greedy in atomic groups
 *  - translate `\w` (when referenced by `μ` / `Word`) into `[a-zA-Z0-9_]`
 *    because .NET's `\w` would also matches letters with diacritics
 *    while Java's `\w` only matches ASCII letters
 *    (use `\p{L}` insead with `λ` / `Letter` for all Unicode letters)
 */
object DotNETFlavor extends Flavor(".NET") {

  private val ASCIIWord    = new TranslatedRECst("[a-zA-Z0-9_]")
  private val NotASCIIWord = new TranslatedRECst("[^a-zA-Z0-9_]")

  override lazy val translator: Rewriter = {

    // named groups & their references
    case    Group(name, re) => Wrapper(re map translator, "(?<" + name + ">", ")", List(name))
    case GroupRef(name)     => new TranslatedREStr("""\k<""" + name + ">")

    // .NET's \w would also match letters with diacritics
    case Word               => ASCIIWord
    case NotWord            => NotASCIIWord

    // Also, no possessive quantifiers
    case rep: Rep if rep.mode == Possessive => possessiveToAtomic(translator)(rep)
  }

}