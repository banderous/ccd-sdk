package uk.gov.hmcts.reform.fpl;


import ccd.sdk.types.CCDConfig;
import ccd.sdk.types.ConfigBuilder;
import ccd.sdk.types.DisplayContext;
import ccd.sdk.types.FieldCollection;
import uk.gov.hmcts.reform.fpl.model.*;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;

public class FPLConfig implements CCDConfig<CaseData> {

    @Override
    public void configure(ConfigBuilder<CaseData> builder) {

        builder.caseType("CARE_SUPERVISION_EPO");
        buildInitialEvents(builder);
        buildSharedEvents(builder, "Submitted", "Gatekeeping", "-PREPARE_FOR_HEARING");
        buildPrepareForHearingEvents(builder);
        buildC21Events(builder);
        buildUniversalEvents(builder);
        buildSubmittedEvents(builder);
        buildGatekeepingEvents(builder);
        buildStandardDirections(builder, "Submitted", "");
        buildStandardDirections(builder, "Gatekeeping", "AfterGatekeeping");
        buildTransitions(builder);
    }

    private void buildC21Events(ConfigBuilder<CaseData> builder) {
        builder.event("createC21Order")
                .forStates("Submitted", "Gatekeeping")
                .name("Create an order")
                .allWebhooks("create-order")
                .midEventURL("/create-order/mid-event")
                .fields()
                    .page("OrderInformation")
                    .complex(CaseData::getC21Order)
                        .optional(C21Order::getOrderTitle)
                        .mandatory(C21Order::getOrderDetails)
                        .readonly(C21Order::getDocument, "document=\"DO_NOT_SHOW\"")
                        .readonly(C21Order::getOrderDate, "document=\"DO_NOT_SHOW\"")
                        .done()
                    .page("JudgeInformation")
                    .complex(CaseData::getJudgeAndLegalAdvisor)
                        .optional(JudgeAndLegalAdvisor::getJudgeTitle)
                        .mandatory(JudgeAndLegalAdvisor::getJudgeLastName)
                        .optional(JudgeAndLegalAdvisor::getJudgeFullName)
                        .optional(JudgeAndLegalAdvisor::getLegalAdvisorName);
    }

    private void buildTransitions(ConfigBuilder<CaseData> builder) {
        builder.event("submitApplication")
                .forStateTransition("Open", "Submitted")
                .name("Submit application")
                .endButtonLabel("Submit")
                .allWebhooks("case-submission")
                .midEventURL("/case-submission/mid-event")
                .retries(1,2,3,4,5)
                .fields()
                    .label("submissionConsentLabel", "")
                    .field("submissionConsent", DisplayContext.Mandatory);

        builder.event("populateSDO")
                .forStateTransition("Submitted", "Gatekeeping")
                .name("Populate standard directions")
                .fields()
                    .pageLabel(" ")
                    .optional(CaseData::getAllParties)
                    .optional(CaseData::getLocalAuthorityDirections)
                    .optional(CaseData::getRespondentDirections)
                    .optional(CaseData::getCafcassDirections)
                    .optional(CaseData::getOtherPartiesDirections)
                    .optional(CaseData::getCourtDirections);

        builder.event("deleteApplication")
                .forStateTransition("Open", "Deleted")
                .name("Delete an application")
                .aboutToSubmitURL("/case-deletion/about-to-submit")
                .endButtonLabel("Delete application")
                .fields()
                    .field("deletionConsent", DisplayContext.Mandatory);
    }

