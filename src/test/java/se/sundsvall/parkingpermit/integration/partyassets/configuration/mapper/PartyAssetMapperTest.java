package se.sundsvall.parkingpermit.integration.partyassets.configuration.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import generated.se.sundsvall.partyassets.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.integration.partyassets.mapper.PartyAssetMapper;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("junit")
class PartyAssetMapperTest {

	@Test
	void toAssetUpdateRequest() {
		final var asset = new Asset();
		final var caseNumber = 123L;

		final var request = PartyAssetMapper.toAssetUpdateRequest(asset, caseNumber);

		assertThat(request).isNotNull();
		assertThat(request.getAdditionalParameters()).hasSize(1);
		assertThat(request.getAdditionalParameters().get("appealedErrand")).isEqualTo(String.valueOf(caseNumber));
	}

	@Test
	void toAssetUpdateRequestWhenAssetIsNull() {
		final var caseNumber = 123L;

		final var request = PartyAssetMapper.toAssetUpdateRequest(null, caseNumber);

		assertThat(request).isNull();
	}

	@Test
	void toAssetUpdateRequestWhenCaseNumberIsNull() {
		final var asset = new Asset();

		final var request = PartyAssetMapper.toAssetUpdateRequest(asset, null);

		assertThat(request).isNull();
	}
}
