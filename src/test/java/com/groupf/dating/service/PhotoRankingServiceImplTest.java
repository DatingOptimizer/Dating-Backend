package com.groupf.dating.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupf.dating.dto.PhotoRankResponse;
import com.groupf.dating.exception.IException;
import com.groupf.dating.repository.ProfileRequestRepository;
import com.groupf.dating.service.impl.PhotoRankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoRankingServiceImplTest {

    @Mock
    private ClaudeApiService claudeApiService;

    @Mock
    private ProfileRequestRepository profileRequestRepository;

    private PhotoRankingServiceImpl photoRankingService;

    @BeforeEach
    void setUp() {
        photoRankingService = new PhotoRankingServiceImpl(
                claudeApiService, new ObjectMapper(), profileRequestRepository);
    }

    // ──────────── validation ────────────

    @Test
    void rankPhotos_throwsTooFew_whenLessThanTwoPhotos() {
        MockMultipartFile single = jpeg("photo1.jpg");

        assertThatThrownBy(() -> photoRankingService.rankPhotos(new MultipartFile[]{single}))
                .isInstanceOf(IException.class);
    }

    @Test
    void rankPhotos_throwsTooMany_whenMoreThanFivePhotos() {
        MultipartFile[] photos = new MultipartFile[6];
        for (int i = 0; i < 6; i++) photos[i] = jpeg("photo" + i + ".jpg");

        assertThatThrownBy(() -> photoRankingService.rankPhotos(photos))
                .isInstanceOf(IException.class);
    }

    @Test
    void rankPhotos_throwsInvalidFormat_forNonImageFile() {
        MockMultipartFile txt = new MockMultipartFile("photos", "doc.txt", "text/plain", new byte[100]);
        MockMultipartFile img = jpeg("photo.jpg");

        assertThatThrownBy(() -> photoRankingService.rankPhotos(new MultipartFile[]{txt, img}))
                .isInstanceOf(IException.class);
    }

    // ──────────── JSON parsing — plain JSON ────────────

    @Test
    void rankPhotos_parsesPlainJsonResponse() {
        String plainJson = """
                [
                  {"rank": 1, "score": 85, "reasoning": "Great lighting and smile"},
                  {"rank": 2, "score": 70, "reasoning": "Good composition"}
                ]""";
        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any()))
                .thenReturn(plainJson);

        PhotoRankResponse response = photoRankingService.rankPhotos(twoPhotos());

        assertThat(response.getRankedPhotos()).hasSize(2);
        assertThat(response.getRankedPhotos().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRankedPhotos().get(0).getScore()).isEqualTo(85.0);
        assertThat(response.getRankedPhotos().get(0).getReasoning()).isEqualTo("Great lighting and smile");
        assertThat(response.getRankedPhotos().get(1).getRank()).isEqualTo(2);
    }

    // ──────────── JSON parsing — markdown-wrapped ────────────

    @Test
    void rankPhotos_parsesMarkdownWrappedJson() {
        String markdownResponse = """
                ```json
                [
                  {"rank": 1, "score": 90, "reasoning": "Best photo"},
                  {"rank": 2, "score": 65, "reasoning": "Decent photo"}
                ]
                ```
                Some extra commentary from Claude that should be ignored.""";

        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any()))
                .thenReturn(markdownResponse);

        PhotoRankResponse response = photoRankingService.rankPhotos(twoPhotos());

        assertThat(response.getRankedPhotos()).hasSize(2);
        assertThat(response.getRankedPhotos().get(0).getScore()).isEqualTo(90.0);
        assertThat(response.getRankedPhotos().get(0).getReasoning()).isEqualTo("Best photo");
        // Extra commentary should NOT appear in reasoning
        assertThat(response.getRankedPhotos().get(0).getReasoning())
                .doesNotContain("commentary");
    }

    @Test
    void rankPhotos_parsesCodeBlockWithoutLanguageTag() {
        String response = """
                ```
                [{"rank": 1, "score": 80, "reasoning": "Nice"}, {"rank": 2, "score": 60, "reasoning": "OK"}]
                ```""";

        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any()))
                .thenReturn(response);

        PhotoRankResponse result = photoRankingService.rankPhotos(twoPhotos());

        assertThat(result.getRankedPhotos()).hasSize(2);
        assertThat(result.getRankedPhotos().get(0).getReasoning()).isEqualTo("Nice");
    }

    // ──────────── sorting ────────────

    @Test
    void rankPhotos_sortsResultsByRankAscending() {
        String json = """
                [
                  {"rank": 2, "score": 70, "reasoning": "Second"},
                  {"rank": 1, "score": 90, "reasoning": "First"}
                ]""";
        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any()))
                .thenReturn(json);

        PhotoRankResponse response = photoRankingService.rankPhotos(twoPhotos());

        assertThat(response.getRankedPhotos().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRankedPhotos().get(1).getRank()).isEqualTo(2);
    }

    // ──────────── helpers ────────────

    private MockMultipartFile jpeg(String name) {
        return new MockMultipartFile("photos", name, "image/jpeg", new byte[100]);
    }

    private MultipartFile[] twoPhotos() {
        return new MultipartFile[]{jpeg("photo1.jpg"), jpeg("photo2.jpg")};
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void rankPhotos_acceptsExactlyTwoPhotos() {
        // make sure the boundary is handled correctly
        String json = "[{\"rank\": 1, \"score\": 88, \"reasoning\": \"Best one\"}, " +
                      "{\"rank\": 2, \"score\": 55, \"reasoning\": \"Not as good\"}]";
        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any())).thenReturn(json);

        var result = photoRankingService.rankPhotos(twoPhotos());
        assertThat(result.getRankedPhotos()).hasSize(2);
    }

    @Test
    void rankPhotos_acceptsExactlyFivePhotos() {
        // testing the upper boundary doesn't throw
        MultipartFile[] fivePhotos = new MultipartFile[5];
        for (int i = 0; i < 5; i++) fivePhotos[i] = jpeg("p" + i + ".jpg");

        String json = "[{\"rank\":1,\"score\":90,\"reasoning\":\"a\"},{\"rank\":2,\"score\":80,\"reasoning\":\"b\"}," +
                "{\"rank\":3,\"score\":70,\"reasoning\":\"c\"},{\"rank\":4,\"score\":60,\"reasoning\":\"d\"}," +
                "{\"rank\":5,\"score\":50,\"reasoning\":\"e\"}]";
        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any())).thenReturn(json);

        var result = photoRankingService.rankPhotos(fivePhotos);
        assertThat(result.getRankedPhotos()).hasSize(5);
    }

    @Test
    void rankPhotos_pngFilesAreAccepted() {
        // making sure png isn't blocked
        MockMultipartFile png1 = new MockMultipartFile("photos", "selfie.png", "image/png", new byte[200]);
        MockMultipartFile png2 = new MockMultipartFile("photos", "outdoors.png", "image/png", new byte[200]);

        String json = "[{\"rank\":1,\"score\":75,\"reasoning\":\"looks great\"},{\"rank\":2,\"score\":60,\"reasoning\":\"ok\"}]";
        when(claudeApiService.callClaudeApiWithVision(anyString(), anyString(), any())).thenReturn(json);

        var result = photoRankingService.rankPhotos(new MultipartFile[]{png1, png2});
        assertThat(result.getRankedPhotos()).hasSize(2);
    }
}
