package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.application.ports.StorageService;
import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnEntity;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAssetApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private HymnJpaRepository hymnJpaRepository;

    @Autowired
    private AssetJpaRepository assetJpaRepository;

    @MockBean
    private StorageService storageService;

    private UUID hymnId;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        assetJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "관리", "10", "tag", true, Instant.now()));

        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(adminId, "관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userId, "일반", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));

        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
        userToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void presignRequiresAdminRole() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "filename", "score.png",
            "contentType", "image/png"
        ));

        mockMvc.perform(post("/admin/assets/presign")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("forbidden"));
    }

    @Test
    void adminCanPresignAssetUpload() throws Exception {
        when(storageService.presignUpload(any())).thenReturn(
            new StorageService.PresignResult(
                "https://upload.local/presigned",
                "https://public.local/hymns/asset.png",
                "hymns/asset.png"
            )
        );

        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "filename", "score.png",
            "contentType", "image/png"
        ));

        mockMvc.perform(post("/admin/assets/presign")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.uploadUrl").value("https://upload.local/presigned"))
            .andExpect(jsonPath("$.data.publicUrl").value("https://public.local/hymns/asset.png"))
            .andExpect(jsonPath("$.data.objectKey").value("hymns/asset.png"));
    }

    @Test
    void adminCanConfirmAsset() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "publicUrl", "https://public.local/hymns/asset.png",
            "objectKey", "hymns/" + hymnId + "/PNG/ALL/asset.png",
            "checksum", "abc123",
            "version", "v1"
        ));

        String response = mockMvc.perform(post("/admin/assets/confirm")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hymnId").value(hymnId.toString()))
            .andExpect(jsonPath("$.data.type").value(AssetType.PNG.name()))
            .andExpect(jsonPath("$.data.url").value("https://public.local/hymns/asset.png"))
            .andExpect(jsonPath("$.data.objectKey").value("hymns/" + hymnId + "/PNG/ALL/asset.png"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        UUID assetId = UUID.fromString(objectMapper.readTree(response).get("data").get("assetId").asText());
        var saved = assetJpaRepository.findById(assetId).orElseThrow();
        assertThat(saved.getPart()).isEqualTo(com.eunhyehymn.domain.model.PartType.ALL);
    }

    @Test
    void confirmRejectsInvalidObjectKey() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "publicUrl", "https://public.local/hymns/asset.png",
            "objectKey", "hymns/" + UUID.randomUUID() + "/PNG/ALL/asset.png"
        ));

        mockMvc.perform(post("/admin/assets/confirm")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("invalid_object_key"));
    }

    @Test
    void confirmReplacesExistingAssetForSameKey() throws Exception {
        assetJpaRepository.save(new com.eunhyehymn.infrastructure.persistence.AssetEntity(
            UUID.randomUUID(),
            hymnId,
            AssetType.PNG,
            com.eunhyehymn.domain.model.PartType.ALL,
            "https://public.local/hymns/old.png",
            "hymns/" + hymnId + "/PNG/ALL/old.png",
            null,
            null,
            Instant.now()
        ));

        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "publicUrl", "https://public.local/hymns/new.png",
            "objectKey", "hymns/" + hymnId + "/PNG/ALL/new.png"
        ));

        mockMvc.perform(post("/admin/assets/confirm")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.objectKey").value("hymns/" + hymnId + "/PNG/ALL/new.png"));

        var sameKeyAssets = assetJpaRepository.findByHymnId(hymnId).stream()
            .filter(asset -> asset.getType() == AssetType.PNG)
            .filter(asset -> asset.getPart() == com.eunhyehymn.domain.model.PartType.ALL)
            .toList();
        assertThat(sameKeyAssets).hasSize(1);
        assertThat(sameKeyAssets.get(0).getObjectKey()).isEqualTo("hymns/" + hymnId + "/PNG/ALL/new.png");
    }

    @Test
    void confirmUsesAllWhenPartIsMissing() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "hymnId", hymnId.toString(),
            "type", AssetType.PNG.name(),
            "publicUrl", "https://public.local/hymns/asset.png",
            "objectKey", "hymns/" + hymnId + "/PNG/ALL/missing-part.png"
        ));

        String response = mockMvc.perform(post("/admin/assets/confirm")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.objectKey").value("hymns/" + hymnId + "/PNG/ALL/missing-part.png"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        UUID assetId = UUID.fromString(objectMapper.readTree(response).get("data").get("assetId").asText());
        var saved = assetJpaRepository.findById(assetId).orElseThrow();
        assertThat(saved.getPart()).isEqualTo(com.eunhyehymn.domain.model.PartType.ALL);
    }
}
