package core.i18n;

import static org.junit.Assert.*;
import java.util.Locale;
import org.junit.Test;

public class DefaultLocalizationServiceTest {
    @Test public void switchesLocaleWithoutRestartAndUsesRequestedBundle() {
        DefaultLocalizationService service = new DefaultLocalizationService();
        service.setLocale(Locale.FRENCH);
        assertEquals("fr", service.locale().getLanguage());
        assertEquals("Profil", service.messages("i18n.profile").text("profile.title"));
    }

    @Test public void fallsBackToEnglishForUnsupportedLanguageAndMissingKeys() {
        DefaultLocalizationService service = new DefaultLocalizationService();
        service.setLocale(new Locale("ru"));
        assertEquals(Locale.ENGLISH, service.locale());
        assertEquals("Profile", service.messages("i18n.profile").text("profile.title"));
        assertEquals("missing.key", service.messages("i18n.profile").text("missing.key"));
    }

	@Test public void readsPluginPropertiesAsUtf8() {
		DefaultLocalizationService service = new DefaultLocalizationService();
		service.setLocale(Locale.GERMAN);
		assertEquals("Benutzer hinzufügen",
				service.messages("plugins.core.users.messages").text("userview.adduser"));
	}

	@Test public void leavesLiteralTemplateBracesUntouchedWithoutFormatArguments() {
		DefaultLocalizationService service = new DefaultLocalizationService();
		service.setLocale(Locale.GERMAN);
		String help = service.messages("plugins.community.invoice.messages").text("invoice.templateHelp");
		assertTrue(help.contains("{{lines}}"));
		assertTrue(help.contains("{{seller.address}}"));
	}
}
