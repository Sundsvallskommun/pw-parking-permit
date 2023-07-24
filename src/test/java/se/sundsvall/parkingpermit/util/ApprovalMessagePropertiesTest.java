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
class ApprovalMessagePropertiesTest {
	private static final String DESCRIPTION = "Personen är folkbokförd i Sundsvalls kommun. Rekommenderat beslut är att godkänna ansökan.";

	@Autowired
	private ApprovalMessageProperties approvalMessageProperties;

	@Test
	void toWebMessageRequest() {
		assertThat(approvalMessageProperties.description()).isEqualTo(DESCRIPTION);
	}
}
