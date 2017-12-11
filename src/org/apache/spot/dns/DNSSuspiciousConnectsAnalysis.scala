package org.apache.spot.dns

import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spot.SuspiciousConnectsArgumentParser.SuspiciousConnectsConfig
import org.apache.spot.dns.DNSSchema._
import org.apache.spot.dns.model.DNSSuspiciousConnectsModel
import org.apache.spot.dns.model.DNSSuspiciousConnectsModel.ModelSchema
//import org.apache.spot.proxy.ProxySchema.Score
import org.apache.spot.utilities.data.validation.{InvalidDataHandler => dataValidation}
import org.elasticsearch.spark._
import org.elasticsearch.spark.sql._
import org.apache.hadoop.fs.{LocatedFileStatus, Path, RemoteIterator, FileUtil => fileUtil}
import java.util.Calendar
import java.text.SimpleDateFormat
/**
  * The suspicious connections analysis of DNS log data develops a probabilistic model the DNS queries
  * made by each client IP and flags those assigned a low probability as "suspicious"
  */

object DNSSuspiciousConnectsAnalysis {



  /**
    * Run suspicious connections analysis on DNS log data.
    * Saves the most suspicious connections to a CSV file on HDFS.
    *
    * @param config Object encapsulating runtime parameters and CLI options.
    * @param sparkContext
    * @param sqlContext
    * @param logger
    */
  def run(config: SuspiciousConnectsConfig, sparkContext: SparkContext, sqlContext: SQLContext, logger: Logger,
          inputDNSRecords: DataFrame) = {


    logger.info("Starting DNS suspicious connects analysis.")

    val cleanDNSRecords = filterAndSelectCleanDNSRecords(inputDNSRecords)

    val scoredDNSRecords = scoreDNSRecords(cleanDNSRecords, config, sparkContext, sqlContext, logger)

    //val filteredDNSRecords = filterScoredDNSRecords(scoredDNSRecords, config.threshold)

    val orderedDNSRecords = scoredDNSRecords.orderBy(Score)

    val mostSuspiciousDNSRecords = if(config.maxResults > 0)  orderedDNSRecords.limit(config.maxResults) else orderedDNSRecords

    val outputDNSRecords = mostSuspiciousDNSRecords.select(OutSchema:_*).sort(Score)

    logger.info("DNS  suspicious connects analysis completed.")
    logger.info("Saving results to : " + config.hdfsScoredConnect)

    import sqlContext.implicits._
    //outputDNSRecords.map(_.mkString(config.outputDelimiter)).write.format("csv").save(config.hdfsScoredConnect)
    outputDNSRecords.write.mode("overwrite").csv(config.hdfsScoredConnect)
        
    val invalidDNSRecords = filterAndSelectInvalidDNSRecords(inputDNSRecords)
    dataValidation.showAndSaveInvalidRecords(invalidDNSRecords, config.hdfsScoredConnect, logger)

    val corruptDNSRecords = filterAndSelectCorruptDNSRecords(scoredDNSRecords)
    dataValidation.showAndSaveCorruptRecords(corruptDNSRecords, config.hdfsScoredConnect, logger)
    
    val es_index  = mostSuspiciousDNSRecords.select(mostSuspiciousDNSRecords("index")).distinct().collect()(0)(0)
    
       
    //IOf exception is thrown then update is replace with upsert
    val esConfig = Map(("es.nodes",config.esnodes),("es.port",config.esport),("es.write.operation", "update"),("es.mapping.id", "id"), ("es.mapping.exclude","id")) //("es.index.auto.create","true")) //,
    val csvSchema = StructType(
    List(TimestampField,
      UnixTimestampField,
      FrameLengthField,
      ClientIPField,
      QueryNameField,
      QueryClassField,
      QueryTypeField,
      QueryResponseCodeField,
      ScoreField))
      
      val customSchema = StructType(Array(
         
    StructField("id", StringType, true),
   // StructField("dns_qry_name", StringType, true),
    //StructField("ip_dst", StringType, true),
    StructField("dns_score", DoubleType, true)))
      
     val hadoopConfiguration = sparkContext.hadoopConfiguration
     val fileSystem = org.apache.hadoop.fs.FileSystem.get(hadoopConfiguration) 
     val srcDir = new Path(config.hdfsScoredConnect)
     val files: RemoteIterator[LocatedFileStatus] = fileSystem.listFiles(srcDir, false)
     //val dstFile = new Path(config.hdfsScoredConnect + "/" + "_results.csv")
     //fileUtil.copyMerge(fileSystem, srcDir, fileSystem, dstFile, false, hadoopConfiguration, "")

      while (files.hasNext) {
        val filePath = files.next.getPath
        if (filePath.toString.contains(".csv")) {
          logger.info("FileName:" +filePath.toString)
           val es1=	sqlContext.read.format("com.databricks.spark.csv").option("header", "false").option("inferSchema", "false").schema(customSchema).load(filePath.toString)
           es1.saveToEs(es_index+"/metadata", esConfig)
           //fileSystem.delete(filePath, false)
    
        }
      }
    
   
  }


  /**
    * Identify anomalous DNS log entries in in the provided data frame.
    *
    * @param data Data frame of DNS entries
    * @param config
    * @param sparkContext
    * @param sqlContext
    * @param logger
    * @return
    */

