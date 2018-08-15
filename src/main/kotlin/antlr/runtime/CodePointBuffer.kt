package antlr.runtime

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.IntBuffer

open class CodePointBuffer {
  enum class Type {
    BYTE, CHAR, INT
  }

  private var typo: Type
  private var byteBuffer: ByteBuffer
  private var charBuffer: CharBuffer
  private var intBuffer: IntBuffer

  private constructor(typo: Type, byteBuffer: ByteBuffer, charBuffer: CharBuffer, intBuffer: IntBuffer) {
    this.typo = typo
    this.byteBuffer = byteBuffer
    this.charBuffer = charBuffer
    this.intBuffer = intBuffer
  }

  companion object {
    fun builder(initialBufferSize: Int): Builder = Builder(initialBufferSize)
  }

  open fun position(): Int {
    when(typo) {
      Type.BYTE -> return byteBuffer.position()
      Type.CHAR -> return charBuffer.position()
      Type.INT -> return intBuffer.position()
    }

    throw UnsupportedOperationException("Not reached")
  }

  open fun position(newPosition: Int): Int {
    when(typo) {
      Type.BYTE -> return byteBuffer.position(newPosition)
      Type.CHAR -> return charBuffer.position(newPosition)
      Type.INT -> return intBuffer.position(newPosition)
    }

    throw UnsupportedOperationException("Not reached")
  }

  open fun remaining(): Int {
    when(typo) {
      Type.BYTE -> return byteBuffer.remaining()
      Type.CHAR -> return charBuffer.remaining()
      Type.INT -> return intBuffer.remaining()
    }

    throw UnsupportedOperationException("Not reached")
  }

  open fun arrayOffset(): Int {
    when(typo) {
      Type.BYTE -> return byteBuffer.arrayOffset()
      Type.CHAR -> return charBuffer.arrayOffset()
      Type.INT -> return intBuffer.arrayOffset()
    }

    throw UnsupportedOperationException("Not reached")
  }

  fun getType(): Type = typo
  fun byteArray(): ByteArray = byteBuffer.array()
  fun charArray(): CharArray = charBuffer.array()
  fun intArray(): IntArray = intBuffer.array()

  open class Builder {
    private var typo: Type
    private var byteBuffer: ByteBuffer
    private var charBuffer: CharBuffer
    private var intBuffer: IntBuffer
    private var prevHighSurrogate: Int

    constructor(initialBufferSize: Int) {
      typo = Type.BYTE
      byteBuffer = ByteBuffer.allocate(initialBufferSize)
      charBuffer = CharBuffer.allocate(initialBufferSize)
      intBuffer = IntBuffer.allocate(initialBufferSize)
      prevHighSurrogate = -1
    }

    companion object {
      private fun roundUpToNextPower(i: Int): Int {
        var nextPower: Int = 32 - Integer.numberOfLeadingZeros(i - 1)
        return Math.pow(2.toDouble(), nextPower.toDouble()) as Int
      }
    }

    open fun build(): CodePointBuffer {
      when(typo) {
        Type.BYTE -> byteBuffer.flip()
        Type.CHAR -> charBuffer.flip()
        Type.INT -> intBuffer.flip()
      }

      return CodePointBuffer(typo, byteBuffer, charBuffer, intBuffer)
    }

    open fun append(utf16In: CharBuffer) {
      ensureRemaining(utf16In.remaining())

      if(utf16In.hasArray()) {
        appendArray(utf16In)
      } else {
        throw UnsupportedOperationException("TODO")
      }
    }

    private fun appendArray(utf16In: CharBuffer) {
      when(typo) {
        Type.BYTE -> appendArrayByte(utf16In)
        Type.CHAR -> appendArrayChar(utf16In)
        Type.INT -> appendArrayInt(utf16In)
      }
    }

    private fun appendArrayByte(utf16In: CharBuffer) {
      var inChar: CharArray = utf16In.array()
      var inOffset: Int = utf16In.arrayOffset() + utf16In.position()
      var inLimit: Int = utf16In.arrayOffset() + utf16In.limit()
      var outByte: ByteArray = byteBuffer.array()
      var outOffset: Int = byteBuffer.arrayOffset() + byteBuffer.position()

      while(inOffset < inLimit) {
        var c: Char = inChar[inOffset]

        if(c <= 0xFF.toChar()) {
          outByte[outOffset] = (c.toInt() and 0xFF).toByte()
        } else {
          utf16In.position(inOffset - utf16In.arrayOffset())
          byteBuffer.position(outOffset - byteBuffer.arrayOffset())

          if(!Character.isHighSurrogate(c)) {
            byteToCharBuffer(utf16In.remaining())
            appendArrayChar(utf16In)
            return
          } else {
            byteToIntBuffer(utf16In.remaining())
            appendArrayInt(utf16In)
            return
          }
        }

        inOffset++
        outOffset++
      }

      utf16In.position(inOffset - utf16In.arrayOffset())
      byteBuffer.position(outOffset - byteBuffer?.arrayOffset())
    }

