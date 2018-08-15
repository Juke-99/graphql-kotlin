package antlr.runtime

open interface IntStream {
  companion object {
    val EOF: Int = -1
    val UNKNOWN_SOURCE_NAME: String = "<unknown>"
  }

  fun consume()
  fun LA(i: Int): Int
  fun mark(): Int
  fun relese(marker: Int)
  fun index(): Int
  fun seek(index: Int)
  fun size(): Int
  fun getSourceName(): String
}
