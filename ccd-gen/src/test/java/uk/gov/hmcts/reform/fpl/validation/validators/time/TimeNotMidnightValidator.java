package uk.gov.hmcts.reform.fpl.validation.validators.time;

import uk.gov.hmcts.reform.fpl.validation.interfaces.time.TimeNotMidnight;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeNotMidnightValidator implements ConstraintValidator<TimeNotMidnight, LocalDateTime> {
    private static final LocalTime MIDNIGHT = LocalTime.of(0, 0, 0);

    @Override
    public boolean isValid(LocalDateTime localDateTime, ConstraintValidatorContext context) {
        return !localDateTime.toLocalTime().equals(MIDNIGHT);
    }
}
