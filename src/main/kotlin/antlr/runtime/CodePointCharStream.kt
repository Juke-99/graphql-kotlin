package antlr.runtime

import antlr.runtime.misc.Interval

import java.nio.charset.StandardCharsets

open abstract class CodePointCharStream(protected var position: Int = 0, protected val size: Int, protected val name: String) : CharStream {
  companion object {
    open fun fromBuffer(codePointBuffer: CodePointBuffer, name: String = IntStream.UNKNOWN_SOURCE_NAME): CodePointCharStream {
      when(codePointBuffer.getType()) {
        CodePointBuffer.Type.BYTE -> return CodePointByteCharStream(
          codePointBuffer.position(),
          codePointBuffer.remaining(),
          name,
          codePointBuffer.byteArray(),
          codePointBuffer.arrayOffset())

        CodePointBuffer.Type.CHAR -> return CodePoint2ByteCharStream(
          codePointBuffer.position(),
          codePointBuffer.remaining(),
          name,
          codePointBuffer.charArray(),
          codePointBuffer.arrayOffset())

        CodePointBuffer.Type.INT -> return CodePoint4ByteCharStream(
          codePointBuffer.position(),
          codePointBuffer.remaining(),
          name,
          codePointBuffer.intArray(),
          codePointBuffer.arrayOffset())
      }

      throw UnsupportedOperationException("Not reached")
    }

    private class CodePointByteCharStream : CodePointCharStream {
      private val byteArray: ByteArray

      constructor(
        position: Int,
        remaining: Int,
        name: String,
        byteArray: ByteArray,
        arrayOffset: Int) : super(position, remaining, name) {
          assert(arrayOffset == 0)
          this.byteArray = byteArray
      }

      override open fun getText(interval: Interval): String {
        val startIndex: Int = Math.min(interval.a, size)
        val length: Int = Math.min(interval.b - interval.a + 1, size - startIndex)

        return String(byteArray, startIndex, length, StandardCharsets.ISO_8859_1)
      }

      override open fun LA(i: Int): Int {
        val offset: Int

        when(Integer.signum(i)) {
          -1 -> {
            offset = position + 1

            if(offset < 0) return IntStream.EOF

            return byteArray[offset].toInt() and 0xFF
          }
          0 -> return 0
          1 -> {
            offset = position + i - 1

            if(offset >= size) return IntStream.EOF

            return byteArray[offset].toInt() and 0xFF
          }
        }

        throw UnsupportedOperationException("Not reached")
      }

      override fun getIntervalStorage(): Any = byteArray
    }

    private class CodePoint2ByteCharStream : CodePointCharStream {
      private val charArray: CharArray

      constructor(
        position: Int,
        remaining: Int,
        name: String,
        charArray: CharArray,
        arrayOffset: Int) : super(position, remaining, name) {
          this.charArray = charArray
          assert(arrayOffset == 0)
      }

      override open fun getText(interval: Interval): String {
        val startIndex: Int = Math.min(interval.a, size)
        val length: Int = Math.min(interval.b - interval.a + 1, size)

        return String(charArray, startIndex, length)
      }

      override open fun LA(i: Int): Int {
        val offset: Int

        when(Integer.signum(i)) {
          -1 -> {
            offset = position + 1

            if(offset < 0) return IntStream.EOF

            return charArray[offset].toInt() and 0xFFFF
          }
          0 -> return 0
          1 -> {
            offset = position + i - 1

            if(offset >= size) return IntStream.EOF

            return charArray[offset].toInt() and 0xFFFF
          }
        }

        throw UnsupportedOperationException("Not reached")
      }

      override fun getIntervalStorage(): Any = charArray
    }

    private class CodePoint4ByteCharStream : CodePointCharStream {
      private val intArray: IntArray

      constructor(
        position: Int,
        remaining: Int,
        name: String,
        intArray: IntArray,
        arrayOffset: Int) : super(position, remaining, name) {
          this.intArray = intArray
          assert(arrayOffset == 0)
      }

      override open fun getText(interval: Interval): String {
        val startIndex: Int = Math.min(interval.a, size)
        val length: Int = Math.min(interval.b - interval.a + 1, size - startIndex)

        return String(intArray, startIndex, length)
      }

      override open fun LA(i: Int): Int {
        val offset: Int

        when(Integer.signum(i)) {
          -1 -> {
            offset = position + 1

            if(offset < 0) return IntStream.EOF

            return intArray[offset]
          }
          0 -> return 0
          1 -> {
            offset = position + i - 1

            if(offset >= size) return IntStream.EOF

            return intArray[offset]
          }
        }

        throw UnsupportedOperationException("Not reached")
      }

      override fun getIntervalStorage(): Any = intArray
    }
  }

  override open fun consume() {
    if(size - position == 0) throw IllegalStateException("cannot consume EOF")

    position = position + 1
  }

  override open fun index(): Int = position
  override open fun size(): Int = size
  override open fun mark(): Int = -1

  override open fun relese(marker: Int) {}
  override open fun seek(index: Int) {
    position = index
  }

  override open fun getSourceName(): String {
    if(name == null || name.isEmpty()) return IntStream.UNKNOWN_SOURCE_NAME
    return name
  }

  override open fun toString(): String = getText(Interval.of(0, size - 1))

  abstract fun getIntervalStorage(): Any
  override abstract fun getText(interval: Interval): String
}
