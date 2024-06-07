package com.learn.antivol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MailSender(private val username: String, private val appPassword: String) {

    private val TAG = "MailSender"

    suspend fun sendMail(subject: String, body: String, recipients: List<String>, attachmentPath: String) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.auth", "true")
                put("mail.smtp.port", "465")
            }

            val session = Session.getDefaultInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, appPassword)
                }
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    val addressArray = recipients.map { InternetAddress(it) }.toTypedArray()
                    setRecipients(Message.RecipientType.TO, addressArray)
                    setSubject(subject)
                    setText(body)
                }

                val multipart = MimeMultipart().apply {
                    val messageBodyPart = MimeBodyPart()
                    messageBodyPart.setText(body)
                    addBodyPart(messageBodyPart)

                    val attachBodyPart = MimeBodyPart()
                    attachBodyPart.dataHandler = DataHandler(FileDataSource(attachmentPath))
                    attachBodyPart.fileName = attachmentPath.substringAfterLast("/")
                    addBodyPart(attachBodyPart)
                }

                message.setContent(multipart)

                Transport.send(message)
                Log.d(TAG, "E-mail envoyé avec succès")
            } catch (e: MessagingException) {
                Log.e(TAG, "Erreur lors de l'envoi de l'e-mail", e)
            }
        }
    }
}
