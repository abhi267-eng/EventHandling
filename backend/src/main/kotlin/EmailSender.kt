package com.eventhandling

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

interface EmailSender {
    fun send(to: String, subject: String, body: String)
}

/*
 * Default mode: prints the email in the server terminal.
 * Needs no account, so every teammate can run the project.
 */
class ConsoleEmailSender : EmailSender {

    override fun send(to: String, subject: String, body: String) {
        println("---------- EMAIL (console mode) ----------")
        println("To: $to")
        println("Subject: $subject")
        println(body)
        println("------------------------------------------")
    }
}

class SmtpEmailSender(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val password: String,
    private val fromAddress: String
) : EmailSender {

    override fun send(to: String, subject: String, body: String) {

        val properties = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")

            if (port == 465) {
                put("mail.smtp.ssl.enable", "true")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
        }

        val session = Session.getInstance(
            properties,
            object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(user, password)
            }
        )

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        message.setSubject(subject, "UTF-8")
        message.setText(body, "UTF-8")

        Transport.send(message)
    }
}

object EmailSenders {

    val default: EmailSender by lazy { fromEnvironment() }

    private fun fromEnvironment(): EmailSender {

        val host = System.getenv("SMTP_HOST")
        val user = System.getenv("SMTP_USER")
        val password = System.getenv("SMTP_PASSWORD")

        if (host.isNullOrBlank() || user.isNullOrBlank() || password.isNullOrBlank()) {
            println("Email mode: console (set SMTP_HOST, SMTP_USER and SMTP_PASSWORD to send real emails)")
            return ConsoleEmailSender()
        }

        val port = System.getenv("SMTP_PORT")?.toIntOrNull() ?: 587
        val from = System.getenv("SMTP_FROM")?.takeIf { it.isNotBlank() } ?: user

        println("Email mode: SMTP via $host:$port")
        return SmtpEmailSender(host, port, user, password, from)
    }
}