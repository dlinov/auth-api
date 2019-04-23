package io.dlinov.auth.domain.customer.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.CustomerAttributes.{AccountNumber, AccountStatus, AccountType, NameAttribute}
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.CustomerAttributes._

case class Account(id: UUID, accountNumber: AccountNumber, accountName: NameAttribute,
    accountType: AccountType, isMainAccount: Boolean, currency: Currency,
    balance: BigDecimal, blockedBalance: BigDecimal, accountStatus: AccountStatus,
    lastTransactionAt: LocalDateTime, createdAt: LocalDateTime,
    createdBy: BackOfficeUser, updatedBy: Option[BackOfficeUser],
    updatedAt: Option[LocalDateTime], closedAt: Option[LocalDateTime])
