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
class DenialTextPropertiesTest {

	private static final String FILE_NAME = "beslut.pdf";
	private static final String MESSAGE = "Ärendet avskrivs";
	private static final String SUBJECT = "Beslut från Sundsvalls kommun";
	private static final String HTML_BODY = "<p><strong>Hej</strong></p><p>Du har f&aring;tt ett beslut fr&aring;n Sundsvalls kommun.</p><p>Med v&auml;nlig h&auml;lsning<br /><strong>Sundsvalls kommun</strong></p>";
	private static final String PLAIN_BODY = """
		Hej

		Du har fått ett beslut från Sundsvalls kommun.

		Med vänlig hälsning
		Sundsvalls kommun""";
	private static final String DESCRIPTION = "Personen inte folkbokförd i Sundsvalls kommun.";
	private static final String LAW_ARTICLE = "8";
	private static final String LAW_CHAPTER = "13";
	private static final String LAW_HEADING = "13 kap. 8§ Parkeringstillstånd för rörelsehindrade";
	private static final String LAW_SFS = "Trafikförordningen (1998:1276)";

	@Autowired
	private DenialTextProperties denialTextProperties;

	@Test
	void toWebMessageRequest() {
		assertThat(denialTextProperties.description()).isEqualTo(DESCRIPTION);
		assertThat(denialTextProperties.filename()).isEqualTo(FILE_NAME);
		assertThat(denialTextProperties.htmlBody()).isEqualTo(HTML_BODY);
		assertThat(denialTextProperties.lawArticle()).isEqualTo(LAW_ARTICLE);
		assertThat(denialTextProperties.lawChapter()).isEqualTo(LAW_CHAPTER);
		assertThat(denialTextProperties.lawHeading()).isEqualTo(LAW_HEADING);
		assertThat(denialTextProperties.lawSfs()).isEqualTo(LAW_SFS);
		assertThat(denialTextProperties.message()).isEqualTo(MESSAGE);
		assertThat(denialTextProperties.plainBody()).isEqualTo(PLAIN_BODY);
		assertThat(denialTextProperties.subject()).isEqualTo(SUBJECT);
	}
}
