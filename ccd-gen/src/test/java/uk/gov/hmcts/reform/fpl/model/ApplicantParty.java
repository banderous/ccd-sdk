package uk.gov.hmcts.reform.fpl.model;

import uk.gov.hmcts.ccd.sdk.types.CCD;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.reform.fpl.enums.PartyType;
import uk.gov.hmcts.reform.fpl.model.common.EmailAddress;
import uk.gov.hmcts.reform.fpl.model.common.Party;
import uk.gov.hmcts.reform.fpl.model.common.Telephone;
import uk.gov.hmcts.reform.fpl.model.interfaces.TelephoneContacts;
import uk.gov.hmcts.reform.fpl.validation.interfaces.HasContactDirection;
import uk.gov.hmcts.reform.fpl.validation.interfaces.HasTelephoneOrMobile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@HasTelephoneOrMobile
@HasContactDirection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicantParty extends Party implements TelephoneContacts {
    @NotBlank(message = "Enter the applicant's full name")
    private final String organisationName;
    private final Telephone mobileNumber;
    @NotBlank(message = "Enter a job title for the contact")
    private final String jobTitle;
    private final String pbaNumber;
    @NotNull(message = "Enter a valid address for the contact")
    @Valid
    private final Address address;
    @NotNull(message = "Enter an email address for the contact")
    @Valid
    public final EmailAddress email;

    @CCD(label = "Telephone")
    private final Telephone telephoneNumber;

    @Builder(toBuilder = true)
    private ApplicantParty(String partyId,
                           PartyType partyType,
                           String organisationName,
                           Address address,
                           EmailAddress email,
                           Telephone telephoneNumber,
                           Telephone mobileNumber,
                           String jobTitle,
                           String pbaNumber) {
        super(partyId, partyType, address);

        this.organisationName = organisationName;
        this.mobileNumber = mobileNumber;
        this.jobTitle = jobTitle;
        this.pbaNumber = pbaNumber;
        this.address = address;
        this.email = email;
        this.telephoneNumber = telephoneNumber;
    }
}
