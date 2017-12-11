package org.apache.spot.dns

import org.apache.spark.sql.types._

/**
  * Data frame schemas and column names used in the DNS suspicious connects analysis.
  */
object DNSSchema {

  // input fields

  val ID = "id"
  val IdField = StructField(ID, StringType, nullable= true)
  
  val Index = "index"
  val IndexField = StructField(Index, StringType, nullable= true)
  
  val Timestamp = "frame_time"
  val TimestampField = StructField(Timestamp, StringType, nullable= true)

  val UnixTimestamp = "unix_tstamp"
  val UnixTimestampField = StructField(UnixTimestamp, StringType, nullable= true)

  val FrameLength = "frame_len"
  val FrameLengthField = StructField(FrameLength, StringType, nullable= true)

  val ClientIP = "ip_dst"
  val ClientIPField = StructField(ClientIP, StringType, nullable= true)

  val ServerIP = "ip_src"
  val ServerIPField = StructField(ServerIP, StringType, nullable= true)

  val QueryName = "dns_qry_name"
  val QueryNameField = StructField(QueryName, StringType, nullable= true)

  val QueryClass = "dns_qry_class"
  val QueryClassField = StructField(QueryClass, StringType, nullable= true)

  val QueryType = "dns_qry_type"
  val QueryTypeField = StructField(QueryType, StringType, nullable= true)

  val QueryResponseCode = "dns_qry_rcode"
  val QueryResponseCodeField = StructField(QueryResponseCode, StringType, nullable= true)

  val AnswerAddress = "dns_a"
  val AnswerAddressField = StructField(AnswerAddress, StringType, nullable= true)

  // intermediate and derived fields

  val Domain = "domain"
  val DomainField = StructField(Domain, StringType)

  val TopDomain = "top_domain"
  val TopDomainField = StructField(TopDomain, IntegerType)

  val Subdomain = "subdomain"
  val SubdomainField = StructField(Subdomain, StringType)

  val SubdomainLength = "subdomain_length"
  val SubdomainLengthField = StructField(SubdomainLength, IntegerType)

  val NumPeriods = "num_periods"
  val NumPeriodsField = StructField(NumPeriods, IntegerType)

  val SubdomainEntropy = "subdomain_entropy"
  val SubdomainEntropyField = StructField(SubdomainEntropy, DoubleType)

  // output fields

  val Word = "word"
  val WordField = StructField(Word, StringType)

  val Score = "dns_score"
  val ScoreField = StructField(Score, DoubleType)
}