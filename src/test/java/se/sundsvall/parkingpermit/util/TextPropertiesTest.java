package se.sundsvall.parkingpermit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class TextPropertiesTest {

	static final String MUNICIPALITY_ID = "2281";
	static final String MUNICIPALITY_ID_ANGE = "2260";
	// Approval text properties
	private static final String APPROVAL_DESCRIPTION = "Personen är folkbokförd i Sundsvalls kommun. Rekommenderat beslut är att godkänna ansökan.";
	private static final String APPROVAL_FILE_NAME = "beslut.pdf";
	private static final String APPROVAL_SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String APPROVAL_HTML_BODY = "<p><strong>Hej</strong></p><p>Du har f&aring;tt ett beslut fr&aring;n Sundsvalls kommun.</p><p>Med v&auml;nlig h&auml;lsning<br /><strong>Sundsvalls kommun</strong></p>";

	// Common text properties
	private static final String COMMON_DEPARTMENT = "SBK(Gatuavdelningen, Trafiksektionen)";
	private static final String COMMON_CONTACTINFO_EMAIL = "sundsvalls.kommun@sundsvall.se";
	private static final String COMMON_CONTACTINFO_PHONENUMBER = "+46 60 191000";
	private static final String COMMON_CONTACTINFO_TEXT = "Kontakta oss via epost eller telefon.";
	private static final String COMMON_CONTACTINFO_URL = "https://sundsvall.se/";
	// Denial text properties
	private static final String DENIAL_FILE_NAME = "beslut.pdf";
	private static final String DENIAL_MESSAGE = "Ärendet avskrivs";
	private static final String DENIAL_SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String DENIAL_HTML_BODY = "<p><strong>Hej</strong></p><p>Du har f&aring;tt ett beslut fr&aring;n Sundsvalls kommun.</p><p>Med v&auml;nlig h&auml;lsning<br /><strong>Sundsvalls kommun</strong></p>";
	private static final String DENIAL_PLAIN_BODY = """
		Hej

		Du har fått ett beslut från Sundsvalls kommun.

		Med vänlig hälsning
		Sundsvalls kommun""";
	private static final String DENIAL_DESCRIPTION = "Personen inte folkbokförd i Sundsvalls kommun.";
	private static final String DENIAL_LAW_ARTICLE = "8";
	private static final String DENIAL_LAW_CHAPTER = "13";
	private static final String DENIAL_LAW_HEADING = "13 kap. 8§ Parkeringstillstånd för rörelsehindrade";
	private static final String DENIAL_LAW_SFS = "Trafikförordningen (1998:1276)";
	private static final String DENIAL_TEMPLATE_ID = "sbk.prh.decision.all.rejection.municipality";
	// Simplified service text properties
	private static final String SIMPLIFIED_MESSAGE = "Kontrollmeddelande för förenklad delgivning";
	private static final String SIMPLIFIED_SUBJECT = "Kontrollmeddelande för förenklad delgivning";
	private static final String SIMPLIFIED_HTML_BODY = """
		<p><strong>Kontrollmeddelande f&ouml;r f&ouml;renklad delgivning</strong></p><p>Vi har nyligen delgivit dig ett beslut via brev. Du f&aring;r nu ett kontrollmeddelande f&ouml;r att s&auml;kerst&auml;lla att du mottagit informationen.</p>
		<p>N&aumlr det har g&aringtt tv&aring veckor fr&aringn det att beslutet skickades anses du blivit delgiven och du har d&aring tre veckor p&aring dig att &oumlverklaga beslutet.</p>
		<p>Om du bara f&aringtt kontkontrollmeddelandet men inte sj&auml;lva delgivningen med beslutet m&aring;ste du kontakta oss via e-post till</p>
		<p><a href="mailto:kontakt@sundsvall.se">kontakt@sundsvall.se</a> eller telefon till 060-19 10 00.</p>""";
	private static final String SIMPLIFIED_PLAIN_BODY = """
		Kontrollmeddelande för förenklad delgivning

		Vi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.
		När det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.
		Om du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till
		kontakt@sundsvall.se eller telefon till 060-19 10 00.""";

	@Autowired
	private TextProperties textProperties;

	@Test
	void approvalText() {
		assertThat(textProperties.getApprovals().get(MUNICIPALITY_ID).getDescription()).isEqualTo(APPROVAL_DESCRIPTION);
		assertThat(textProperties.getApprovals().get(MUNICIPALITY_ID).getFilename()).isEqualTo(APPROVAL_FILE_NAME);
		assertThat(textProperties.getApprovals().get(MUNICIPALITY_ID).getHtmlBody()).isEqualTo(APPROVAL_HTML_BODY);
		assertThat(textProperties.getApprovals().get(MUNICIPALITY_ID).getSubject()).isEqualTo(APPROVAL_SUBJECT);
	}

	@Test
	void commonTexts() {
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getDepartment()).isEqualTo(COMMON_DEPARTMENT);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoEmail()).isEqualTo(COMMON_CONTACTINFO_EMAIL);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoPhonenumber()).isEqualTo(COMMON_CONTACTINFO_PHONENUMBER);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoText()).isEqualTo(COMMON_CONTACTINFO_TEXT);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoUrl()).isEqualTo(COMMON_CONTACTINFO_URL);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getSendDigitalMail()).isTrue();
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID_ANGE).getSendDigitalMail()).isFalse();
	}

	@Test
	void denialTexts() {
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getDescription()).isEqualTo(DENIAL_DESCRIPTION);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getFilename()).isEqualTo(DENIAL_FILE_NAME);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getHtmlBody()).isEqualTo(DENIAL_HTML_BODY);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getLawArticle()).isEqualTo(DENIAL_LAW_ARTICLE);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getLawChapter()).isEqualTo(DENIAL_LAW_CHAPTER);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getLawHeading()).isEqualTo(DENIAL_LAW_HEADING);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getLawSfs()).isEqualTo(DENIAL_LAW_SFS);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getMessage()).isEqualTo(DENIAL_MESSAGE);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getPlainBody()).isEqualTo(DENIAL_PLAIN_BODY);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getSubject()).isEqualTo(DENIAL_SUBJECT);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getTemplateId()).isEqualTo(DENIAL_TEMPLATE_ID);
	}

	@Test
	void simplifiedServiceTexts() {
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getMessage()).isEqualTo(SIMPLIFIED_MESSAGE);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getSubject()).isEqualTo(SIMPLIFIED_SUBJECT);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getHtmlBody()).isEqualTo(SIMPLIFIED_HTML_BODY);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getPlainBody()).isEqualTo(SIMPLIFIED_PLAIN_BODY);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getDescription()).isNull();
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getDelay()).isEqualTo("P1D");
	}
}
