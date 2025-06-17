package se.sundsvall.parkingpermit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final TextProperties textProperties;

	TextProvider(final TextProperties textProperties) {
		this.textProperties = textProperties;
	}

	public ApprovalTextProperties getApprovalTexts(String municipalityId) {
		return textProperties.getApprovals().get(municipalityId);
	}

	public CommonTextProperties getCommonTexts(String municipalityId) {
		return textProperties.getCommons().get(municipalityId);
	}

	public DenialTextProperties getDenialTexts(String municipalityId) {
		return textProperties.getDenials().get(municipalityId);
	}

	public SimplifiedServiceTextProperties getSimplifiedServiceTexts(String municipalityId) {
		return textProperties.getSimplifiedServices().get(municipalityId);
	}
}
