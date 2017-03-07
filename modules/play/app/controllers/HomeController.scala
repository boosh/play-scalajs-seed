package controllers


import javax.inject.{Inject, Singleton}

import play.api.mvc._
import play.api.libs.json._
import shared.SharedMessages

@Singleton
class HomeController @Inject() () extends Controller {

//  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def index = Action {
//    logger.info("Calling index")
    Ok(views.html.index(SharedMessages.itWorks))
  }
}
