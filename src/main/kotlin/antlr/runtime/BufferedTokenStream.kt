package antlr.runtime

open class BufferedTokenStream : TokenStream {
  protected var tokenSource: TokenSource
  protected var p: Int = -1
  protected var fetchEOF: Boolean

  constructor(tokenSource: TokenSource) {
    if(tokenSource == null) throw NullPointerException("tokenSource cannot be null")
    this.tokenSource = tokenSource
  }

  override open fun getTokenSource(): TokenSource = tokenSource
  override open fun index(): Int = p
  override open fun mark(): Int = 0
  override open fun relese(marker: Int) {}

  override open fun seek(index: Int) {
    lazyInit()
    p = adjustSeekIndex(index)
  }

  override open fun size(): Int = tokens.size()

  override open fun consume() {
    var skipEofCheck: Boolean

    if(p >= 0) {
      if(fetchEOF) skipEofCheck = p < tokens.size() - 1
      else skipEofCheck = p < tokens.size()
    } else {
      skipEofCheck = false
    }

    if(!skipEofCheck && LA(1) == EOF) throw IllegalStateException("cannot consume EOF")
    if(sync(p + 1)) p = adjustSeekIndex(p + 1)
  }

  override open fun get(i: Int): Token {
    if(i < 0 || i >= tokens.size()) throw IndexOutOfBoundsException("token index " + i + " out of range 0.." + (tokens.size() - 1))
    return tokens.get(i)
  }

  open fun get(start: Int, stop: Int): List<Token> {
    if(start < 0 || stop < 0) return null
    lazyInit()
    val subset: List<Token> = ArrayList<Token>()
    if(stop >= tokens.size()) stop = tokens.size() - 1
    var i: Int = 0

    while(i <= stop) {
      val t: Token = tokens.get(i)
      if(t.getType() == Token.EOF) break
      subset.add(t)
    }

    return subset
  }

  override open fun LA(i: Int): Token = LT(i).getType()

  override open fun LT(k: Int): Token {
    lazyInit()

    if(k == 0) return null
    if(k < 0) return LB(-k)

    val i: Int = p + k - 1
    sync(i)

    if(i >= tokens.size()) return tokens.get(tokens.size() - 1)

    return tokens.get(i)
  }

  override open fun getSourceName(): String = tokenSource.getSourceName()

  override open fun getText(): String = getText(Interval.of(0, size() - 1))

  override open fun getText(interval: Interval): String {
    var start: Int = Interval.a
    var stop: Int = Interval.b

    if(start < 0 || stop < 0) return ""
    fill()

    if(stop >= tokens.size()) stop = tokens.size() - 1

    val buffer: StringBuilder = StringBuilder()
    var i: Int = start

    while(i <= stop) {
      val t: Token = tokens.get(i)

      if(t.getType() == Token.EOF) break

      buffer.append(t.getText())

      i++
    }

    return buffer.toString()
  }

  override open fun getText(context: RuleContext): String = getText(context.getSourceInterval())

  override open fun getText(start: Token, stop: Token): String {
    if(start != null && stop != null) return getText(Interval.of(start.getTokenIndex(), stop.getTokenIndex()))
    return ""
  }

  protected fun lazyInit() {
    if(p == -1) setup()
  }

  protected fun setup() {
    sync(0)
    p = adjustSeekIndex(0)
  }

  protected fun sync(i: Int): Boolean {
    assert(i >= 0)
    val n: Int = i - tokens.size() + 1
    if(n > 0) {
      val fetch: Int = fetch(n)
      return fetch >= n
    }

    return true
  }

  protected fun adjustSeekIndex(i: Int): Int = i

  open fun fill() {
    lazyInit()

    val blockSize: Int = 1000

    while(true) {
      val fetched: Int = fetch(blockSize)

      if(fetched < blockSize) return
    }
  }
}
