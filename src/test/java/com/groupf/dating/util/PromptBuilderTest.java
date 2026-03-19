package com.groupf.dating.util;

import com.groupf.dating.common.ToneType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    // ──────────── buildBioRewriteSystemPrompt ────────────

    @ParameterizedTest
    @EnumSource(ToneType.class)
    void buildBioRewriteSystemPrompt_containsToneValueAndDescription(ToneType tone) {
        String prompt = PromptBuilder.buildBioRewriteSystemPrompt(tone);

        assertThat(prompt).contains(tone.getValue());
        assertThat(prompt).contains(tone.getDescription());
    }

    @Test
    void buildBioRewriteSystemPrompt_mentionsVersionCount() {
        String prompt = PromptBuilder.buildBioRewriteSystemPrompt(ToneType.CASUAL);
        // Should specify how many versions to generate
        assertThat(prompt).containsPattern("\\d+");
    }

    // ──────────── buildBioRewriteUserPrompt ────────────

    @Test
    void buildBioRewriteUserPrompt_containsOriginalBioText() {
        String bio = "I love hiking and coffee.";
        String prompt = PromptBuilder.buildBioRewriteUserPrompt(bio);

        assertThat(prompt).contains(bio);
    }

    @Test
    void buildBioRewriteUserPrompt_instructsNumberedFormat() {
        String prompt = PromptBuilder.buildBioRewriteUserPrompt("Test bio");

        assertThat(prompt).containsIgnoringCase("numbered");
    }

    // ──────────── buildPhotoRankingSystemPrompt ────────────

    @Test
    void buildPhotoRankingSystemPrompt_mentionesDatingContext() {
        String prompt = PromptBuilder.buildPhotoRankingSystemPrompt();

        assertThat(prompt).containsIgnoringCase("dating");
        assertThat(prompt).containsIgnoringCase("rank");
    }

    // ──────────── buildPhotoRankingUserPrompt ────────────

    @Test
    void buildPhotoRankingUserPrompt_containsPhotoCount() {
        String prompt = PromptBuilder.buildPhotoRankingUserPrompt(3);

        assertThat(prompt).contains("3");
    }

    @Test
    void buildPhotoRankingUserPrompt_requestsJsonFormat() {
        String prompt = PromptBuilder.buildPhotoRankingUserPrompt(2);

        assertThat(prompt).containsIgnoringCase("json");
    }

    // ──────────── buildConversationStarterSystemPrompt ────────────

    @ParameterizedTest
    @EnumSource(ToneType.class)
    void buildConversationStarterSystemPrompt_containsToneInfo(ToneType tone) {
        String prompt = PromptBuilder.buildConversationStarterSystemPrompt(tone);

        assertThat(prompt).contains(tone.getValue());
        assertThat(prompt).contains(tone.getDescription());
    }

    // ──────────── buildConversationStarterUserPrompt ────────────

    @Test
    void buildConversationStarterUserPrompt_containsBioText() {
        String bio = "Data scientist who loves hiking and guitar.";
        String prompt = PromptBuilder.buildConversationStarterUserPrompt(bio);

        assertThat(prompt).contains(bio);
    }
}
