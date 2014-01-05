package com.github.robertberry.fpis

object Chapter10 {

  trait Monoid[A] {
    def op(a1: A, a2: A): A

    def zero: A
  }

  val stringMonoid = new Monoid[String] {
    def op(a1: String, a2: String): String = a1 + a2

    def zero: String = ""
  }

  def listMonoid[A] = new Monoid[List[A]] {
    def op(a1: List[A], a2: List[A]): List[A] = a1 ++ a2

    def zero: List[A] = Nil
  }

  /** Exercise 1
    *
    * Give Monoid instances for integer addition and multiplication as well as Boolean operators
    */
  val intAddition = new Monoid[Int] {
    def op(a1: Int, a2: Int): Int = a1 + a2

    def zero: Int = 0
  }

  val intMultiplication = new Monoid[Int] {
    def op(a1: Int, a2: Int): Int = a1 * a2

    def zero: Int = 1
  }

  val booleanOr = new Monoid[Boolean] {
    def op(a1: Boolean, a2: Boolean): Boolean = a1 || a2

    def zero: Boolean = false
  }

  val booleanAnd = new Monoid[Boolean] {
    def op(a1: Boolean, a2: Boolean): Boolean = a1 && a2

    def zero: Boolean = true
  }

  /** Exercise 2
    *
    * Give a Monoid instance for combining two Options
    */
  def firstOption[A] = new Monoid[Option[A]] {
    def op(a1: Option[A], a2: Option[A]): Option[A] = a1 orElse a2

    def zero: Option[A] = None
  }

  def endoMonoid[A] = new Monoid[A => A] {
    def op(a1: (A) => A, a2: (A) => A): (A) => A = a1 compose a2

    def zero: (A) => A = identity[A]
  }

  /** Exercise 5
    *
    * Write a Monoid instance for STring that inserts spaces between words unless they exist, and trims spaces off the
    * ends of the result
    */
  val trimMonoid = new Monoid[String] {
    def op(a1: String, a2: String): String = (a1.trim, a2.trim) match {
      case ("", x) => x
      case (x, "") => x
      case (x, y) => x + " " + y
    }

    def zero: String = ""
  }

  /** Exercise 6
    *
    * Implement concatenate, a function that folds a list with a Monoid
    */
  def concatenate[A](as: List[A])(implicit monoid: Monoid[A]): A = as.foldLeft(monoid.zero)(monoid.op)

  /** Exercise 7
    *
    * Implement foldMap
    */
  def foldMap[A,B](as: List[A], m: Monoid[B])(f: A => B): B = {
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))
  }

  /** Exercise 8
    *
    * Implement foldLeft and foldRight in terms of foldMap
    */
  def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B) = {
    foldMap(as, endoMonoid[B])({ a: A => (b: B) => f(b, a) })(z)
  }

  def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B = {
    foldMap(as, endoMonoid[() => B])({ a: A => (b: () => B) => () => f(a, b()) })(() => z)()
  }

  /** Exercise 9
    *
    * Implement a foldMap for IndexedSeq - it should do a balanced fold
    */
  def foldMapV[A, B](as: IndexedSeq[A], m: Monoid[B])(f: A => B): B = {
    if (as.length == 0) m.zero
    else if (as.length == 1) m.op(m.zero, f(as(0)))
    else {
      val (left, right) = as.splitAt(as.length / 2)
      m.op(foldMapV(left, m)(f), foldMapV(right, m)(f))
    }
  }

  /** Exercise 10
    *
    * Use foldMapV to detect whether a given IndexedSeq[Int] is ordered.
    *
    * You will need to come up with a creative Monoid.
    */

  /** This is inelegant, but I couldn't think of a better solution at the time. :-( */
  sealed trait Order
  case object Ascending extends Order
  case object Descending extends Order
  case object Level extends Order
  case object Variadic extends Order

  sealed trait Range
  case object ZeroRange extends Range
  case class IntRange(low: Int, high: Int, order: Order) extends Range

  val rangeMonoid = new Monoid[Range] {
    def op(a1: Range, a2: Range): Range = (a1, a2) match {
      case (ZeroRange, x) => x
      case (x, ZeroRange) => x
      case (IntRange(xLow, xHigh, xOrder), IntRange(yLow, yHigh, yOrder)) => IntRange(
        xLow min yLow,
        xHigh max yHigh,
        (xOrder, yOrder) match {
          case (Level, Level) if xHigh == yLow => Level
          case (Ascending | Level, Ascending | Level) if xHigh <= yLow => Ascending
          case (Descending | Level, Descending | Level) if xLow >= yHigh => Descending
          case otherwise => Variadic
        }
      )
    }

    def zero: Range = ZeroRange
  }

  def isOrdered(xs: IndexedSeq[Int]) = foldMapV(xs, rangeMonoid)(x => IntRange(x, x, Level)) match {
    case IntRange(_, _, Variadic) => false
    case _ => true
  }
}