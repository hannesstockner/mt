package com.hannesstockner.mt.domain.account

import com.hannesstockner.mt.Amount
import org.scalatest.{FunSuite, Matchers}

class BalanceSpec extends FunSuite with Matchers {

  test("A Balance should withdraw an amount") {
    val balance = Balance(Amount(100))

    balance.withdraw(Amount(25)) shouldBe Balance(Amount(75))
  }

  test("A Balance should deposit an amount") {
    val balance = Balance(Amount(100))

    balance.deposit(Amount(25)) shouldBe Balance(Amount(125))
  }

  test("A Balance should result in negative when balance is negative") {
    val balance = Balance(Amount(100))

    assert(balance.withdrawWillCauseNegativeBalance(Amount(125)))
  }

}
