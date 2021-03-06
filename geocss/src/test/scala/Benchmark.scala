package org.geoscript.geocss

import org.geotools.{ styling => gt }
import gt.Style
import collection.JavaConversions._

object Benchmark {
  val Translator = new Translator()
  val template = """
[%1$s < 10] {
  stroke: black;
  label: "%1$s - 1";
}

[%1$s > 20] {
  stroke: red;
  label: "%1$s - 2";
}

[%1$s < 20] {
  stroke: blue;
  label: "%1$s - 3";
}

[%1$s = 15] {
  stroke: green;
  label: "%1$s - 4";
}
"""

  val tx = new org.geotools.styling.SLDTransformer()
  tx.setIndentation(4)

  def encodeSLD(sld: gt.Style): String = {
    val out = new java.io.StringWriter()
    tx.transform(sld, out)
    out.toString()
  }

  def time[A](op: => A): (A, Long) = {
    val startTime = System.currentTimeMillis()
    (op, System.currentTimeMillis() - startTime)
  }

  def ruleCount(sld: Style): Int = {
    sld.featureTypeStyles.foldLeft(0) { (i, fts) => i + fts.rules.length }
  }

  def main(args: Array[String]) {
    for (end <- 'A' to 'B') {
      // dry run; warm up the JIT statistics
      encodeSLD(
        Translator.css2sld(
          CssParser.parse(
            'A' to end map { template.format(_) } mkString
          ).get
        )
      )
    }

    println("properties, parse_time, transform_time, rule_count, encode_time") 

    for (end <- 'A' to 'D') {
      val range = 1 + (end - 'A')
      val css = 'A' to end map { template.format(_) } mkString
      val (cssRules, parseTime) = time { CssParser.parse(css).get }
      val (sldRules: Style, transformTime) = time { Translator.css2sld(cssRules) }
      val (sld, encodeTime) = time { encodeSLD(sldRules) }
      println(Seq(range, parseTime, transformTime, ruleCount(sldRules), encodeTime).mkString(", "))
    }

    println("values, parse_time, transform_time, rule_count, encode_time") 
    for (range <- 4 to 16 by 4) {
      val css = (1 to range) map {
        """
        [A=%1$s] { label: "%1$s"; }
        """.format(_)
      } mkString
      val (cssRules, parseTime) = time { CssParser.parse(css).get }
      val (sldRules, transformTime) = time { Translator.css2sld(cssRules) }
      val (sld, encodeTime) = time { encodeSLD(sldRules) }
      println(Seq(range, parseTime, transformTime, ruleCount(sldRules), encodeTime).mkString(", "))
    }
  }
}
