package uk.gov.hmcts.ccd.sdk.types;

public interface EventTypeBuilder<T, R extends Role, S> {
    Event.EventBuilder<T, R, S> forState(S state);
    Event.EventBuilder<T, R, S> initialState(S state);
    Event.EventBuilder<T, R, S> forStateTransition(S from, S to);
    Event.EventBuilder<T, R, S> forAllStates();
    Event.EventBuilder<T, R, S> forStates(S... states);
}
