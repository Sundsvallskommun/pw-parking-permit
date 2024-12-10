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
class CommonTextPropertiesTest {
	private static final String DEPARTMENT = "SBK(Gatuavdelningen, Trafiksektionen)";
	private static final String CONTACTINFO_EMAIL = "sundsvalls.kommun@sundsvall.se";
	private static final String CONTACTINFO_PHONENUMBER = "+46 60 191000";
	private static final String CONTACTINFO_TEXT = "Kontakta oss via epost eller telefon.";
	private static final String CONTACTINFO_URL = "https://sundsvall.se/";

	@Autowired
	private CommonTextProperties commonTextProperties;

	@Test
	void toWebMessageRequest() {
		assertThat(commonTextProperties.department()).isEqualTo(DEPARTMENT);
		assertThat(commonTextProperties.contactInfoEmail()).isEqualTo(CONTACTINFO_EMAIL);
		assertThat(commonTextProperties.contactInfoPhonenumber()).isEqualTo(CONTACTINFO_PHONENUMBER);
		assertThat(commonTextProperties.contactInfoText()).isEqualTo(CONTACTINFO_TEXT);
		assertThat(commonTextProperties.contactInfoUrl()).isEqualTo(CONTACTINFO_URL);
	}
}
