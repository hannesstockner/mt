package com.hannesstockner.mt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.hannesstockner.mt.api.AccountService
import com.hannesstockner.mt.domain.account.{AccountProcessor, CommandCoordinator}
import com.hannesstockner.mt.domain.transfer.{EventCoordinator, TransferFSM}
import com.hannesstockner.mt.report.AccountReportRepo

object Context {

  def start(implicit system: ActorSystem, port: Int = 8005): Route = {
    sys.addShutdownHook(system.terminate())

    implicit val mat = ActorMaterializer()

    import system.dispatcher

    val accountReportRepo = system.actorOf(AccountReportRepo.props(subscribe = system.eventStream.subscribe), "accountReportRepo")

    val commandCoordinator = system.actorOf(CommandCoordinator.props(toAccount = AccountProcessor.props(publish = system.eventStream.publish)), "commandCoordinator")
    system.actorOf(
      EventCoordinator.props(toTransfer = TransferFSM.props(commandCoordinator = commandCoordinator), subscribe = system.eventStream.subscribe), "eventCoordinator")

    val accountService = new AccountService(commandCoordinator = commandCoordinator, accountReportRepo = accountReportRepo)

    Http().bindAndHandle(accountService.route, "0.0.0.0", port)

    accountService.route
  }

}
