package scalaz

import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalazArbitrary._
import std.AllInstances._
import syntax.contravariant._
import org.scalacheck.Prop.forAll

object EphemeralStreamTest extends SpecLite {

  checkAll(equal.laws[EphemeralStream[Int]])
  checkAll(monadPlus.laws[EphemeralStream])
  checkAll(traverse.laws[EphemeralStream])
  checkAll(zip.laws[EphemeralStream])
  checkAll(align.laws[EphemeralStream])
  checkAll(cobind.laws[EphemeralStream])

  implicit def ephemeralStreamShow[A: Show]: Show[EphemeralStream[A]] =
    Show[List[A]].contramap(_.toList)

  "reverse" ! forAll{ e: EphemeralStream[Int] =>
    e.reverse.toList must_===(e.toList.reverse)
    e.reverse.reverse must_===(e)
  }

  "foldLeft large stream" in {
    val list = List.fill(10000000)(1)
    val xs = EphemeralStream(list : _*)
    Foldable[EphemeralStream].foldLeft(xs, 0)(_ + _) must_===(list.sum)
  }

  "foldLeft" ! forAll{ xs: List[List[Int]] =>
    Foldable[EphemeralStream].foldLeft(EphemeralStream(xs: _*), List[Int]())(_ ::: _) must_===(xs.foldLeft(List[Int]())(_ ::: _))
  }

  "unzip zip" ! forAll { xs: EphemeralStream[(Int, Int)] =>
    val (firsts, seconds) = xs.unzip
    (firsts zip seconds) must_===(xs)
  }

  "zip has right length" ! forAll {(xs: EphemeralStream[Int], ys: EphemeralStream[Int]) =>
    (xs zip ys).length must_===(xs.length min ys.length)
  }

  "interleave has right length" ! forAll {(xs: EphemeralStream[Int], ys: EphemeralStream[Int]) =>
    (xs interleave ys).length must_===(xs.length + ys.length)
  }

  "take" ! forAll { (xs: Stream[Int], n: Int) =>
    EphemeralStream.fromStream(xs).take(n) must_===(EphemeralStream.fromStream(xs.take(n)))
  }

  "take from infinite stream" in {
    val n = util.Random.nextInt(1000)
    EphemeralStream.iterate(0)(_ + 1).take(n) must_===(EphemeralStream.fromStream(Stream.iterate(1)(_ + 1).take(n)))
  }

  "takeWhile" ! forAll { (xs: Stream[Int], n: Int) =>
    EphemeralStream.fromStream(xs).takeWhile(_ < n) must_===(EphemeralStream.fromStream(xs.takeWhile(_ < n)))
  }

  "takeWhile from infinite stream" in {
    val n = util.Random.nextInt(1000)
    EphemeralStream.iterate(0)(_ + 1).takeWhile(_ < n) must_===(EphemeralStream.fromStream(Stream.iterate(1)(_ + 1).takeWhile(_ < n)))
  }

  "index" ! forAll {(xs: EphemeralStream[Int], i: Int) =>
    Foldable[EphemeralStream].index(xs, i) must_===(xs.toList.lift.apply(i))
  }

  "index infinite stream" in {
    val i = util.Random.nextInt(1000)
    val xs = Stream from 0
    Foldable[EphemeralStream].index(EphemeralStream.fromStream(xs), i) must_===(xs.lift.apply(i))
  }

  "inits" ! forAll { xs: EphemeralStream[Int] =>
    import syntax.std.list._
    xs.inits.map(_.toList).toList must_===(xs.toList.initz)
  }

  "tails" ! forAll { xs: EphemeralStream[Int] =>
    import syntax.std.list._
    xs.tails.map(_.toList).toList must_===(xs.toList.tailz)
  }

  "inits infinite stream" in {
    EphemeralStream.iterate(0)(_ + 1).inits
    ()
  }

  "tails infinite stream" in {
    val n = util.Random.nextInt(1000)
    EphemeralStream.iterate(0)(_ + 1).tails
      .map(t => Foldable[EphemeralStream].toStream(t.take(n)))
      .take(n) must_===(
      EphemeralStream.fromStream(Stream.iterate(1)(_ + 1).tails.map(_ take n).toStream.take(n))
    )
  }

  "no stack overflow infinite stream foldMap" in {
    val infiniteStream = EphemeralStream.iterate(false)(identity)
    Foldable[EphemeralStream].foldMap(infiniteStream)(identity)(booleanInstance.conjunction) must_===(false)
  }

  "no stack overflow infinite stream foldRight" in {
    val infiniteStream = EphemeralStream.iterate(true)(identity)
    Foldable[EphemeralStream].foldRight(infiniteStream, true)(_ || _) must_===(true)
  }

  "zipL" in {
    val size = 100
    val infinite = EphemeralStream.iterate(0)(_ + 1)
    val finite = EphemeralStream.range(0, size)
    val F = Traverse[EphemeralStream]
    F.zipL(infinite, infinite)
    F.zipL(finite, infinite).length must_===(size)
    F.zipL(finite, infinite) must_===((finite zip infinite).map{x => (x._1, Option(x._2))})
    F.zipL(infinite, finite).take(1000).length must_===(1000)
    F.zipL(infinite, finite).takeWhile(_._2.isDefined).length must_===(size)
  }
}