    private void buildGatekeepingEvents(ConfigBuilder<CaseData> builder) {
        builder.event("otherAllocationDecision")
                .forState("Gatekeeping")
                .name("Allocation decision")
                .description("Entering other proceedings and allocation proposals")
                .aboutToStartURL("/allocation-decision/about-to-start")
                .aboutToSubmitURL("/allocation-decision/about-to-submit")
                .retries(1,2,3,4,5)
                .fields()
                    .field(CaseData::getAllocationDecision, DisplayContext.Mandatory, true);

        builder.event("draftSDO")
                .forState("Gatekeeping")
                .name("Draft standard directions")
                .allWebhooks("draft-standard-directions")
                .midEventURL("/draft-standard-directions/mid-event")
                .fields()
                    .page("judgeAndLegalAdvisor")
                        .optional(CaseData::getJudgeAndLegalAdvisor)
                    .page("allPartiesDirections")
                        .readonly(CaseData::getHearingDetails, "hearingDate=\"DO_NOT_SHOW\"")
                        .field().id("hearingDate").context(DisplayContext.ReadOnly).showCondition("hearingDetails.startDate=\"\"").context(DisplayContext.ReadOnly).done()
                        .field(CaseData::getAllParties)
                        .field(CaseData::getAllPartiesCustom)
                    .page("localAuthorityDirections")
                        .field(CaseData::getLocalAuthorityDirections)
                        .field(CaseData::getLocalAuthorityDirectionsCustom)
                    .page("parentsAndRespondentsDirections")
                        .field(CaseData::getRespondentDirections)
                        .field(CaseData::getRespondentDirectionsCustom)
                    .page("cafcassDirections")
                        .field(CaseData::getCafcassDirections)
                        .field(CaseData::getCafcassDirectionsCustom)
                    .page("otherPartiesDirections")
                        .field(CaseData::getOtherPartiesDirections)
                        .field(CaseData::getOtherPartiesDirectionsCustom)
                    .page("courtDirections")
                        .field(CaseData::getCourtDirections)
                        .field(CaseData::getCourtDirectionsCustom)
                    .page("documentReview")
                        .field(CaseData::getStandardDirectionOrder);
    }

    private void buildStandardDirections(ConfigBuilder<CaseData> builder, String state, String suffix) {
        builder.event("uploadStandardDirections" + suffix)
                .forState(state)
                .name("Documents")
                .description("Upload standard directions")
                .fields()
                    .label("standardDirectionsLabel", "Upload standard directions and other relevant documents, for example the C6 Notice of Proceedings or C9 statement of service.")
                    .label("standardDirectionsTitle", "## 1. Standard directions")
                    .field("standardDirectionsDocument", DisplayContext.Optional)
                    .field("otherCourtAdminDocuments", DisplayContext.Optional);
    }

    private void buildSubmittedEvents(ConfigBuilder<CaseData> builder) {
        builder.event("addFamilyManCaseNumber")
                .forState("Submitted")
                .name("Add case number")
                .fields()
                    .optional(CaseData::getFamilyManCaseNumber);

        builder.event("addStatementOfService")
                .forState("Submitted")
                .name("Add statement of service (c9)")
                .description("Add statement of service")
                .aboutToStartURL("/statement-of-service/about-to-start")
                .fields()
                    .label("c9Declaration", "If you send documents to a party's solicitor or a children's guardian, give their details") .field(CaseData::getStatementOfService, DisplayContext.Mandatory, true) .label("serviceDeclarationLabel", "Declaration" ) .field("serviceConsent", DisplayContext.Mandatory);


        builder.event("uploadDocumentsAfterSubmission")
                .forState("Submitted")
                .name("Documents")
                .description("Only here for backwards compatibility with case history");
    }

    private void buildUniversalEvents(ConfigBuilder<CaseData> builder) {
        builder.event("uploadDocuments")
                .forAllStates()
                .name("Documents")
                .description("Upload documents")
                .midEventURL("/upload-documents/mid-event")
                .fields()
                    .label("uploadDocuments_paragraph_1", "You must upload these documents if possible. Give the reason and date you expect to provide it if you don’t have a document yet.")
                    .optional(CaseData::getSocialWorkChronologyDocument)
                    .optional(CaseData::getSocialWorkStatementDocument)
                    .optional(CaseData::getSocialWorkAssessmentDocument)
                    .optional(CaseData::getSocialWorkCarePlanDocument)
                    .optional(CaseData::getSocialWorkEvidenceTemplateDocument)
                    .optional(CaseData::getThresholdDocument)
                    .optional(CaseData::getChecklistDocument)
                    .field("[STATE]", DisplayContext.ReadOnly, "courtBundle = \"DO_NOT_SHOW\"")
                    .field("courtBundle", DisplayContext.Optional, "[STATE] != \"Open\"")
                    .label("documents_socialWorkOther_border_top", "---------------------------------")
                    .optional(CaseData::getOtherSocialWorkDocuments)
                    .label("documents_socialWorkOther_border_bottom", "---------------------------------");
    }

