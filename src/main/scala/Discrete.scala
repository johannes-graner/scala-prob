package scalaprob.probability

import spire.algebra.Field
import spire.syntax.field._
import spire.math._
import spire.implicits._
//import scala.math.{abs,pow}

trait DiscreteTrait {

  def density: Map[Int, Rational]

  implicit val num = spire.compat.numeric[Rational]

  def parameter: Any = None

  def checkDensity: Boolean = density.values.sum == Rational.one
  def distribution: Map[Int, Rational] = {
    density.keys.toSeq.sorted
      .scanLeft(0, Rational.zero) {
        case ((m, fm), n) =>
          (n, fm + density(n))
      }
      .tail
      .toMap
  }

  def moment(k: Int): Rational = density.map(p => pow(p._1, k) * p._2).sum
  def mean: Rational = moment(1)
  def variance: Rational = moment(2) - mean*mean

  def getProb(k: Int): Rational = density.getOrElse(k, Rational.zero)

  def percentile(p: Double): Int = {
    if (p < 0 || p > 1)
      throw new IllegalArgumentException(s"$p is not between 0 and 1")
    else
      distribution.filter(_._2 <= p).map(_._1).max
  }

  protected def conv(n: Int, f: DiscreteTrait, g: DiscreteTrait): Rational = {
    val (longDens, shortDens) =
      if (f.density.size > g.density.size)
        (f.density, g.density)
      else
        (g.density, f.density)
    shortDens
      .map(p => longDens.getOrElse(n - p._1, Rational.zero) * p._2)
      .sum
  }

  def convolution(other: DiscreteTrait): DiscreteTrait = {
    this.toDiscreteProb.convolution(other)
  }
  /*= {
        val thisKeys = this.density.keys
        val otherKeys = other.density.keys
        DiscreteProb(
            ( thisKeys.min + otherKeys.min to thisKeys.max + otherKeys.max )
                .map( n => (n, conv(n, this, other)) ).toMap
        )
    }*/

  override def toString: String = density.toSeq.sortBy(_._1).toString

  def toDiscreteProb: DiscreteProb = DiscreteProb(density)
}

case class DiscreteProb(override val density: Map[Int, Rational])
    extends DiscreteTrait {

  override def convolution(other: DiscreteTrait): DiscreteProb = {
    val thisKeys = this.density.keys
    val otherKeys = other.density.keys
    DiscreteProb(
      (thisKeys.min + otherKeys.min to thisKeys.max + otherKeys.max)
        .map(n => (n, conv(n, this, other)))
        .toMap
    )
  }
}

case class DeMoivre(k: Int) extends DiscreteTrait {
  override val parameter = k
  override val density: Map[Int, Rational] =
    ((1 to k).toSeq zip Seq.fill(k)(Rational.one / k)).toMap
}

case class Bernoulli(p: Double) extends DiscreteTrait {
  override val parameter = p
  override val density = Map((0, 1 - p), (1, p))

  override def convolution(other: DiscreteTrait): DiscreteTrait = {
    if (other.isInstanceOf[Bernoulli] && other.parameter == parameter)
      Binomial(p, 2)
    else
      this.toDiscreteProb.convolution(other)
  }
}

case class Binomial(p: Double, n: Int) extends DiscreteTrait {
  override val parameter: Any = (p, n)
  override val density = {
    val binoms = (2 to n).scanLeft(Rational.one)((k, cnk) => (n + 1 - k) / k * cnk).toSeq
    ((1 to n).toSeq zip binoms).map {
      case (k: Int, cnk: Rational) => (k, cnk * pow(p, k) * pow(1 - p, n - k))
    }.toMap
  }

  override def convolution(other: DiscreteTrait): DiscreteTrait = {
    if (other.isInstanceOf[Bernoulli] && other.parameter == p)
      Binomial(p, n + 1)
    else if (other.isInstanceOf[Binomial] && other.parameter == parameter)
      Binomial(p, other.parameter.asInstanceOf[(Double, Int)]._2 + n)
    else
      this.toDiscreteProb.convolution(other)
  }
}