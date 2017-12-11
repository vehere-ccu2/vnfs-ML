package org.apache.spot.utilities

import scala.math.log10

object MathUtils {
  /**
    * Answers the question "To what power must "base" be raised, in order to yield "x"?  https://en.wikipedia.org/wiki/Logarithm
    *
    * @param x    This is a Double which is the result of the formula: base to the power of y = x
    * @param base This is the base of the number we are trying to find
    * @return y rounded down to an integer
    */
  def logBaseXInt(x: Double, base: Int): Int = if (x == 0) 0 else (log10(x) / log10(base)).toInt

  /**
    * Returns the ceiling of the logarithm base 2 of the incoming double.
    *
    * @param x A double.
    * @return Integer ceiling of logarithm base-2 of x.
    */
  def ceilLog2(x: Double) : Int = Math.ceil(log10(x) / log10(2d)).toInt
}