    private void buildPrepareForHearingEvents(ConfigBuilder<CaseData> builder) {
        builder.event("uploadOtherCourtAdminDocuments-PREPARE_FOR_HEARING")
                .forState("PREPARE_FOR_HEARING")
                .fields()
                    .field("otherCourtAdminDocuments", DisplayContext.Optional);

        builder.event("draftCMO")
                .forState("PREPARE_FOR_HEARING")
                .name("Draft CMO")
                .description("Draft Case Management Order")
                .fields()
                .page("hearingDate")
                    .field("cmoHearingDateList", DisplayContext.Mandatory)
                .page("allPartiesDirections")
                    .label("allPartiesLabelCMO", "## For all parties")
                    .label("allPartiesPrecedentLabelCMO", "Add completed directions from the precedent library or your own template.")
                    .complex(CaseData::getAllPartiesCustom, Direction.class, this::renderDirection)
                .page("localAuthorityDirections")
                     .label("localAuthorityDirectionsLabelCMO", "## For the local authority")
                     .complex(CaseData::getLocalAuthorityDirectionsCustom, Direction.class, this::renderDirection)
                .page(2)
                     .label("respondentsDirectionLabelCMO", "For the parents or respondents")
                     .label("respondentsDropdownLabelCMO", " ")
                     .complex(CaseData::getRespondentDirectionsCustom, Direction.class)
                        .optional(Direction::getDirectionType)
                        .mandatory(Direction::getDirectionText)
                        .mandatory(Direction::getParentsAndRespondentsAssignee)
                        .optional(Direction::getDateToBeCompletedBy)
                    .done()
                .page("cafcassDirections")
                     .label("cafcassDirectionsLabelCMO", "## For Cafcass")
                     .complex(CaseData::getCafcassDirectionsCustom, Direction.class, this::renderDirection)
                    .complex(CaseData::getCaseManagementOrder)
                        .readonly(CaseManagementOrder::getHearingDate)
                    .done()
                .page(3)
                     .label("otherPartiesDirectionLabelCMO", "## For other parties")
                     .label("otherPartiesDropdownLabelCMO", " ")
                     .complex(CaseData::getOtherPartiesDirectionsCustom, Direction.class)
                        .optional(Direction::getDirectionType)
                        .mandatory(Direction::getDirectionText)
                        .mandatory(Direction::getOtherPartiesAssignee)
                        .optional(Direction::getDateToBeCompletedBy)
                    .done()
                .page("courtDirections")
                     .label("courtDirectionsLabelCMO", "## For the court")
                     .complex(CaseData::getCourtDirectionsCustom, Direction.class, this::renderDirection)
                .page(5)
                     .label("orderBasisLabel", "## Basis of order")
                     .label("addRecitalLabel", "## Add recital")
                     .field("recitals", DisplayContext.Optional)
                .page("schedule")
                     .field("schedule", DisplayContext.Mandatory);

        builder.event("COMPLY_LOCAL_AUTHORITY")
                .forState("PREPARE_FOR_HEARING")
                .name("Comply with directions")
                .description("Allows Local Authority user access to comply with their directions as well as ones for all parties")
                .fields()
                .pageLabel(" ")
                    .field(CaseData::getLocalAuthorityDirections);

        builder.event("COMPLY_CAFCASS")
                .forState("PREPARE_FOR_HEARING")
                .name("Comply with directions")
                .description("Allows Cafcass user access to comply with their directions as well as ones for all parties")
                .fields()
                .pageLabel(" ")
                    .field(CaseData::getCafcassDirections);

        // courtDirectionsCustom is used here to prevent Gatekeeper inadvertently getting C and D permissions in draftSDO journey on courtDirections. We do not want them to able to create/delete directions"
        builder.event("COMPLY_COURT")
                .forState("PREPARE_FOR_HEARING")
                .name("Comply with directions")
                .description("Event gives Court user access to comply with their directions as well as all parties")
                .fields()
                .pageLabel(" ")
                    .field(CaseData::getCourtDirectionsCustom);

    }

    private void renderDirection(FieldCollection.FieldCollectionBuilder<Direction, ?> builder) {
        builder.optional(Direction::getDirectionType)
                .mandatory(Direction::getDirectionText)
                .optional(Direction::getDateToBeCompletedBy);
    }

