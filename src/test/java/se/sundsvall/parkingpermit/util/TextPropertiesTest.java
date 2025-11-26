package se.sundsvall.parkingpermit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
	private static final String APPROVAL_SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String APPROVAL_HTML_BODY = """
		<!doctype html>
			<html lang="sv">
			<head>
				<meta charset="utf-8">
				<meta name="viewport" content="width=device-width,initial-scale=1">
				<title>Beslut</title>
			</head>
			<body>
				<p><strong>Hej</strong></p>
				<p>Du har fått ett beslut från Sundsvalls kommun.</p>
				<p>Med vänlig hälsning<br>
				<strong>Sundsvalls kommun</strong></p>
			</body>
		</html>
		""";

	// Common text properties
	private static final String COMMON_ORGANIZATION_NUMBER = "1122334455";
	private static final String COMMON_DEPARTMENT = "SBK(Gatuavdelningen, Trafiksektionen)";
	private static final String COMMON_CONTACTINFO_EMAIL = "sundsvalls.kommun@sundsvall.se";
	private static final String COMMON_CONTACTINFO_PHONENUMBER = "+46 60 191000";
	private static final String COMMON_CONTACTINFO_TEXT = "Kontakta oss via epost eller telefon.";
	private static final String COMMON_CONTACTINFO_URL = "https://sundsvall.se/";
	private static final String COMMON_FILE_NAME = "beslut.pdf";
	// Denial text properties
	private static final String DENIAL_MESSAGE = "Ärendet avskrivs";
	private static final String DENIAL_SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String DENIAL_HTML_BODY = """
		<!doctype html>
			<html lang="sv">
			<head>
				<meta charset="utf-8">
				<meta name="viewport" content="width=device-width,initial-scale=1">
				<title>Beslut</title>
			</head>
			<body>
				<p><strong>Hej</strong></p>
				<p>Du har fått ett beslut från Sundsvalls kommun.</p>
				<p>Med vänlig hälsning<br>
				<strong>Sundsvalls kommun</strong></p>
			</body>
		</html>
		""";
	private static final String DENIAL_PLAIN_BODY = """
		Hej

		Du har fått ett beslut från Sundsvalls kommun.

		Med vänlig hälsning
		Sundsvalls kommun""";
	private static final String DENIAL_DESCRIPTION = "Personen inte folkbokförd i Sundsvalls kommun.";
	private static final String DENIAL_TEMPLATE_ID = "sbk.prh.decision.all.rejection.municipality";
	// Simplified service text properties
	private static final String SIMPLIFIED_MESSAGE = "Kontrollmeddelande för förenklad delgivning";
	private static final String SIMPLIFIED_SUBJECT = "Kontrollmeddelande för förenklad delgivning";
	private static final String SIMPLIFIED_HTML_BODY = """
		      <!doctype html>
		          <html lang="sv">
		          <head>
		              <meta charset="utf-8">
		              <meta content="width=device-width, initial-scale=1" name="viewport">
		              <title>Kontrollmeddelande</title>
		          </head>
		          <body>
		              <p><strong>Kontrollmeddelande för förenklad delgivning</strong></p>
		              <p>Vi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.</p>
		              <p>När det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.</p>
		              <p>Om du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till</p>
		              <p><a href="mailto:kontakt@sundsvall.se">kontakt@sundsvall.se</a> eller telefon till 060-19 10 00.</p>
		         </body>
		</html>""";
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
		assertThat(normalizeHtml(textProperties.getApprovals().get(MUNICIPALITY_ID).getHtmlBody())).isEqualTo(normalizeHtml(APPROVAL_HTML_BODY));
		assertThat(textProperties.getApprovals().get(MUNICIPALITY_ID).getSubject()).isEqualTo(APPROVAL_SUBJECT);
	}

	@Test
	void commonTexts() {
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getOrganizationNumber()).isEqualTo(COMMON_ORGANIZATION_NUMBER);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getDepartment()).isEqualTo(COMMON_DEPARTMENT);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoEmail()).isEqualTo(COMMON_CONTACTINFO_EMAIL);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoPhonenumber()).isEqualTo(COMMON_CONTACTINFO_PHONENUMBER);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoText()).isEqualTo(COMMON_CONTACTINFO_TEXT);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getContactInfoUrl()).isEqualTo(COMMON_CONTACTINFO_URL);
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getSendDigitalMail()).isTrue();
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID_ANGE).getSendDigitalMail()).isFalse();
		assertThat(textProperties.getCommons().get(MUNICIPALITY_ID).getFilename()).isEqualTo(COMMON_FILE_NAME);
	}

	@Test
	void denialTexts() {
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getDescription()).isEqualTo(DENIAL_DESCRIPTION);
		assertThat(normalizeHtml(textProperties.getDenials().get(MUNICIPALITY_ID).getHtmlBody())).isEqualTo(normalizeHtml(DENIAL_HTML_BODY));
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getMessage()).isEqualTo(DENIAL_MESSAGE);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getPlainBody()).isEqualTo(DENIAL_PLAIN_BODY);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getSubject()).isEqualTo(DENIAL_SUBJECT);
		assertThat(textProperties.getDenials().get(MUNICIPALITY_ID).getTemplateId()).isEqualTo(DENIAL_TEMPLATE_ID);
	}

	@Test
	void simplifiedServiceTexts() {
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getMessage()).isEqualTo(SIMPLIFIED_MESSAGE);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getSubject()).isEqualTo(SIMPLIFIED_SUBJECT);
		assertThat(normalizeHtml(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getHtmlBody())).isEqualTo(normalizeHtml(SIMPLIFIED_HTML_BODY));
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getPlainBody()).isEqualTo(SIMPLIFIED_PLAIN_BODY);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getDescription()).isNull();
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getDelay()).isEqualTo("P1D");
	}

	private String normalizeHtml(String html) {
		Document doc = Jsoup.parse(html);
		doc.outputSettings()
			.prettyPrint(true)
			.indentAmount(2);
		return doc.outerHtml()
			.replace("\r\n", "\n")
			.replace("\r", "\n")
			.trim();
	}
}
