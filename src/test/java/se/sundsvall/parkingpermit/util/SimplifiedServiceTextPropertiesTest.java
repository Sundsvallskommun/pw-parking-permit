package se.sundsvall.parkingpermit.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class SimplifiedServiceTextPropertiesTest {

	private static final String MESSAGE = "Kontrollmeddelande för förenklad delgivning";
	private static final String SUBJECT = "Kontrollmeddelande för förenklad delgivning";
	private static final String HTML_BODY = """
		<p><strong>Kontrollmeddelande f&ouml;r f&ouml;renklad delgivning</strong></p><p>Vi har nyligen delgivit dig ett beslut via brev. Du f&aring;r nu ett kontrollmeddelande f&ouml;r att s&auml;kerst&auml;lla att du mottagit informationen.</p>
		<p>N&aumlr det har g&aringtt tv&aring veckor fr&aringn det att beslutet skickades anses du blivit delgiven och du har d&aring tre veckor p&aring dig att &oumlverklaga beslutet.</p>
		<p>Om du bara f&aringtt kontkontrollmeddelandet men inte sj&auml;lva delgivningen med beslutet m&aring;ste du kontakta oss via e-post till</p>
		<p><a href="mailto:kontakt@sundsvall.se">kontakt@sundsvall.se</a> eller telefon till 060-19 10 00.</a></p>""";
	private static final String PLAIN_BODY = """
		Kontrollmeddelande för förenklad delgivning
		
		Vi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.
		När det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.
		Om du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till
		kontakt@sundsvall.se eller telefon till 060-19 10 00.""";

	@Autowired
	private SimplifiedServiceTextProperties simplifiedServiceTextProperties;

	@Test
	void toWebMessageRequest() {
		assertThat(simplifiedServiceTextProperties.message()).isEqualTo(MESSAGE);
		assertThat(simplifiedServiceTextProperties.subject()).isEqualTo(SUBJECT);
		assertThat(simplifiedServiceTextProperties.htmlBody()).isEqualTo(HTML_BODY);
		assertThat(simplifiedServiceTextProperties.plainBody()).isEqualTo(PLAIN_BODY);
	}
}
