package org.geoscript.example

import org.geoscript._

object Intersections {
  def process(src: layer.Layer, dest: layer.Layer, joinField: String) {
    println("Processing %s".format(src.schema.name))

    for (feat <- src.features) {
      val intersections = 
        src.filter(filter.Filter.intersects(feat.geometry))
      dest ++= 
        intersections.filter(_.id > feat.id).map { corner =>
          feature.Feature(
            "geom" -> (feat.geometry intersection corner.geometry),
            (joinField + "Left") -> feat.get(joinField),
            (joinField + "Right") -> corner.get(joinField)
          )
        }
    }

    println("Found %d intersections".format(dest.count))
  }

  def rewrite(schema: feature.Schema, fieldName: String): feature.Schema = 
    feature.Schema(
      schema.name + "_intersections",
      feature.Field(
        "geom",
        classOf[com.vividsolutions.jts.geom.Geometry],
        schema.geometry.projection
      ),
      feature.Field(fieldName + "Left", classOf[String]),
      feature.Field(fieldName + "Right", classOf[String])
    )

  def main(args: Array[String]) = {
    if (args.length == 0) {
      println("You need to provide the path to a shapefile as an argument to this example.")
    } else {
      val src = layer.Shapefile(args(0))
      val joinField = 
        src.schema.fields.find { _.gtBinding == classOf[String] } match {
          case Some(f) => f.name
          case None => "id"
        }
      val dest = src.workspace.create(rewrite(src.schema, joinField))
      process(src, dest, joinField)
    }
  }
}
