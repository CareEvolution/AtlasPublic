package org.ohdsi.webapi.unittests.cohortdefinition;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.cohortdefinition.CohortGenerationInfo;
import org.ohdsi.webapi.common.SourceMapKey;
import org.ohdsi.webapi.common.sensitiveinfo.CohortGenerationSensitiveInfoService;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CohortDefinitionServiceTests {

    @Mock
    private CohortDefinitionRepository cohortDefinitionRepository;

    @Mock
    private SourceService sourceService;

    @Mock
    private AuthorizationManager authorizationManager;

    @Mock
    private CohortGenerationSensitiveInfoService sensitiveInfoService;

    @InjectMocks
    private CohortDefinitionService cohortDefinitionService;

    private CohortDefinitionEntity cohortDefinition;
    private Set<CohortGenerationInfo> generationInfoList;
    private Map<Integer, SourceEntity> sourceMap;

    @BeforeEach
    void setUp() {
        // Create a cohort definition with generation info
        cohortDefinition = new CohortDefinitionEntity();
        cohortDefinition.setId(1);
        cohortDefinition.setName("Test Cohort");

        // Create generation info for different sources
        var genInfo1 = new CohortGenerationInfo(cohortDefinition, 100);
        var genInfo2 = new CohortGenerationInfo(cohortDefinition, 200);

        generationInfoList = new HashSet<>();
        generationInfoList.add(genInfo1);
        generationInfoList.add(genInfo2);

        cohortDefinition.setGenerationInfoList(generationInfoList);

        // Create source entities
        var source1 = new SourceEntity();
        source1.setSourceId(100);
        source1.setSourceKey("source1");

        var source2 = new SourceEntity();
        source2.setSourceId(200);
        source2.setSourceKey("source2");

        sourceMap = new HashMap<>();
        sourceMap.put(100, source1);
        sourceMap.put(200, source2);
    }

    @Test
    void getInfo_WithValidId_ReturnsFilteredGenerationInfo() {
        // Arrange
        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(authorizationManager.currentUserCanRead("source1")).thenReturn(true);
        when(authorizationManager.currentUserCanRead("source2")).thenReturn(true);
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).hasSize(2);
        verify(cohortDefinitionRepository).findById(1);
        verify(sourceService).getSourcesMap(SourceMapKey.BY_SOURCE_ID);
        verify(authorizationManager, times(2)).currentUserCanRead(anyString());
        verify(sensitiveInfoService).filterSensitiveInfo(anyList(), any());
    }

    @Test
    void getInfo_WhenUserCannotReadSource_FiltersOutThatSource() {
        // Arrange
        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(authorizationManager.currentUserCanRead("source1")).thenReturn(true);
        when(authorizationManager.currentUserCanRead("source2")).thenReturn(false); // User can't read source2
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().getSourceId()).isEqualTo(100);
        verify(authorizationManager).currentUserCanRead("source1");
        verify(authorizationManager).currentUserCanRead("source2");
    }

    @Test
    void getInfo_WhenUserCannotReadAnySource_ReturnsEmptyList() {
        // Arrange
        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(authorizationManager.currentUserCanRead(anyString())).thenReturn(false);
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).isEmpty();
        verify(authorizationManager, times(2)).currentUserCanRead(anyString());
    }

    @Test
    void getInfo_WithInvalidId_ThrowsNotFoundException() {
        // Arrange
        when(cohortDefinitionRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cohortDefinitionService.getInfo(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("There is no cohort definition with id = 999");

        verify(cohortDefinitionRepository).findById(999);
        verify(sourceService, never()).getSourcesMap(any());
    }

    @Test
    void getInfo_WithNoGenerationInfo_ReturnsEmptyList() {
        // Arrange
        cohortDefinition.setGenerationInfoList(new HashSet<>());
        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).isEmpty();
        verify(cohortDefinitionRepository).findById(1);
        verify(sensitiveInfoService).filterSensitiveInfo(anyList(), any());
    }

    @Test
    void getInfo_CallsSensitiveInfoService_WithCorrectParameters() {
        // Arrange
        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(authorizationManager.currentUserCanRead(anyString())).thenReturn(true);
        
        List<CohortGenerationInfo> filteredList = new ArrayList<>(generationInfoList);
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenReturn(filteredList);

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).hasSize(2);
        verify(sensitiveInfoService).filterSensitiveInfo(anyList(), any());
    }

    @Test
    void getInfo_WhenGenerationReferencesNonExistentSource_SkipsThatGeneration() {
        // Arrange
        var genInfo3 = new CohortGenerationInfo(cohortDefinition, 300); // SourceId 300 does not exist in sourceMap
        generationInfoList.add(genInfo3);
        cohortDefinition.setGenerationInfoList(generationInfoList);

        when(cohortDefinitionRepository.findById(1)).thenReturn(Optional.of(cohortDefinition));
        when(sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID)).thenReturn(sourceMap);
        when(authorizationManager.currentUserCanRead("source1")).thenReturn(true);
        when(authorizationManager.currentUserCanRead("source2")).thenReturn(true);
        when(sensitiveInfoService.filterSensitiveInfo(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = cohortDefinitionService.getInfo(1);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(info -> info.getId().getSourceId()).doesNotContain(300);
    }
}
