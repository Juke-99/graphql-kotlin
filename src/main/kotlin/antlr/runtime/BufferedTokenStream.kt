package antlr.runtime

open class BufferedTokenStream : TokenStream {
  protected var tokenSource: TokenSource

  constructor(tokenSource: TokenSource) {
    if(tokenSource == null) throw NullPointerException("tokenSource cannot be null")
    this.tokenSource = tokenSource
  }
}
