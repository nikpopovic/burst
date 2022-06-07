/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.catalog.persist.account

import org.burstsys.catalog.model.account.CatalogAccount
import org.burstsys.catalog.model.account.CatalogSqlAccount
import org.burstsys.catalog.persist.NamedCatalogEntityPersister
import org.burstsys.relate.RelateExceptions.BurstUnknownMonikerException
import org.burstsys.relate._
import org.burstsys.relate.dialect.RelateDerbyDialect
import org.burstsys.relate.dialect.RelateMySqlDialect
import org.burstsys.vitals.errors.VitalsException
import org.burstsys.vitals.properties._
import org.bouncycastle.crypto.generators.BCrypt
import scalikejdbc.WrappedResultSet
import scalikejdbc._

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.util.Failure
import scala.util.Success
import scala.util.Try

final case
class CatalogAccountPersister(service: RelateService) extends NamedCatalogEntityPersister[CatalogAccount]
  with CatalogAccountSql {

  override val sqlTableName: String = "burst_catalog_account"

  private val passwordColumn = "password"

  private val saltColumn = "salt"

  override val columns: Seq[String] = Seq(entityPkColumn, monikerColumn, passwordColumn, saltColumn, labelsColumn)

  override val nameConverters = Map("^hashedPassword$" -> "password")

  override def resultToEntity(rs: WrappedResultSet): CatalogSqlAccount = CatalogSqlAccount(
    rs.long(entityPkColumn),
    rs.string(monikerColumn),
    rs.string(passwordColumn),
    rs.string(saltColumn),
    stringToOptionalPropertyMap(rs.string(labelsColumn))
  )

  override def createTableSql: TableCreateSql = service.dialect match {
    case RelateMySqlDialect => mysqlCreateTableSql
    case RelateDerbyDialect => derbyCreateTableSql
  }

  override def insertEntitySql(entity: CatalogAccount): WriteSql = {
    this.column
    sql"""
     INSERT INTO  ${this.table}
       (${this.column.labels}, ${this.column.moniker}, ${this.column.hashedPassword}, ${this.column.salt})
     VALUES
       ({labels}, {moniker}, {password}, {salt})
     """.bindByName(
      'labels -> entity.labels,
      'moniker -> entity.moniker,
      'password -> entity.hashedPassword,
      'salt -> entity.salt
    )
  }

  override def updateEntityByPkSql(entity: CatalogAccount): WriteSql = {
    sql"""
     UPDATE  ${this.table}
     SET
       ${this.column.moniker} = {moniker},
       ${this.column.labels} = {labels},
       ${this.column.hashedPassword} = {password},
       ${this.column.salt} = {salt}
     WHERE
       ${this.column.pk} = {pk}
     """.bindByName(
      'pk -> entity.pk,
      'moniker -> entity.moniker,
      'labels -> optionalPropertyMapToString(entity.labels),
      'password -> entity.hashedPassword,
      'salt -> entity.salt
    )
  }

  def insertAccount(username: String, password: String)(implicit session: DBSession): RelatePk = {
    val salt = newSalt
    val newAccount = CatalogSqlAccount(0, username, getPasswordHash(password, salt), salt, Option.empty)
    insertEntity(newAccount)
  }

  def changePassword(username: String, oldPassword: String, newPassword: String)(implicit session: DBSession): Try[Boolean] = {
    findEntityByMoniker(username) map { account =>
      if (getPasswordHash(oldPassword, account.salt) != account.hashedPassword) return Success(false)
      val updatedAccount = account.copy(hashedPassword = getPasswordHash(newPassword, account.salt))
      try {
        updateEntity(updatedAccount)
        Success(true)
      } catch handleSqlException(service.dialect) {
        case t => Failure(t)
      }
    } getOrElse Failure(BurstUnknownMonikerException(username))
  }

  def verifyAcount(username: String, password: String)(implicit session: DBSession): Try[CatalogAccount] = {
    findEntityByMoniker(username) map { account =>
      if (getPasswordHash(password, account.salt) == account.hashedPassword) {
        Success(account)
      } else {
        Failure(VitalsException("Incorrect password"))
      }
    } getOrElse Failure(BurstUnknownMonikerException(username))
  }

  private val utf8 = StandardCharsets.UTF_8

  private def newSalt: String = new String(UUID.randomUUID().toString.getBytes(utf8).slice(0, 16))

  private def getPasswordHash(password: String, salt: String): String = {
    new String(BCrypt.generate(password.getBytes(utf8), salt.getBytes(utf8), 10), utf8)
  }
}