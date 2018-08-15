package antlr.runtime

import java.nio.CharBuffer

open class CharStreams {
  companion object {
    fun fromString(input: String, sourceName: String = IntStream.UNKNOWN_SOURCE_NAME): CodePointCharStream {
      var codePointBufferBuilder: CodePointBuffer.Builder = CodePointBuffer.builder(input.length)
      var charBuffer: CharBuffer = CharBuffer.allocate(input.length)
      charBuffer.put(input)
      charBuffer.flip()
      codePointBufferBuilder.append(charBuffer)

      return CodePointCharStream.fromBuffer(codePointBufferBuilder.build(), sourceName)
    }
  }
}