    private void buildSharedEvents(ConfigBuilder<CaseData> builder, String... states) {
        builder.event("hearingBookingDetails")
                .forStates(states)
                .name("Add hearing details")
                .description("Add hearing booking details to a case")
                .midEventURL("/add-hearing-bookings/mid-event")
                .fields()
                .complex(CaseData::getHearingDetails, HearingBooking.class)
                    .mandatory(HearingBooking::getType)
                    .mandatory(HearingBooking::getTypeDetails, "hearingDetails.type=\"OTHER\"")
                    .mandatory(HearingBooking::getVenue)
                    .mandatory(HearingBooking::getStartDate)
                    .mandatory(HearingBooking::getEndDate)
                    .mandatory(HearingBooking::getHearingNeedsBooked)
                    .mandatory(HearingBooking::getHearingNeedsDetails, "hearingDetails.hearingNeedsBooked!=\"NONE\"")
                    .complex(HearingBooking::getJudgeAndLegalAdvisor)
                        .mandatory(JudgeAndLegalAdvisor::getJudgeTitle)
                        .mandatory(JudgeAndLegalAdvisor::getOtherTitle, "hearingDetails.judgeAndLegalAdvisor.judgeTitle=\"OTHER\"")
                        .mandatory(JudgeAndLegalAdvisor::getJudgeLastName, "hearingDetails.judgeAndLegalAdvisor.judgeTitle!=\"MAGISTRATES\" AND hearingDetails.judgeAndLegalAdvisor.judgeTitle!=\"\"")
                        .optional(JudgeAndLegalAdvisor::getJudgeFullName, "hearingDetails.judgeAndLegalAdvisor.judgeTitle=\"MAGISTRATES\"")
                        .optional(JudgeAndLegalAdvisor::getLegalAdvisorName);

        builder.event("uploadC2")
                .forStates(states)
                .name("Upload a C2")
                .description("Upload a c2 to the case")
                .midEventURL("/upload-c2/mid-event")
                .fields()
                    .complex(CaseData::getTemporaryC2Document)
                        .mandatory(C2DocumentBundle::getDocument)
                        .mandatory(C2DocumentBundle::getDescription);
        builder.event("sendToGatekeeper")
                .forStates(states)
                .name("Send to gatekeeper")
                .description("Send email to gatekeeper")
                    .fields()
                    .label("gateKeeperLabel", "Let the gatekeeper know there's a new case")
                    .mandatory(CaseData::getGatekeeperEmail);
        builder.event("amendChildren")
                .forStates(states)
                .name("Children")
                .description("Amending the children for the case")
                .fields()
                    .optional(CaseData::getChildren1);
        builder.event("amendRespondents")
                .forStates(states)
                .name("Respondents")
                .description("Amending the respondents for the case")
                .fields()
                    .optional(CaseData::getRespondents1);
        builder.event("amendOthers")
                .forStates(states)
                .name("Others to be given notice")
                .description("Amending others for the case")
                .fields()
                    .optional(CaseData::getOthers);
        builder.event("amendInternationalElement")
                .forStates(states)
                .name("International element")
                .description("Amending the international element")
                .fields()
                    .optional(CaseData::getInternationalElement);
        builder.event("amendOtherProceedings")
                .forStates(states)
                .name("Other proceedings")
                .description("Amending other proceedings and allocation proposals")
                .midEventURL("/enter-other-proceedings/mid-event")
                .fields()
                    .optional(CaseData::getProceeding);
        builder.event("amendAttendingHearing")
                .forStates(states)
                .name("Attending the hearing")
                .description("Amend extra support needed for anyone to take part in hearing")
                .fields()
                    .optional(CaseData::getHearingPreferences);
        builder.event("createNoticeOfProceedings")
                .forStates(states)
                .name("Create notice of proceedings")
                .fields()
                    .label("proceedingLabel", "## Other proceedings 1")
                    .complex(CaseData::getNoticeOfProceedings)
                        .complex(NoticeOfProceedings::getJudgeAndLegalAdvisor)
                            .optional(JudgeAndLegalAdvisor::getJudgeTitle)
                            .mandatory(JudgeAndLegalAdvisor::getJudgeLastName)
                            .optional(JudgeAndLegalAdvisor::getJudgeFullName)
                            .optional(JudgeAndLegalAdvisor::getLegalAdvisorName)
                        .done()
                        .mandatory(NoticeOfProceedings::getProceedingTypes);
    }

