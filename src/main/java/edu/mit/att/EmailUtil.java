package edu.mit.att;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/*
This could be replaced with a generic message-oriented interface
 */
@Component
public class EmailUtil {

    private final Logger logger = getLogger(this.getClass());

    @Autowired
    public EmailUtil() {
    }

    @Autowired
    public JavaMailSender emailSender;

    public void notify(final String to, final String subject, final String text) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            emailSender.send(message);
            logger.debug("Email sent to:{} with subject:{}", to, subject);
        } catch (MailException e) {
            logger.debug("Error sending email to: {}", to, e);
        }
    }

}


