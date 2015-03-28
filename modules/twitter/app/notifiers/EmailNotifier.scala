package notifiers

/**
 * Created by bharadwaj on 17/03/14.
 */
import com.typesafe.plugin._
import play.api.Play.current

object EmailNotifier {
  def sendMail {
    val mail = use[MailerPlugin].email
    mail.setSubject("Mail test")

    mail.setRecipient("Bharath <bharath12345@gmail.com>","bharath12345@yahoo.com")
    mail.setFrom("Bharath <bharath12345@gmail.com>")

    mail.setBcc(List("Dummy <example@example.org>", "Dummy2 <example@example.org>"):_*)
    mail.sendHtml("<html>html</html>")

    val user = "bharath"
    val stringBody = views.html.mailBody.render(user).body
    mail.sendHtml(stringBody)

  }
}