    private void buildInitialEvents(ConfigBuilder<CaseData> builder) {
        builder.event("openCase")
                .initialState("Open")
                .name("Start application")
                .description("Create a new case – add a title")
                .aboutToSubmitURL("/case-initiation/about-to-submit")
                .submittedURL("/case-initiation/submitted")
                .retries(1,2,3,4,5)
                .fields()
                    .optional(CaseData::getCaseName);

        builder.event("ordersNeeded").forState("Open")
                .name("Orders and directions needed")
                .description("Selecting the orders needed for application")
                .aboutToSubmitURL("/orders-needed/about-to-submit")
                .fields()
                    .optional(CaseData::getOrders);

        builder.event("hearingNeeded").forState("Open")
                .name("Hearing needed")
                .description("Selecting the hearing needed for application")
                .fields()
                    .optional(CaseData::getHearing);

        builder.event("enterChildren").forState("Open")
                .name("Children")
                .description("Entering the children for the case")
                .aboutToStartURL("/enter-children/about-to-start")
                .aboutToSubmitURL("/enter-children/about-to-submit")
                .fields()
                    .optional(CaseData::getChildren1);

        builder.event("enterRespondents").forState("Open")
                .name("Respondents")
                .description("Entering the respondents for the case")
                .aboutToStartWebhook()
                .aboutToSubmitWebhook()
                .midEventWebhook()
                .fields()
                    .optional(CaseData::getRespondents1);

        builder.event("enterApplicant").forState("Open")
                .name("Applicant")
                .description("Entering the applicant for the case")
                .aboutToStartWebhook()
                .aboutToSubmitWebhook()
                .midEventWebhook()
                .fields()
                    .optional(CaseData::getApplicants)
                    .optional(CaseData::getSolicitor);

        builder.event("enterOthers").forState("Open")
                .name("Others to be given notice")
                .description("Entering others for the case")
                .fields()
                    .optional(CaseData::getOthers);

        builder.event("enterGrounds").forState("Open")
                .name("Grounds for the application")
                .description("Entering the grounds for the application")
                .fields()
                    .field().id("EPO_REASONING_SHOW").context(DisplayContext.Optional).showCondition("groundsForEPO CONTAINS \"Workaround to show groundsForEPO. Needs to be hidden from UI\"").done()
                    .optional(CaseData::getGroundsForEPO, "EPO_REASONING_SHOW CONTAINS \"SHOW_FIELD\"")
                    .optional(CaseData::getGrounds);

        builder.event("enterRiskHarm").forState("Open")
                .name("Risk and harm to children")
                .description("Entering opinion on risk and harm to children")
                .fields()
                    .optional(CaseData::getRisks);

        builder.event("enterParentingFactors").forState("Open")
                .name("Factors affecting parenting")
                .description("Entering the factors affecting parenting")
                .grant("caseworker-publiclaw-solicitor", "CRU")
                .fields()
                    .optional(CaseData::getFactorsParenting);

        builder.event("enterInternationalElement").forState("Open")
                .name("International element")
                .description("Entering the international element")
                .fields()
                    .optional(CaseData::getInternationalElement);

        builder.event("otherProceedings").forState("Open")
                .name("Other proceedings")
                .description("Entering other proceedings and proposals")
                .midEventURL("/enter-other-proceedings/mid-event")
                .fields()
                    .optional(CaseData::getProceeding);

        builder.event("otherProposal").forState("Open")
                .name("Allocation proposal")
                .description("Entering other proceedings and allocation proposals")
                .fields()
                    .label("allocationProposal_label", "This should be completed by a solicitor with good knowledge of the case. Use the [President's Guidance](https://www.judiciary.uk/wp-content/uploads/2013/03/President%E2%80%99s-Guidance-on-Allocation-and-Gatekeeping.pdf) and [schedule](https://www.judiciary.uk/wp-content/uploads/2013/03/Schedule-to-the-President%E2%80%99s-Guidance-on-Allocation-and-Gatekeeping.pdf) on allocation and gatekeeping to make your recommendation.")
                    .optional(CaseData::getAllocationProposal);

        builder.event("attendingHearing").forState("Open")
                .name("Attending the hearing")
                .description("Enter extra support needed for anyone to take part in hearing")
                .displayOrder(13)
                .fields()
                    .optional(CaseData::getHearingPreferences);

        builder.event("changeCaseName").forState("Open")
                .name("Change case name")
                .description("Change case name")
                .displayOrder(15)
                .fields()
                    .optional(CaseData::getCaseName);

        builder.event("addCaseIDReference").forState("Open")
                .name("Add case ID")
                .description("Add case ID")
                .displayOrder(16)
                .fields()
                    .pageLabel("Add Case ID")
                    .field("caseIDReference", DisplayContext.Optional);
    }
}
