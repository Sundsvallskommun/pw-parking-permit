package se.sundsvall.parkingpermit.businesslogic.worker;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.parkingpermit.Application;
import se.sundsvall.parkingpermit.integration.engine.externaltask.OperatonExternalTaskToken;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class AbstractTaskWorkerWiringTest {

	@Autowired
	private List<AbstractTaskWorker> workers;

	@Autowired
	private OperatonExternalTaskToken token;

	@Test
	void everyWorkerIsBuiltWithTheExternalTaskToken() {
		// Without the token a 401 on complete/handleFailure cannot be retried, see AbstractTaskWorkerTest
		assertThat(workers)
			.hasSize(20)
			.allSatisfy(worker -> assertThat(worker).extracting("token").isSameAs(token));
	}
}
