package app.softnetwork.notification.model

import app.softnetwork.utils.HashTools
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}
import javax.mail.util.ByteArrayDataSource

class AttachmentSpec extends AnyFlatSpec with Matchers {

  "png attachment" should "work" in {
    val name = "avatar.png"
    val path =
      Paths.get(Thread.currentThread().getContextClassLoader.getResource(name).getPath)
    val bytes = Files.readAllBytes(path)
    val attachment = Attachment(name, path.toString, bytes)
    assert(attachment.name == name)
    assert(attachment.dataSource.isInstanceOf[ByteArrayDataSource])
    assert(attachment.`type`.isDefined)
    assert(attachment.`type`.getOrElse("") == "image/png")
    val md5 = HashTools
      .hashStream(
        new ByteArrayInputStream(bytes)
      )
      .getOrElse("")
    assert(
      HashTools
        .hashStream(
          new ByteArrayInputStream(attachment.bytes.map(_.toByteArray).getOrElse(Array.empty))
        )
        .getOrElse("") == md5
    )
  }

}
