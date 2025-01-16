package app.softnetwork.notification.service

import app.softnetwork.notification.scalatest.AllNotificationsRoutesTestKit
import app.softnetwork.session.handlers.{JwtClaimsRefreshTokenDao, SessionRefreshTokenDao}
import app.softnetwork.session.model.SessionDataCompanion
import app.softnetwork.session.service.{BasicSessionMaterials, JwtSessionMaterials}
import com.softwaremill.session.RefreshTokenStorage
import org.softnetwork.session.model.{JwtClaims, Session}

package Directives {

  package OneOff {
    package Cookie {

      import app.softnetwork.session.scalatest.OneOffCookieSessionServiceTestKit

      class AllNotificationsRoutesWithOneOffCookieBasicSessionSpec
          extends NotificationServiceSpec[Session]
          with OneOffCookieSessionServiceTestKit[Session]
          with AllNotificationsRoutesTestKit[Session]
          with BasicSessionMaterials[Session] {

        override implicit def refreshTokenStorage: RefreshTokenStorage[Session] =
          SessionRefreshTokenDao(ts)

        override implicit def companion: SessionDataCompanion[Session] = Session
      }

      class AllNotificationsRoutesWithOneOffCookieJwtSessionSpec
          extends NotificationServiceSpec[JwtClaims]
          with OneOffCookieSessionServiceTestKit[JwtClaims]
          with AllNotificationsRoutesTestKit[JwtClaims]
          with JwtSessionMaterials[JwtClaims] {
        override implicit def companion: SessionDataCompanion[JwtClaims] = JwtClaims

        override implicit def refreshTokenStorage: RefreshTokenStorage[JwtClaims] =
          JwtClaimsRefreshTokenDao(ts)
      }
    }

    package Header {

      import app.softnetwork.session.scalatest.OneOffHeaderSessionServiceTestKit

      class AllNotificationsRoutesWithOneOffHeaderBasicSessionSpec
          extends NotificationServiceSpec[Session]
          with OneOffHeaderSessionServiceTestKit[Session]
          with AllNotificationsRoutesTestKit[Session]
          with BasicSessionMaterials[Session] {
        override implicit def refreshTokenStorage: RefreshTokenStorage[Session] =
          SessionRefreshTokenDao(ts)

        override implicit def companion: SessionDataCompanion[Session] = Session
      }

      class AllNotificationsRoutesWithOneOffHeaderJwtSessionSpec
          extends NotificationServiceSpec[JwtClaims]
          with OneOffHeaderSessionServiceTestKit[JwtClaims]
          with AllNotificationsRoutesTestKit[JwtClaims]
          with JwtSessionMaterials[JwtClaims] {
        override implicit def companion: SessionDataCompanion[JwtClaims] = JwtClaims

        override implicit def refreshTokenStorage: RefreshTokenStorage[JwtClaims] =
          JwtClaimsRefreshTokenDao(ts)
      }

    }
  }

  package Refreshable {
    package Cookie {

      import app.softnetwork.session.scalatest.RefreshableCookieSessionServiceTestKit

      class AllNotificationsRoutesWithRefreshableCookieBasicSessionSpec
          extends NotificationServiceSpec[Session]
          with RefreshableCookieSessionServiceTestKit[Session]
          with AllNotificationsRoutesTestKit[Session]
          with BasicSessionMaterials[Session] {
        override implicit def refreshTokenStorage: RefreshTokenStorage[Session] =
          SessionRefreshTokenDao(ts)

        override implicit def companion: SessionDataCompanion[Session] = Session
      }

      class AllNotificationsRoutesWithRefreshableCookieJwtSessionSpec
          extends NotificationServiceSpec[JwtClaims]
          with RefreshableCookieSessionServiceTestKit[JwtClaims]
          with AllNotificationsRoutesTestKit[JwtClaims]
          with JwtSessionMaterials[JwtClaims] {
        override implicit def companion: SessionDataCompanion[JwtClaims] = JwtClaims

        override implicit def refreshTokenStorage: RefreshTokenStorage[JwtClaims] =
          JwtClaimsRefreshTokenDao(ts)
      }

    }

    package Header {

      import app.softnetwork.session.scalatest.RefreshableHeaderSessionServiceTestKit

      class AllNotificationsRoutesWithRefreshableHeaderBasicSessionSpec
          extends NotificationServiceSpec[Session]
          with RefreshableHeaderSessionServiceTestKit[Session]
          with AllNotificationsRoutesTestKit[Session]
          with BasicSessionMaterials[Session] {
        override implicit def refreshTokenStorage: RefreshTokenStorage[Session] =
          SessionRefreshTokenDao(ts)

        override implicit def companion: SessionDataCompanion[Session] = Session
      }

      class AllNotificationsRoutesWithRefreshableHeaderJwtSessionSpec
          extends NotificationServiceSpec[JwtClaims]
          with RefreshableHeaderSessionServiceTestKit[JwtClaims]
          with AllNotificationsRoutesTestKit[JwtClaims]
          with JwtSessionMaterials[JwtClaims] {
        override implicit def companion: SessionDataCompanion[JwtClaims] = JwtClaims

        override implicit def refreshTokenStorage: RefreshTokenStorage[JwtClaims] =
          JwtClaimsRefreshTokenDao(ts)
      }

    }

  }

}
