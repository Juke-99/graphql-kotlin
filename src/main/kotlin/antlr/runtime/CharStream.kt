package antlr.runtime

import antlr.runtime.misc.Interval

open interface CharStream : IntStream {
  open fun getText(interval: Interval): String
}
