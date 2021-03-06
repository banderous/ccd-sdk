package uk.gov.hmcts.reform.fpl.model;

import uk.gov.hmcts.ccd.sdk.types.CCD;
import uk.gov.hmcts.ccd.sdk.types.ComplexType;
import uk.gov.hmcts.ccd.sdk.types.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.validation.groups.HearingBookingDetailsGroup;
import uk.gov.hmcts.reform.fpl.validation.interfaces.time.HasEndDateAfterStartDate;
import uk.gov.hmcts.reform.fpl.validation.interfaces.time.TimeNotMidnight;

import javax.validation.constraints.Future;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@HasEndDateAfterStartDate(groups = HearingBookingDetailsGroup.class)
@ComplexType
public class HearingBooking {
    @CCD(label = "Type of hearing", type = FieldType.FixedRadioList, typeParameter = "HearingType")
    private final String type;
    @CCD(type = FieldType.TextArea, label = "Give details", showCondition = "type=\"OTHER\"")
    private final String typeDetails;
    @CCD(label = "Venue", typeParameter = "HearingVenue")
    private final String venue;
    @TimeNotMidnight(message = "Enter a valid start time", groups = HearingBookingDetailsGroup.class)
    @Future(message = "Enter a start date in the future", groups = HearingBookingDetailsGroup.class)
    @CCD(label = "Start date and time", hint = "Use 24 hour format")
    private final LocalDateTime startDate;
    @TimeNotMidnight(message = "Enter a valid end time", groups = HearingBookingDetailsGroup.class)
    @Future(message = "Enter an end date in the future", groups = HearingBookingDetailsGroup.class)
    @CCD(label = "End date and time", hint = "Use 24 hour format")
    private final LocalDateTime endDate;
    @CCD(label = "Hearing needs booked", typeParameter = "HearingNeedsBooked", showCondition = "hearingNeedsBooked!=\"NONE\"")
    private final List<String> hearingNeedsBooked;
    @CCD(type = FieldType.TextArea, label = "Give details", showCondition = "hearingNeedsBooked!=\"NONE\"")
    private final String hearingNeedsDetails;
    @CCD(label = "Judge and legal advisor")
    private final JudgeAndLegalAdvisor judgeAndLegalAdvisor;

    public boolean hasDatesOnSameDay() {
        return this.startDate.toLocalDate().isEqual(this.endDate.toLocalDate());
    }
}
