package org.geoscript.geocss

import collection.JavaConversions._

import filter.FilterOps.filters

import org.opengis.filter.Filter

/**
 * A Selector expresses some subset of all possible features.  It restricts the
 * conditions under which a Rule will be applied to a feature, including but
 * not limited to requirements regarding the feature's attributes.
 */
sealed abstract class Selector {
  /**
   * An Option containing the OGC Filter equivalent to this Selector, if it
   * exists.
   */
  def filterOpt: Option[Filter]
}

/**
 * A Selector which filters on typical database attributes.
 */
abstract class DataSelector extends Selector {
  protected val filters = 
    org.geotools.factory.CommonFactoryFinder.getFilterFactory2(null)

  /**
   * The OGC Filter corresponding to this Selector
   */
  def asFilter: Filter
  override def filterOpt = Some(asFilter)
}

/**
 * A Selector which filters on something other than typical database
 * attributes.
 */
abstract class MetaSelector extends Selector {
  def filterOpt = None
}

/**
 * A Selector which only applies to pseudo-elements
 */
trait Context extends MetaSelector

/**
 * A Selector which only accepts the feature with a specific FID.  This
 * corresponds to the #id123 syntax in CSS, for example.
 */
case class Id(id: String) extends DataSelector {
  /**
   * The (singleton) set of OGC Identifier objects for this selector.
   */
  val idSet: java.util.Set[org.opengis.filter.identity.Identifier] = {
    val set = new java.util.HashSet[org.opengis.filter.identity.Identifier]
    set.add(filters.featureId(id))
    java.util.Collections.unmodifiableSet[org.opengis.filter.identity.Identifier](set)
  }

  override def asFilter = filters.id(idSet)

  override def toString = "#" + id
}

/**
 * A Selector that never rejects anything, corresponding to the '*' syntax in
 * CSS.
 */
case object Accept extends DataSelector {
  override def asFilter = org.opengis.filter.Filter.INCLUDE
  override def toString = "*"
}

case object Exclude extends DataSelector {
  override def asFilter = org.opengis.filter.Filter.EXCLUDE
  override def toString = "[!!]"
}

/**
 * A Selector which constrains based on typename.
 */
case class TypenameSelector(typename: String) extends MetaSelector {
  override def toString = typename
}

/**
 * A Selector which uses CQL-like syntax to express constraints on a contextual
 * property such as the scale denominator at render time.  This corresponds to
 * the [&64;scale &gt; 10000] syntax in CSS, for example.
 */
case class PseudoSelector(property: String, operator: String, value: String)
extends MetaSelector {
  override def toString = "@%s%s%s".format(property, operator, value)
}

/**
 * A Selector which only matches pseudo-features such as geometries generated
 * by well-known-marks.  This correponds to the :mark syntax in CSS, for
 * example.
 */
case class PseudoClass(name: String) extends Context {
  override def toString = ":%s".format(name)
}


/**
 * A Selector which only matches pseudo-features such as geometries generated by
 * well-known-marks, with additional parameters besides just the name.  This
 * corresponds to the :nth-mark(n) syntax in CSS, for example.
 */
case class ParameterizedPseudoClass(name: String, param: String) 
extends Context {
  override def toString = ":%s(%s)".format(name, param)
}

/**
 * A Selector which wraps a CQL expression.
 */
case class ExpressionSelector(expression: String) extends DataSelector {
  override lazy val asFilter = 
    org.geotools.filter.text.ecql.ECQL.toFilter(expression)
  override def toString = expression
}

/**
 * A Selector which wraps an OGC Filter instance.
 */
case class WrappedFilter(filter: org.opengis.filter.Filter) 
extends DataSelector {
  override def asFilter = filter
  override def toString = filter.toString
}

/**
 * A selector which wraps another and reverses its decisions (it accepts only
 * features that would not be accepted by the wrapped selector).
 */
case class NotSelector(selector: Selector) extends Selector {
  override def filterOpt =
    selector match {
      case NotSelector(sel) => sel.filterOpt
      case sel => 
        selector.filterOpt map {
          case org.opengis.filter.Filter.EXCLUDE =>
            org.opengis.filter.Filter.INCLUDE
          case org.opengis.filter.Filter.INCLUDE =>
            org.opengis.filter.Filter.EXCLUDE
          case f =>
            filters.not(f)
        }
  }
}

/**
 * An aggregate selector which accepts features accepted by all of its members.
 */
case class AndSelector(children: Seq[Selector]) extends Selector {
  override def filterOpt =
    if (children.forall(_.filterOpt.isDefined)) {
      val operands = children map { _.filterOpt.get }
      Some(
        if (operands contains Filter.EXCLUDE) {
          Filter.EXCLUDE
        } else {
          operands.filter(Filter.INCLUDE !=) match {
            case Seq() => Filter.INCLUDE
            case Seq(f) => f
            case fs => filters.and(fs)
          }
        }
      )
    } else {
      None
    }
}

/**
 * An aggregate selector which accepts features accepted by any of its members.
 */
case class OrSelector(children: Seq[Selector]) extends Selector {
  override def filterOpt =
    if (children.forall(_.filterOpt.isDefined)) {
      val operands = children map { _.filterOpt.get }
      Some(
        if (operands.exists {_ == Filter.INCLUDE}) {
          Filter.INCLUDE
        } else {
          val parts = operands.partition(Filter.EXCLUDE==)
          parts._2 match {
            case Seq() if (parts._1.isEmpty) => Filter.INCLUDE
            case Seq() => Filter.EXCLUDE
            case Seq(f) => f
            case fs => filters.or(fs)
          }
        }
      )
    } else {
      None
    }
}
