
function calc(attribute, attention, reference, hesitating) {
    var suggestion = Suggestion.NoIntervention;

    if (attention.level == Attention.GeneralAttention.level) {
        if (hesitating || reference.name == Reference.Abnormal.name) {
            suggestion = Suggestion.ChattingService;
        }
    } else if (attention.level == Attention.FocusedAttention.level) {
        suggestion = Suggestion.PsychologicalCounseling;
    } else if (attention.level == Attention.SpecialAttention.level) {
        suggestion = Suggestion.PsychiatryDepartment;
    }

    return {
        "suggestion": suggestion
    }
}
