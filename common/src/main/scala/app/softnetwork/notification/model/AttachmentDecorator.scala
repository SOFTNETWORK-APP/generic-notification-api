package app.softnetwork.notification.model

import app.softnetwork.utils.MimeTypeTools
import com.google.protobuf.ByteString

import javax.activation.{DataSource, FileDataSource}
import javax.mail.util.ByteArrayDataSource

trait AttachmentDecorator { _: Attachment =>

  def copyFrom(bytes: Array[Byte]): Attachment = this.withBytes(ByteString.copyFrom(bytes))

  lazy val arrayOfBytes: Option[Array[Byte]] = bytes.map(_.toByteArray)

  lazy val `type`: Option[String] = arrayOfBytes.flatMap(MimeTypeTools.detectMimeType)

  lazy val dataSource: DataSource = arrayOfBytes
    .map { b =>
      val ds = new ByteArrayDataSource(b, `type`.getOrElse("application/octet-stream"))
      ds.setName(name)
      ds
    }
    .getOrElse(new FileDataSource(path))

}
