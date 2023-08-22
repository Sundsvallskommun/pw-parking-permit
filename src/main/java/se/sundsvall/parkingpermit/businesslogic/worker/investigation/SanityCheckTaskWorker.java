package se.sundsvall.parkingpermit.businesslogic.worker.investigation;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Component;
import se.sundsvall.parkingpermit.businesslogic.worker.AbstractTaskWorker;

import java.util.Map;

@Component
@ExternalTaskSubscription("InvestigationSanityCheckTask")
public class SanityCheckTaskWorker extends AbstractTaskWorker {
	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for SanityCheckTaskWorker");
			//TODO
			// 1. Set updateAvailable to false -> clearUpdateAvailable(externalTask);
			// 2. fetch errand -> getErrand(externalTask)
			// 3. Run sanity checks
			// 4. If passed set "sanityCheckPassed" to true, else set to false and update errand with missing parameters
			clearUpdateAvailable(externalTask);
			externalTaskService.complete(externalTask, Map.of("sanityCheckPassed", true));
		} catch (Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}
