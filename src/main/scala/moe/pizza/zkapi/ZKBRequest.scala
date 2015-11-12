package moe.pizza.zkapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import dispatch._
import moe.pizza.zkapi.zkillboard.Killmail
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder

import scala.util.{Success, Failure, Try}

case class ZKBRequest(
                       baseurl: String = "https://zkillboard.com/api/",
                       useragent: String = "pizza-zkbapi, unknown application",
                       sort: String = "desc",
                       modifiers: Map[String,Long] = Map(),
                       page: Int = 1,
                       typemodifier: Option[String] = None,
                       start: Option[DateTime] = None,
                       end: Option[DateTime] = None

                       ) {
  implicit class EitherPimp[L <: Throwable,T](e:Either[L,T]){
    def toTry:Try[T] = e.fold(Failure(_), Success(_))
  }

  val OM = new ObjectMapper()
  OM.registerModule(new DefaultScalaModule)

  val zkbdateformatter = new DateTimeFormatterBuilder()
    .appendYear(4, 4)
    .appendMonthOfYear(2)
    .appendDayOfMonth(2)
    .appendHourOfDay(2)
    .appendMinuteOfHour(2)
    .toFormatter

  def sortAsc() = this.copy(sort = "asc")
  def sortDesc() = this.copy(sort = "desc")
  def page(i: Int) = this.copy(page = i)
  def userAgent(s: String) = this.copy(useragent = "pizza-zkbapi, %s".format(s))
  def start(s: DateTime) = this.copy(start = Some(s))
  def end(e: DateTime) = this.copy(end = Some(e))
  def kills() = this.copy(typemodifier = Some("kills"))
  def losses() = this.copy(typemodifier = Some("losses"))
  def wspace() = this.copy(typemodifier = Some("w-space"))
  def solo() = this.copy(typemodifier = Some("solo"))
  def characterID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("characterID" -> id))
  def corporationID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("corporationID" -> id))
  def allianceID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("allianceID" -> id))
  def factionID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("factionID" -> id))
  def shipTypeID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("shipTypeID" -> id))
  def groupID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("groupID" -> id))
  def solarSystemID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("solarSystemID" -> id))
  def regionID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("regionID" -> id))
  def warID(id: Long) = this.copy(modifiers = this.modifiers ++ Map("warID" -> id))

  def build(): Future[Try[List[Killmail]]] = {
    val mainurl = baseurl + "/orderDirection/%s".format(this.sort) + "/page/%d".format(page)
    val typestring = typemodifier match {
      case Some(t) => "/%s".format(t)
      case None => ""

    }
    val modifierstring = modifiers.toList.map(kv => "/%s/%d".format(kv._1, kv._2)).mkString
    val startstring = this.start match {
      case Some(s) => "/startTime/%s".format(zkbdateformatter.print(s))
      case None => ""
    }
    val endstring = this.end match {
      case Some(e) => "/endTime/%s".format(zkbdateformatter.print(e))
      case None => ""
    }

    val fullurl = mainurl + typestring + modifierstring + startstring + endstring
    val mysvc = url(fullurl).addHeader("User-Agent", this.useragent)
    var req = mysvc.GET
    // return as future either
    val response = Http(req OK as.String)
    response.either.map {
      case Right(r) => Right(OM.readValue(r, classOf[List[zkillboard.Killmail]]))
      case Left(t) => Left(t)
    }.map{_.toTry}
  }
}