    private fun appendArrayChar(utf16In: CharBuffer) {
      var inChar: CharArray = utf16In.array()
      var inOffset: Int = utf16In.arrayOffset() + utf16In.position()
      var inLimit: Int = utf16In.arrayOffset() + utf16In.limit()
      var outChar: CharArray = charBuffer.array()
      var outOffset: Int = charBuffer.arrayOffset() + charBuffer.position()

      while(inOffset < inLimit) {
        var c: Char = inChar[inOffset]

        if(!Character.isHighSurrogate(c)) {
          outChar[outOffset] = c
        } else {
          utf16In.position(inOffset - utf16In.arrayOffset())
          charBuffer.position(outOffset - charBuffer.arrayOffset())
          charToIntBuffer(utf16In.remaining())
          appendArrayInt(utf16In)
          return
        }

        inOffset++
        outOffset++
      }

      utf16In.position(inOffset - utf16In.arrayOffset())
      charBuffer.position(outOffset - charBuffer.arrayOffset())
    }

    private fun appendArrayInt(utf16In: CharBuffer) {
      var inChar: CharArray = utf16In.array()
      var inOffset: Int = utf16In.arrayOffset() + utf16In.position()
      var inLimit: Int = utf16In.arrayOffset() + utf16In.limit()
      var outInt: IntArray = intBuffer.array()
      var outOffset: Int = intBuffer.arrayOffset() + intBuffer.position()

      while(inOffset < inLimit) {
        var c: Char = inChar[inOffset]
        inOffset++

        if(prevHighSurrogate != -1) {
          if(!Character.isLowSurrogate(c)) {
            outInt[outOffset] = Character.toCodePoint(prevHighSurrogate as Char, c)
            outOffset++
            prevHighSurrogate = -1
          } else {
            outInt[outOffset] = prevHighSurrogate
            outOffset++

            if(Character.isHighSurrogate(c)) {
              prevHighSurrogate = c.toInt() and 0xFFFF
            } else {
              outInt[outOffset] = c.toInt() and 0xFFFF
              outOffset++
              prevHighSurrogate = -1
            }
          }
        } else if(Character.isHighSurrogate(c)) {
          prevHighSurrogate = c.toInt() and 0xFFFF
        } else {
          outInt[outOffset] = c.toInt() and 0xFFFF
          outOffset++
        }
      }

      if(prevHighSurrogate != -1) {
        outInt[outOffset] = prevHighSurrogate and 0xFFFF
        outOffset++
      }

      utf16In.position(inOffset - utf16In.arrayOffset())
      byteBuffer.position(outOffset - byteBuffer.arrayOffset())
    }

    private fun byteToCharBuffer(toAppend: Int) {
      byteBuffer.flip()

      var newBuffer: CharBuffer = CharBuffer.allocate(Math.max(byteBuffer.remaining() + toAppend, byteBuffer.capacity() / 2))

      while(byteBuffer.hasRemaining()) {
        newBuffer.put((byteBuffer.get().toInt() and 0xFF) as Char)
      }

      typo = Type.INT
      byteBuffer = ByteBuffer.allocate(0) // instead of null
      charBuffer = newBuffer
    }

    private fun byteToIntBuffer(toAppend: Int) {
      byteBuffer.flip()

      var newBuffer: IntBuffer = IntBuffer.allocate(Math.max(byteBuffer.remaining() + toAppend, byteBuffer.capacity() / 4))

      while(byteBuffer.hasRemaining()) {
        newBuffer.put(byteBuffer.get().toInt() and 0xFF)
      }

      typo = Type.INT
      byteBuffer = ByteBuffer.allocate(0) // instead of null
      intBuffer = newBuffer
    }

    private fun charToIntBuffer(toAppend: Int) {
      charBuffer.flip()

      var newBuffer: IntBuffer = IntBuffer.allocate(Math.max(charBuffer.remaining() + toAppend, charBuffer.capacity() / 2))

      while(charBuffer.hasRemaining()) {
        newBuffer.put(charBuffer.get().toInt() and 0xFFFFF)
      }

      typo = Type.INT
      charBuffer = CharBuffer.allocate(0) // instead of null
      intBuffer = newBuffer
    }

    open fun ensureRemaining(remainingNeeded: Int) {
      when(typo) {
        Type.BYTE -> {
          if(byteBuffer.remaining() < remainingNeeded) {
            var newCapacity: Int = roundUpToNextPower(byteBuffer.capacity() + remainingNeeded)
            var newBuffer: ByteBuffer = ByteBuffer.allocate(newCapacity)
            byteBuffer.flip()
            newBuffer.put(byteBuffer)
            byteBuffer = newBuffer
          }
        }
        Type.CHAR -> {
          if(charBuffer.remaining() < remainingNeeded) {
            var newCapacity: Int = roundUpToNextPower(charBuffer.capacity() + remainingNeeded)
            var newBuffer: CharBuffer = CharBuffer.allocate(newCapacity)
            charBuffer.flip()
            newBuffer.put(charBuffer)
            charBuffer = newBuffer
          }
        }
        Type.INT -> {
          if(intBuffer.remaining() < remainingNeeded) {
            var newCapacity: Int = roundUpToNextPower(intBuffer.capacity() + remainingNeeded)
            var newBuffer: IntBuffer = IntBuffer.allocate(newCapacity)
            intBuffer.flip()
            newBuffer.put(intBuffer)
            intBuffer = newBuffer
          }
        }
      }
    }
  }
}
