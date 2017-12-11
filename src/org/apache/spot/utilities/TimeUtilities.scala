package org.apache.spot.utilities

object TimeUtilities {


  /**
    * It converts HH:MM:SS string to seconds
    *
    * @param timeStr This is time in the form of a string
    * @return It returns time converted to seconds
    */

  def getTimeAsDouble(timeStr: String) : Double = {
    val s = timeStr.split(":")
    val hours = s(0).toInt
    val minutes = s(1).toInt
    val seconds = s(2).toInt

    (3600*hours + 60*minutes + seconds).toDouble
  }

  /**
    * It takes only the hour element of time
    *
    * @param timeStr This is time in the form of a string
    * @return It returns only the hour of time
    */
  def getTimeAsHour(timeStr: String): Int = {
    val s = timeStr.split(":")
    val hours = s(0).toInt
    hours
  }

}