  def scoreDNSRecords(data: DataFrame, config: SuspiciousConnectsConfig,
                      sparkContext: SparkContext,
                      sqlContext: SQLContext,
                      logger: Logger) : DataFrame = {

    logger.info("Fitting probabilistic model to data")
    val model =
      DNSSuspiciousConnectsModel.trainNewModel(sparkContext, sqlContext, logger, config, data, config.topicCount)

    logger.info("Identifying outliers")
    model.score(sparkContext, sqlContext, data, config.userDomain)
  }


  /**
    *
    * @param inputDNSRecords raw DNS records.
    * @return
    */
  def filterAndSelectCleanDNSRecords(inputDNSRecords: DataFrame): DataFrame ={

    val cleanDNSRecordsFilter = inputDNSRecords(Timestamp).isNotNull &&
      inputDNSRecords(Timestamp).notEqual("") &&
      inputDNSRecords(Timestamp).notEqual("-") &&
      inputDNSRecords(UnixTimestamp).isNotNull &&
      inputDNSRecords(FrameLength).isNotNull &&
      inputDNSRecords(QueryName).isNotNull &&
      inputDNSRecords(QueryName).notEqual("") &&
      inputDNSRecords(QueryName).notEqual("-") &&
      inputDNSRecords(QueryName).notEqual("(empty)") &&
      inputDNSRecords(ClientIP).isNotNull &&
      inputDNSRecords(ClientIP).notEqual("") &&
      inputDNSRecords(ClientIP).notEqual("-") &&
      ((inputDNSRecords(QueryClass).isNotNull &&
        inputDNSRecords(QueryClass).notEqual("") &&
        inputDNSRecords(QueryClass).notEqual("-")) ||
        inputDNSRecords(QueryType).isNotNull ||
        inputDNSRecords(QueryResponseCode).isNotNull)

    inputDNSRecords
      .filter(cleanDNSRecordsFilter)
      .select(InSchema: _*)
      .na.fill(DefaultQueryClass, Seq(QueryClass))
      .na.fill(DefaultQueryType, Seq(QueryType))
      .na.fill(DefaultQueryResponseCode, Seq(QueryResponseCode))
  }


  /**
    *
    * @param inputDNSRecords raw DNS records.
    * @return
    */
  def filterAndSelectInvalidDNSRecords(inputDNSRecords: DataFrame): DataFrame ={

    val invalidDNSRecordsFilter = inputDNSRecords(Timestamp).isNull ||
      inputDNSRecords(Timestamp).equalTo("") ||
      inputDNSRecords(Timestamp).equalTo("-") ||
      inputDNSRecords(UnixTimestamp).isNull ||
      inputDNSRecords(FrameLength).isNull ||
      inputDNSRecords(QueryName).isNull ||
      inputDNSRecords(QueryName).equalTo("") ||
      inputDNSRecords(QueryName).equalTo("-") ||
      inputDNSRecords(QueryName).equalTo("(empty)") ||
      inputDNSRecords(ClientIP).isNull ||
      inputDNSRecords(ClientIP).equalTo("") ||
      inputDNSRecords(ClientIP).equalTo("-") ||
      ((inputDNSRecords(QueryClass).isNull ||
        inputDNSRecords(QueryClass).equalTo("") ||
        inputDNSRecords(QueryClass).equalTo("-")) &&
        inputDNSRecords(QueryType).isNull &&
        inputDNSRecords(QueryResponseCode).isNull)

    inputDNSRecords
      .filter(invalidDNSRecordsFilter)
      .select(InSchema: _*)
  }


  /**
    *
    * @param scoredDNSRecords scored DNS records.
    * @param threshold score tolerance.
    * @return
    */
  def filterScoredDNSRecords(scoredDNSRecords: DataFrame, threshold: Double): DataFrame ={


    val filteredDNSRecordsFilter = scoredDNSRecords(Score).leq(threshold) &&
      scoredDNSRecords(Score).gt(dataValidation.ScoreError)

    scoredDNSRecords.filter(filteredDNSRecordsFilter)
  }

  /**
    *
    * @param scoredDNSRecords scored DNS records.
    * @return
    */
  def filterAndSelectCorruptDNSRecords(scoredDNSRecords: DataFrame): DataFrame = {

    val corruptDNSRecordsFilter = scoredDNSRecords(Score).equalTo(dataValidation.ScoreError)

    scoredDNSRecords
      .filter(corruptDNSRecordsFilter)
      .select(OutSchema: _*)

  }


  val DefaultQueryClass = "unknown"
  val DefaultQueryType = "-1"
  val DefaultQueryResponseCode = "-1"

  val InStructType = StructType(List(IdField,IndexField,TimestampField, UnixTimestampField, FrameLengthField, ClientIPField,
    QueryNameField, QueryClassField, QueryTypeField, QueryResponseCodeField))

  val InSchema = InStructType.fieldNames.map(col)

  assert(ModelSchema.fields.forall(InStructType.fields.contains(_)))

  val OutSchema = StructType(
    List(IdField,ScoreField)).fieldNames.map(col)
      //List(IdField,QueryNameField,ScoreField)).fieldNames.map(col)

}