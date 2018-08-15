package antlr.runtime.misc

open class Interval {
  open var a: Int
  open var b: Int

  constructor(a: Int, b: Int = a) {
    this.a = a
    this.b = b
  }

  companion object {
    open val INTERVAL_POOL_MAX_VALUE: Int = 1000
    var cache: Array<Interval> = Array<Interval>(INTERVAL_POOL_MAX_VALUE + 1, {a -> Interval(a, a)})

    open fun of(a: Int, b: Int): Interval {
      if(a != b || a == 0 || a > INTERVAL_POOL_MAX_VALUE) return Interval(a, b)
      if(cache[a] == null) cache[a] = Interval(a, a)

      return cache[a]
    }
  }

  override open fun equals(any: Any?): Boolean {
    if(any == null || !(any is Interval)) return false

    val other: Interval = any as Interval
    return this.a == other.a && this.b == other.b
  }

  override open fun hashCode(): Int {
    var hash: Int = 23
    hash = hash * 31 + a
    hash = hash * 31 + b
    return hash
  }

  override open fun toString(): String = a.toString() + ".." + b.toString()
}
