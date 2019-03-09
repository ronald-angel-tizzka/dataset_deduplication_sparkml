package com.trovit.processor.cars

import java.sql.Date

import com.trovit.general.SparkSpec
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, GivenWhenThen, Matchers}

class CarMetricsTest extends FlatSpec with SparkSpec with GivenWhenThen with Matchers with BeforeAndAfterEach {


  "Starting with a simple join to understand data" should "Recognize Duplicated Data " in {

    val windowCarsKeyHash = Window.partitionBy(col("hashCategory"))
      .orderBy(col("hashCategory"))

    val windowCarsKeyHashMake = Window.partitionBy(col("hashCategory"),
      col("make")
    )

    val windowCarsKeyHashModel = Window.partitionBy(col("hashCategory"),
      col("year")
    )
      .orderBy(col("hashCategory"), col("make"))

    val carsDF = ss.createDataFrame(Seq(
      ("hash1", "make1", 50.0, 2002, "red", 10000, "1", Date.valueOf("2018-07-29")),
      ("hash1", "make1", 50.5, 2003, "red", 11000, "2", Date.valueOf("2018-07-28")),
      ("hash1", "make2", 50.6, 2004, "white", 12000, "3", Date.valueOf("2017-07-29")),
      ("hash2", "make1", 50.0, 2002, "red", 10000, "4", Date.valueOf("2017-07-29")),
      ("hash2", "make2", 50.0, 2002, "red", 11000, "5", Date.valueOf("2016-07-29")),
      ("hash2", "make3", 50.4, 2002, "red", 13000, "6", Date.valueOf("2018-07-29")),
      ("hash3", "make4", 50.0, 2005, "red", 9000, "7", Date.valueOf("2018-07-29")),
      ("hash3", "make4", 50.0, 2006, "blue", 10000, "8", Date.valueOf("2018-07-29")),
      ("hash3", "make4", 50.0, 2007, "yellow", 10000, "9", Date.valueOf("2018-07-29"))
    )).toDF("hashCategory", "make", "price", "year", "color", "mileage", "uniqueID", "date")

    carsDF.show(false)

    val carsMetrics =  carsDF
      .withColumn("isRecentPost", when(datediff(current_timestamp(), col("date")) > 10, 0).otherwise(1))
      .withColumn("avgPriceCategory",  avg("price").over(windowCarsKeyHash))
      .withColumn("DiffAvgPrice", col("price") - col("avgPriceCategory"))
      .withColumn("makeCount", count("uniqueID").over(windowCarsKeyHashMake))
      .withColumn("rankMake", dense_rank().over(windowCarsKeyHash.orderBy(desc("makeCount"), desc("year"))))
      .withColumn("AvgkmModel", avg(col("mileage")).over(windowCarsKeyHashModel.orderBy(desc("rankMake"))))
      .withColumn("DiffAvgKms", col("mileage") - col("AvgkmModel"))
      .withColumn("NumberRecentPost", sum("isRecentPost").over(windowCarsKeyHash))
      .withColumn("newestRank", row_number().over(windowCarsKeyHash.orderBy("mileage")))
      .withColumn("isTopNew", when(col("newestRank") === 1, 1).otherwise(0))
      .withColumn("otherColors", collect_set("color").over(windowCarsKeyHash))

    carsMetrics.show(false)

    assert(carsMetrics.count() > 0)


    //TODO: add https://github.com/pygmalios/reactiveinflux-spark

    //carsDF.saveToInflux()



  }





}
