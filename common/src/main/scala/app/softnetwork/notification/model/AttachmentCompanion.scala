package app.softnetwork.notification.model

trait AttachmentCompanion {
  def apply(name: String, path: String, bytes: Array[Byte]): Attachment = {
    Attachment.defaultInstance.withName(name).withPath(path).copyFrom(bytes)
  }
}
