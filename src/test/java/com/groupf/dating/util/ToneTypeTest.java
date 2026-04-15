package com.groupf.dating.util;

import com.groupf.dating.common.ToneType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToneTypeTest {

    @Test
    void fromString_null_returnsCasual() {
        assertThat(ToneType.fromString(null)).isEqualTo(ToneType.CASUAL);
    }

    @Test
    void fromString_emptyString_returnsCasual() {
        assertThat(ToneType.fromString("")).isEqualTo(ToneType.CASUAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"casual", "CASUAL", "Casual"})
    void fromString_caseInsensitive_returnsCasual(String input) {
        assertThat(ToneType.fromString(input)).isEqualTo(ToneType.CASUAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bold", "polite", "humorous", "warm"})
    void fromString_validValues_returnsCorrectEnum(String value) {
        ToneType tone = ToneType.fromString(value);
        assertThat(tone.getValue()).isEqualTo(value);
    }

    @Test
    void fromString_unknownValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> ToneType.fromString("aggressive"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aggressive");
    }

    @Test
    void fromString_withLeadingTrailingSpaces_parsesSuccessfully() {
        assertThat(ToneType.fromString("  bold  ")).isEqualTo(ToneType.BOLD);
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void allFiveToneValuesExist() {
        // making sure nobody accidentally removed one of the tone options
        var values = ToneType.values();
        assertThat(values).hasSize(5);
    }

    @Test
    void fromString_numericInput_throwsIllegalArgument() {
        // numbers should not be valid tone values
        assertThatThrownBy(() -> ToneType.fromString("42"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachToneHasNonBlankDescription() {
        // every tone should have a description set (used in prompts)
        for (ToneType t : ToneType.values()) {
            assertThat(t.getDescription()).isNotBlank();
        }
    }
}
