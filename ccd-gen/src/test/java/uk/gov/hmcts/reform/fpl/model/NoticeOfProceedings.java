package uk.gov.hmcts.reform.fpl.model;

import ccd.sdk.types.CaseField;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.fpl.enums.ProceedingType;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeOfProceedings {
    @CaseField(label = "What would you like to create?")
    private final List<ProceedingType> proceedingTypes;
    private final JudgeAndLegalAdvisor judgeAndLegalAdvisor;
}
