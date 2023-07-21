package se.sundsvall.parkingpermit.integration.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.parkingpermit.Application;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class MessagingMapperPropertiesTest {
	private static final String DEPARTMENT = "SBK(Gatuavdelningen, Trafiksektionen)";
	private static final String FILE_NAME = "beslut.pdf";
	private static final String MESSAGE = "Ärendet avskrivs";
	private static final String SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String HTML_BODY = "<p><strong>Hej</strong></p><p>Du har f&aring;tt ett beslut fr&aring;n Sundsvalls kommun.</p><p>Med v&auml;nlig h&auml;lsning<br /><strong>Sundsvalls kommun</strong></p>";
	private static final String PLAIN_BODY = """
		Hej

		Du har fått ett beslut från Sundsvalls kommun.

		Med vänlig hälsning
		Sundsvalls kommun""";
	private static final String CONTACTINFO_EMAIL = "sundsvalls.kommun@sundsvall.se";
	private static final String CONTACTINFO_PHONENUMBER = "4660191000";
	private static final String CONTACTINFO_TEXT = "Kontakta oss via epost eller telefon.";
	private static final String CONTACTINFO_URL = "https://sundsvall.se/";
	private static final String APPROVAL_DESCRIPTION = "Personen är folkbokförd i Sundsvalls kommun. Rekommenderat beslut är att godkänna ansökan.";
	private static final String DISMISSAL_DESCRIPTION = "Personen inte folkbokförd i Sundsvalls kommun.";
	private static final String LAW_ARTICLE = "8";
	private static final String LAW_CHAPTER = "13";
	private static final String LAW_HEADING = "13 kap. 8§ Parkeringstillstånd för rörelsehindrade";
	private static final String LAW_SFS = "Trafikförordningen (1998:1276)";

	@Autowired
	private MessagingMapperProperties messagingMapperProperties;

	@Test
	void toWebMessageRequest() {
		assertThat(messagingMapperProperties.approvalDescription()).isEqualTo(APPROVAL_DESCRIPTION);
		assertThat(messagingMapperProperties.contactInfoEmail()).isEqualTo(CONTACTINFO_EMAIL);
		assertThat(messagingMapperProperties.contactInfoPhonenumber()).isEqualTo(CONTACTINFO_PHONENUMBER);
		assertThat(messagingMapperProperties.contactInfoText()).isEqualTo(CONTACTINFO_TEXT);
		assertThat(messagingMapperProperties.contactInfoUrl()).isEqualTo(CONTACTINFO_URL);
		assertThat(messagingMapperProperties.department()).isEqualTo(DEPARTMENT);
		assertThat(messagingMapperProperties.dismissalDescription()).isEqualTo(DISMISSAL_DESCRIPTION);
		assertThat(messagingMapperProperties.filename()).isEqualTo(FILE_NAME);
		assertThat(messagingMapperProperties.htmlBody()).isEqualTo(HTML_BODY);
		assertThat(messagingMapperProperties.lawArticle()).isEqualTo(LAW_ARTICLE);
		assertThat(messagingMapperProperties.lawChapter()).isEqualTo(LAW_CHAPTER);
		assertThat(messagingMapperProperties.lawHeading()).isEqualTo(LAW_HEADING);
		assertThat(messagingMapperProperties.lawSfs()).isEqualTo(LAW_SFS);
		assertThat(messagingMapperProperties.message()).isEqualTo(MESSAGE);
		assertThat(messagingMapperProperties.plainBody()).isEqualTo(PLAIN_BODY);
		assertThat(messagingMapperProperties.subject()).isEqualTo(SUBJECT);

	}
}
