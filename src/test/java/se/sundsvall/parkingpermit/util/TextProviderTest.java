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
class TextProviderTest {

	@Autowired
	private TextProvider textProvider;

	@Test
	void checkCorrectAutowiring() {
		assertThat(textProvider.getApprovalTexts()).isNotNull();
		assertThat(textProvider.getCommonTexts()).isNotNull();
		assertThat(textProvider.getDenialTexts()).isNotNull();
	}
}
