package plugins.community.invoice.infrastructure;

import static org.junit.Assert.assertEquals;

import javax.mail.internet.InternetAddress;
import org.junit.Test;
import plugins.community.invoice.application.SmtpSettingsDto;

public class SmtpMailTransportTest {
    @Test public void createsUtf8SenderWithSeparateDisplayNameAndAddress() throws Exception {
        SmtpSettingsDto settings = new SmtpSettingsDto("smtp.example.org", 587,
                "Mathias Bauer - mbdus", "mathias.bauer@mbdus.de", "mailer", true, true);

        InternetAddress sender = SmtpMailTransport.senderAddress(settings);

        assertEquals("mathias.bauer@mbdus.de", sender.getAddress());
        assertEquals("Mathias Bauer - mbdus", sender.getPersonal());
        assertEquals("Mathias Bauer - mbdus <mathias.bauer@mbdus.de>", sender.toUnicodeString());
    }
}
