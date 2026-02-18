package se.sundsvall.parkingpermit.integration.camunda.deployment;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.assertj.core.api.FileAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import se.sundsvall.parkingpermit.integration.camunda.CamundaClient;
import se.sundsvall.parkingpermit.integration.camunda.deployment.DeploymentProperties.ProcessArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAwareAutoDeploymentTest {

	private static final String PROCESSMODEL_PATH = "processmodels/";
	private static final String PROCESSMODEL_FILE = "process-parking-permit.bpmn";

	private static final String DEFAULT_PATTERN_PREFIX = "classpath*:**/*.";
	private static final String FILETYPE_BPMN = "bpmn";
	private static final String FILETYPE_DMN = "dmn";
	private static final String FILETYPE_FORM = "form";

	@Mock
	private CamundaClient deploymentApiMock;

	@Mock
	private DeploymentProperties deploymentPropertiesMock;

	@Mock
	private ProcessArchive processArchiveMock;

	@Mock
	private ResourcePatternResolver resourcePatternResolverMock;

	@Mock
	private Resource resourceMock;

	@InjectMocks
	private TenantAwareAutoDeployment tenantAwareAutoDeployment;

	@Captor
	private ArgumentCaptor<File> fileCaptor;

	@Test
	void autoDeployDisabled() {

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(false);

		tenantAwareAutoDeployment.deployCamundaResources();

		verify(deploymentPropertiesMock, atLeastOnce()).isAutoDeployEnabled();
		verifyNoInteractions(deploymentApiMock, resourcePatternResolverMock);
	}

	@Test
	void autoDeployEnabledButNoDefinedProcesses() {
		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);

		tenantAwareAutoDeployment.deployCamundaResources();

		verify(deploymentPropertiesMock, atLeastOnce()).isAutoDeployEnabled();
		verifyNoInteractions(deploymentApiMock, resourcePatternResolverMock);
	}

	@Test
	void autoDeployEnabledWithDefinedProcessAndDefaultPathPatterns() throws IOException {
		final var name = "name";

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));
		when(processArchiveMock.name()).thenReturn(name);

		tenantAwareAutoDeployment.deployCamundaResources();

		verify(deploymentPropertiesMock, atLeastOnce()).isAutoDeployEnabled();
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN);
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_DMN);
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_FORM);
		verifyNoMoreInteractions(resourcePatternResolverMock);
		verifyNoInteractions(deploymentApiMock);
	}

	@Test
	void autoDeployEnabledWithDefinedProcessAndDefaultPathPatternsButNoNameSet() {
		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));

		final var exception = assertThrows(IllegalArgumentException.class, () -> tenantAwareAutoDeployment.deployCamundaResources());

		verifyNoInteractions(deploymentApiMock);
		assertThat(exception.getMessage()).isEqualTo("Processname must be set");
	}

	@Test
	void autoDeployEnabledWithDefinedProcessAndCustomPathPatterns() throws IOException {
		final var name = "name";
		final var custom_pattern_bpmn = "classpath*:**/custompath/*.custom_bpmn";
		final var custom_pattern_dmn = "classpath*:**/custompath/*..custom_dmn";
		final var custom_pattern_form = "classpath*:**/custompath/*..custom_form";

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));
		when(processArchiveMock.name()).thenReturn(name);
		when(processArchiveMock.bpmnResourcePattern()).thenReturn(custom_pattern_bpmn);
		when(processArchiveMock.dmnResourcePattern()).thenReturn(custom_pattern_dmn);
		when(processArchiveMock.formResourcePattern()).thenReturn(custom_pattern_form);

		tenantAwareAutoDeployment.deployCamundaResources();

		verify(deploymentPropertiesMock, atLeastOnce()).isAutoDeployEnabled();
		verify(resourcePatternResolverMock).getResources(custom_pattern_bpmn);
		verify(resourcePatternResolverMock).getResources(custom_pattern_dmn);
		verify(resourcePatternResolverMock).getResources(custom_pattern_form);
		verifyNoMoreInteractions(resourcePatternResolverMock);
		verifyNoInteractions(deploymentApiMock);
	}

	@Test
	void autoDeployEnabledWithDefinedProcessAndMatchingDeploymentResources() throws IOException {
		final var name = "name";
		final var tenant = "tenant";
		final var deploymentName = name + " (" + tenant + ") - " + PROCESSMODEL_FILE;
		final var processFile = new ClassPathResource(PROCESSMODEL_PATH + PROCESSMODEL_FILE);

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));
		when(processArchiveMock.tenant()).thenReturn(tenant);
		when(processArchiveMock.name()).thenReturn(name);
		when(resourcePatternResolverMock.getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN)).thenReturn(new Resource[] {
			resourceMock
		});
		when(resourceMock.getFilename()).thenReturn(PROCESSMODEL_FILE);
		when(resourceMock.getInputStream()).thenReturn(processFile.getInputStream());

		tenantAwareAutoDeployment.deployCamundaResources();

		verify(deploymentPropertiesMock, atLeastOnce()).isAutoDeployEnabled();
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN);
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_DMN);
		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_FORM);
		verify(deploymentApiMock).deploy(eq(tenant), eq(PROCESSMODEL_FILE), eq(true), eq(true), eq(deploymentName), isNull(), fileCaptor.capture());

		new FileAssert(fileCaptor.getValue()).hasSameTextualContentAs(processFile.getFile());
	}

	@Test
	void patternResolverThrowsException() throws IOException {
		final var originException = new IOException("testException");

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));
		when(resourcePatternResolverMock.getResources(any())).thenThrow(originException);

		final var exception = assertThrows(DeploymentException.class, () -> tenantAwareAutoDeployment.deployCamundaResources());

		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN);
		verifyNoMoreInteractions(resourcePatternResolverMock);
		verifyNoInteractions(deploymentApiMock);
		assertThat(exception.getCause()).isEqualTo(originException);
	}

	@Test
	void createDeploymentThrowsException() throws IOException {
		final var name = "name";
		final var originException = new RuntimeException("testException");

		when(deploymentPropertiesMock.isAutoDeployEnabled()).thenReturn(true);
		when(deploymentPropertiesMock.getProcesses()).thenReturn(List.of(processArchiveMock));
		when(processArchiveMock.name()).thenReturn(name);
		when(deploymentApiMock.deploy(any(), any(), anyBoolean(), anyBoolean(), any(), any(), any())).thenThrow(originException);
		when(resourcePatternResolverMock.getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN)).thenReturn(new Resource[] {
			resourceMock
		});
		when(resourceMock.getFilename()).thenReturn(PROCESSMODEL_FILE);
		when(resourceMock.getInputStream()).thenReturn(new ClassPathResource(PROCESSMODEL_PATH + PROCESSMODEL_FILE).getInputStream());

		final var exception = assertThrows(DeploymentException.class, () -> tenantAwareAutoDeployment.deployCamundaResources());

		verify(resourcePatternResolverMock).getResources(DEFAULT_PATTERN_PREFIX + FILETYPE_BPMN);
		verify(deploymentApiMock).deploy(any(), any(), anyBoolean(), anyBoolean(), any(), any(), any());
		verifyNoMoreInteractions(resourcePatternResolverMock, deploymentApiMock);
		assertThat(exception.getCause()).isEqualTo(originException);
	}
}
