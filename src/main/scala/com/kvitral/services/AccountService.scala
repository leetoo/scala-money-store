package com.kvitral.services

import cats.Monad
import cats.data.EitherT
import cats.syntax.all._
import com.kvitral.algebras.{AccountAlg, CurrenciesAlg, Logging}
import com.kvitral.model.errors.{AccountNotFound, AccountServiceErrors}
import com.kvitral.model.{Account, Transaction}

import scala.language.higherKinds

class AccountService[F[_]: Monad](
    accRepo: AccountAlg[F],
    logger: Logging[F],
    currenciesService: CurrenciesService[F]) {

  def getAccount(id: Long): F[Either[AccountNotFound.type, Account]] =
    for {
      _ <- logger.info(s"getting account for $id")
      account <- accRepo.getAccount(id)
    } yield account.toRight(AccountNotFound)

  def changeBalance(transaction: Transaction): F[Either[AccountServiceErrors, Unit]] =
    for {
      accFrom <- getAccount(transaction.from)
      accTo <- getAccount(transaction.to)
      curFrom = accFrom.map(_.currency)
      curTo = accTo.map(_.currency)
//      rate <- currenciesService.getCurrencyRate(curFrom, curTo)
      _ <- logger.info(s"changing balance with transaction $transaction")
      after <- accRepo.changeBalance(transaction)
    } yield after

}

object AccountService {
  def apply[F[_]: Monad](
      accRepo: AccountAlg[F],
      logger: Logging[F],
      currenciesService: CurrenciesService[F]): AccountService[F] =
    new AccountService(accRepo, logger, currenciesService)
}
