package org.geoscript
package geometry

import org.geoscript.io.{ Sink, Source }
import org.specs._

class SerializationSpec extends Specification {
  "JSON Serialization" should {
    "round-trip points" in {
      val p = Point(100, 0)
      val json = io.GeoJSON.write(p, Sink.string)
      json must_== """{"type":"Point","coordinates":[100,0.0]}"""
      // io.GeoJSON.read(Source.string(json)) must_== p
      // TODO: Implement equality for geometries
    }

    "round-trip linestrings" in {
      val ls = LineString((100, 0), (101, 1))
      io.GeoJSON.write(ls, Sink.string) must_==
        """{"type":"LineString","coordinates":[[100,0.0],[101,1]]}"""
    }

    "round-trip polygons" in {
      val solid = Polygon(
        LineString((100, 0), (101, 0), (101, 1), (100, 1), (100, 0))
      )

      val withHoles = Polygon(
        LineString((100, 0), (101, 0), (101, 1), (100, 1), (100, 0)),
        Seq(LineString(
          (100.2, 0.2), (100.8, 0.2), (100.8, 0.8), (100.2, 0.8), (100.2, 0.2)
        ))
      )

      io.GeoJSON.write(solid, Sink.string) must_==
        """{"type":"Polygon","coordinates":[[[100,0.0],[101,0.0],[101,1],[100,1],[100,0.0]]]}"""
      io.GeoJSON.write(withHoles, Sink.string) must_==
        """{"type":"Polygon","coordinates":[[[100,0.0],[101,0.0],[101,1],[100,1],[100,0.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}"""
    }

    "round-trip a multipoint" in {
      val mp = MultiPoint((100.0, 0.0), (101.0, 1.0))
      io.GeoJSON.write(mp, Sink.string) must_==
        """{"type":"MultiPoint","coordinates":[[100,0.0],[101,1]]}"""
    }

    "round-trip a MultiLineString" in {
      val mls = MultiLineString(
        LineString((100, 0), Point(101, 1)),
        LineString((102, 2), Point(103, 3))
      )

      io.GeoJSON.write(mls, Sink.string) must_== 
        """{"type":"MultiLineString","coordinates":[[[100,0.0],[101,1]],[[102,2],[103,3]]]}"""
    }

    "round-trip a MultiPolygon" in {
      val mp = MultiPolygon(
        Polygon(LineString(
          (102, 2), (103, 2), (103, 3), (102, 3), (102, 2)
        )),
        Polygon(LineString(
            (100, 0), (101, 0), (101, 1), (100, 1), (100, 0)
          ),
          Seq(LineString(
            (100.2, 0.2), (100.8, 0.2), (100.8, 0.8), (100.2, 0.8), (100.2, 0.2)
          ))
        )
      )

      io.GeoJSON.write(mp, Sink.string) must_== 
        """{"type":"MultiPolygon","coordinates":[[[[102,2],[103,2],[103,3],[102,3],[102,2]]],[[[100,0.0],[101,0.0],[101,1],[100,1],[100,0.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]]}"""
    }

    "round-trip a GeometryCollection" in {
      val gc = GeometryCollection(
        Point(100.0, 0.0),
        LineString((101.0, 0.0), (102.0, 1.0))
      )

      io.GeoJSON.write(gc, Sink.string) must_==
        """{"type":"GeometryCollection","geometries":[{"type":"Point","coordinates":[100,0.0]},{"type":"LineString","coordinates":[[101,0.0],[102,1]]}]}"""
    }
  }
}
