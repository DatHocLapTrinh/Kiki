package com.kiki.backend;

import com.kiki.backend.service.VietnameseNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseNameNormalizerTest {

    private VietnameseNameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new VietnameseNameNormalizer();
    }

    @Test
    void testNormalize_RestoresJohnToTruongWithUserProfileName() {
        String raw = "my name is john";
        String userName = "Nguyễn Văn Trường";
        String normalized = normalizer.normalize(raw, userName, null);

        assertEquals("my name is Truong", normalized);
    }

    @Test
    void testNormalize_RestoresSeanToTruongWithSingleGivenName() {
        String raw = "Hello I am sean";
        String userName = "Truong";
        String normalized = normalizer.normalize(raw, userName, null);

        assertEquals("Hello I am Truong", normalized);
    }

    @Test
    void testNormalize_RestoresPhoneticAliasWithoutUserProfile() {
        String raw = "Hi, my name is john";
        String normalized = normalizer.normalize(raw, null, null);

        assertEquals("Hi, my name is Truong", normalized);
    }

    @Test
    void testNormalize_PicksCandidateWithRealName() {
        String raw = "my name is john";
        List<String> candidates = List.of("my name is john", "my name is tron", "my name is truong");
        String normalized = normalizer.normalize(raw, "Truong", candidates);

        assertEquals("my name is Truong", normalized);
    }

    @Test
    void testNormalize_RestoresVietnameseCities() {
        String raw = "I live in ha noi and travel to sai gon";
        String normalized = normalizer.normalize(raw, null, null);

        assertEquals("I live in Hanoi and travel to Saigon", normalized);
    }

    @Test
    void testExtractGivenNameAndRemoveAccents() {
        assertEquals("Trường", normalizer.extractGivenName("Nguyễn Văn Trường"));
        assertEquals("Truong", normalizer.removeAccents("Trường"));
        assertEquals("Duc", normalizer.removeAccents("Đức"));
    }
}
