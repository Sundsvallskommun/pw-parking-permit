package se.sundsvall.parkingpermit.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	@Autowired
	private ApprovalTextProperties approvalTexts;

	@Autowired
	private DenialTextProperties denialTexts;

	@Autowired
	private CommonTextProperties commonTexts;

	public ApprovalTextProperties getApprovalTexts() {
		return approvalTexts;
	}

	public DenialTextProperties getDenialTexts() {
		return denialTexts;
	}

	public CommonTextProperties getCommonTexts() {
		return commonTexts;
	}
}
