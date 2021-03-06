package utils

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

object Errors {
  def show(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map {
      case (path, e) => path.toString() +  " : " + e.map(_.toString).mkString(" ")
    }.mkString("\n")
  }
}