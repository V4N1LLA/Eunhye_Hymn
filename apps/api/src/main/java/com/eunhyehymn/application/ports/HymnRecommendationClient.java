package com.eunhyehymn.application.ports;

import java.util.List;
import java.util.UUID;

public interface HymnRecommendationClient {
    List<Recommendation> recommend(String situation, List<CandidateHymn> candidates, int maxResults);

    record CandidateHymn(UUID id, String number, String title, String tags) {
    }

    record Recommendation(UUID hymnId, String reason) {
    }
}
