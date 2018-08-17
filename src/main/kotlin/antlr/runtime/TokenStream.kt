package antlr.runtime

open interface TokenSource : IntStream {
  open fun LT(k: Int): Token
  open fun get(index: Int): Token
  open fun getTokenSource(): TokenSource
  open fun getText(): String
  open fun getText(context: RuleContext): String
  open fun getText(start: Token, stop: Token): String
}
