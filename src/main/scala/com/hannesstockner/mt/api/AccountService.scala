package com.hannesstockner.mt.api

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.util.Timeout
import com.hannesstockner.mt.{AccountId, DepositId, TransferId}
import com.hannesstockner.mt.domain.account.{AccountEvent, DepositAccount, OpenAccount, SendTransfer}
import com.hannesstockner.mt.report.{AccountReport, AccountReportByAccountId}
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  import com.hannesstockner.mt.JsonProtocol._

  implicit val trf = com.hannesstockner.mt.report.JsonProtocol.transferReportFormat
  implicit val drf = com.hannesstockner.mt.report.JsonProtocol.depositReportFormat
  implicit val arf = com.hannesstockner.mt.report.JsonProtocol.accountReportFormat

  implicit val transferRequestFormat = jsonFormat2(TransferRequest)
  implicit val depositRequestFormat = jsonFormat1(DepositRequest)
}

class AccountService(commandCoordinator: ActorRef, accountReportRepo: ActorRef)(implicit ex: ExecutionContext) extends Directives with JsonSupport {

  implicit val timeout = Timeout(2.seconds)

  val route = accountById ~ deposit ~ transfer ~ openAccount

  def accountById =
    path("accounts" / Segment) { id =>
      get {
        onComplete((accountReportRepo ? AccountReportByAccountId(AccountId(id))).mapTo[Option[AccountReport]]) {
          case Success(s) => s match {
            case Some(r) =>
              complete(r)
            case None => complete(StatusCodes.NotFound)
          }
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ ex.getMessage }.")
        }
      }
    }

  def openAccount =
    path("accounts") {
      post {
        onComplete((commandCoordinator ? OpenAccount()).mapTo[Either[String, AccountEvent]]) {
          case Success(s) => s match {
            case Right(e) => extractRequest { request =>
              respondWithHeader(RawHeader("Location", s"${ request.uri }/${ e.id.value }")) {
                complete(StatusCodes.Accepted)
              }
            }
            case Left(msg) => complete(StatusCodes.BadRequest, msg)
          }
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ ex.getMessage }.")
        }
      }
    }

  def deposit =
    path("accounts" / Segment / "deposits") { accountId =>
      post {
        entity(as[DepositRequest]) { request: DepositRequest =>
          onComplete((commandCoordinator ? DepositAccount(id = AccountId(accountId), depositId = DepositId(), amount = request.amount)).mapTo[Either[String, AccountEvent]]) {
            case Success(s) => s match {
              case Right(e) => extractRequest { request =>
                respondWithHeader(RawHeader("Location", s"${ request.uri }/${ e.id.value }")) {
                  complete(StatusCodes.Accepted)
                }
              }
              case Left(msg) => complete(StatusCodes.BadRequest, msg)
            }
            case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ ex.getMessage }.")
          }
        }
      }
    }

  def transfer =
    path("accounts" / Segment / "transfers") { accountId =>
      post {
        entity(as[TransferRequest]) { request: TransferRequest =>
          onComplete((commandCoordinator ? SendTransfer(id = AccountId(accountId), transferId = TransferId(), to = request.to, amount = request.amount)).mapTo[Either[String, AccountEvent]]) {
            case Success(s) => s match {
              case Right(e) => extractRequest { request =>
                respondWithHeader(RawHeader("Location", s"${ request.uri }/${ e.id.value }")) {
                  complete(StatusCodes.Accepted)
                }
              }
              case Left(msg) => complete(StatusCodes.BadRequest, msg)
            }
            case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ ex.getMessage }.")
          }
        }
      }
    }
}
