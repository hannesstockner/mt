package com.hannesstockner.mt.api

import com.hannesstockner.mt.{AccountId, Amount}

case class TransferRequest(to: AccountId, amount: Amount)

case class DepositRequest(amount: Amount)
