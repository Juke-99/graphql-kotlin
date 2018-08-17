package antlr.runtime

open class CommonTokenStream(tokenSource: TokenSource, protected channel: Int = Token.DEFAULT_CHANNEL) : BufferedTokenStream(tokenSource) {

}
