package org.apache.spot.dns.model

import org.apache.log4j.Logger

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spot.dns.model.DNSSuspiciousConnectsModel.{ModelSchema, modelColumns}

import scala.io.Source

/**
  * Routines for ingesting the feedback file provided by the operational analytics layer.
  *
  */
object DNSFeedback {

  /**
    * Load the feedback file for DNS data.
 *
    * @param sc Spark context.
    * @param sqlContext Spark SQL context.
    * @param feedbackFile Local machine path to the DNS feedback file.
    * @param duplicationFactor Number of words to create per flagged feedback entry.
    * @return DataFrame of the feedback events.
    */
  def loadFeedbackDF(sc: SparkContext,
                     sqlContext: SQLContext,
                     feedbackFile: String,
                     duplicationFactor: Int,logger: Logger): DataFrame = {


    if (new java.io.File(feedbackFile).exists) {

      /*
      feedback file is a tab-separated file with a single header line.
      */

      val lines = Source.fromFile(feedbackFile).getLines().toArray.drop(1)
      val feedback: RDD[String] = sc.parallelize(lines)

      /*
      The columns and their entries are as follows:
       0 frame_time
       1 frame_len
       2 ip_dst
       3 dns_qry_name
       4 dns_qry_class
       5 dns_qry_type
       6 dns_qry_rcode
       7 score
       8 tld
       9 query_rep
       10 hh
       11 ip_sev
       12 dns_sev
       13 dns_qry_class_name
       14 dns_qry_type_name
       15 dns_qry_rcode_name
       16 network_context
       17 unix_tstamp
      */
      val FrameTimeIndex = 0
      val UnixTimeStampIndex = 17
      val FrameLenIndex = 1
      val IpDstIndex = 2
      val DnsQryNameIndex = 3
      val DnsQryClassIndex = 4
      val DnsQryTypeIndex = 5
      val DnsQryRcodeIndex = 6
      val DnsSevIndex = 12

      sqlContext.createDataFrame(feedback.map(_.split("\t"))
        .filter(row => row(DnsSevIndex).trim.toInt == 3)
        .map(row => Row.fromSeq(Seq(row(FrameTimeIndex),
          row(UnixTimeStampIndex).toLong,
          row(FrameLenIndex).toInt,
          row(IpDstIndex),
          row(DnsQryNameIndex),
          row(DnsQryClassIndex),
          row(DnsQryTypeIndex).toInt,
          row(DnsQryRcodeIndex).toInt)))
        .flatMap(row => List.fill(duplicationFactor)(row)), ModelSchema)
        .select(modelColumns:_*)
    } else {
    	logger.info("..................................")
      sqlContext.createDataFrame(sc.emptyRDD[Row], ModelSchema)
    }
  }
}