package se.sundsvall.parkingpermit.integration.rpa.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class RpaPropertiesTest {

	@Autowired
	private RpaProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(10);
		assertThat(properties.readTimeout()).isEqualTo(20);
		assertThat(properties.folderIds()).containsEntry("2281", "50").containsEntry("2260", "60");
		assertThat(properties.identityServerUrl()).isEqualTo("https://rpa.base.url/identity");
	}
}
