package io.dlinov.auth.routes.json

import java.util.UUID

import CirceDecoders._
import CirceEncoders._
import io.circe.Json
import io.dlinov.auth.routes.dto.{BackOfficeUserToUpdate, PermissionKeys}
import org.scalatest.MustMatchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.dlinov.auth.routes.dto.{BackOfficeUserToUpdate, PermissionKeys}

class JsonSpec extends AnyWordSpecLike with MustMatchers {

  "Json entity encoders/decoders" should {
    "encode/decode BackOfficeUserToUpdate" in {
      val bouToUpdate = BackOfficeUserToUpdate.empty.copy(firstName = Some("Miroslav"))
      val encoded     = backOfficeUserToUpdateEncoder.encodeObject(bouToUpdate)
      val decoded     = bouToUpdateDecoder.decodeJson(Json.fromJsonObject(encoded))
      decoded mustBe Right(bouToUpdate)
    }
    "encode/decode PermissionKey" in {
      val userId   = UUID.randomUUID()
      val pKey1    = PermissionKeys.UserPermissionKey(userId)
      val encoded1 = pKeyEncoder.encodeObject(pKey1)
      val decoded1 = pKeyDecoder.decodeJson(Json.fromJsonObject(encoded1))
      pKeyEncoder(pKey1).noSpaces mustBe s"""{"user_id":"$userId"}"""
      decoded1 mustBe Right(pKey1)
      val businessUnitId = UUID.randomUUID()
      val roleId         = UUID.randomUUID()
      val pKey2          = PermissionKeys.BusinessUnitAndRolePermissionKey(businessUnitId, roleId)
      val encoded2       = pKeyEncoder.encodeObject(pKey2)
      val decoded2       = pKeyDecoder.decodeJson(Json.fromJsonObject(encoded2))
      pKeyEncoder(pKey2).noSpaces mustBe s"""{"bu_id":"$businessUnitId","role_id":"$roleId"}"""
      decoded2 mustBe Right(pKey2)
    }
  }
}
