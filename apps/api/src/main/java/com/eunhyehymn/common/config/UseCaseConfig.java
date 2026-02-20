package com.eunhyehymn.common.config;

import com.eunhyehymn.application.usecases.AdminCreateHymnUseCase;
import com.eunhyehymn.application.usecases.AdminCreateUserUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteUserUseCase;
import com.eunhyehymn.application.usecases.AdminEventExportJobUseCase;
import com.eunhyehymn.application.usecases.AdminListEventsUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteHymnUseCase;
import com.eunhyehymn.application.usecases.AdminListHymnsUseCase;
import com.eunhyehymn.application.usecases.AdminListUsersUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateHymnUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateUserUseCase;
import com.eunhyehymn.application.usecases.CleanupEventExportJobsUseCase;
import com.eunhyehymn.application.usecases.GetEventExportOpsMetricsUseCase;
import com.eunhyehymn.application.usecases.GetFavoriteUseCase;
import com.eunhyehymn.application.usecases.GetHistoryUseCase;
import com.eunhyehymn.application.usecases.GetHymnDetailUseCase;
import com.eunhyehymn.application.usecases.GetHymnNoteUseCase;
import com.eunhyehymn.application.usecases.GetMyProfileUseCase;
import com.eunhyehymn.application.usecases.ListHymnsUseCase;
import com.eunhyehymn.application.usecases.RecordEventsUseCase;
import com.eunhyehymn.application.usecases.SaveHymnNoteUseCase;
import com.eunhyehymn.application.usecases.ToggleFavoriteUseCase;
import com.eunhyehymn.application.usecases.UpsertMyProfileUseCase;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.domain.repository.EventExportJobCleanupRunRepository;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import com.eunhyehymn.domain.repository.EventRepository;
import com.eunhyehymn.domain.repository.HymnNoteRepository;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    @Bean
    ListHymnsUseCase listHymnsUseCase(HymnRepository hymnRepository) {
        return new ListHymnsUseCase(hymnRepository);
    }

    @Bean
    GetHymnDetailUseCase getHymnDetailUseCase(
        HymnRepository hymnRepository,
        AssetRepository assetRepository,
        UserHymnStateRepository userHymnStateRepository
    ) {
        return new GetHymnDetailUseCase(hymnRepository, assetRepository, userHymnStateRepository);
    }

    @Bean
    ToggleFavoriteUseCase toggleFavoriteUseCase(UserHymnStateRepository userHymnStateRepository) {
        return new ToggleFavoriteUseCase(userHymnStateRepository);
    }

    @Bean
    GetFavoriteUseCase getFavoriteUseCase(UserHymnStateRepository userHymnStateRepository) {
        return new GetFavoriteUseCase(userHymnStateRepository);
    }

    @Bean
    GetHymnNoteUseCase getHymnNoteUseCase(HymnNoteRepository hymnNoteRepository) {
        return new GetHymnNoteUseCase(hymnNoteRepository);
    }

    @Bean
    SaveHymnNoteUseCase saveHymnNoteUseCase(HymnNoteRepository hymnNoteRepository) {
        return new SaveHymnNoteUseCase(hymnNoteRepository);
    }

    @Bean
    GetHistoryUseCase getHistoryUseCase(UserHymnStateRepository userHymnStateRepository, HymnRepository hymnRepository) {
        return new GetHistoryUseCase(userHymnStateRepository, hymnRepository);
    }

    @Bean
    GetMyProfileUseCase getMyProfileUseCase(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository
    ) {
        return new GetMyProfileUseCase(userRepository, userProfileRepository);
    }

    @Bean
    UpsertMyProfileUseCase upsertMyProfileUseCase(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository
    ) {
        return new UpsertMyProfileUseCase(userRepository, userProfileRepository);
    }

    @Bean
    AdminCreateHymnUseCase adminCreateHymnUseCase(HymnRepository hymnRepository) {
        return new AdminCreateHymnUseCase(hymnRepository);
    }

    @Bean
    AdminUpdateHymnUseCase adminUpdateHymnUseCase(HymnRepository hymnRepository) {
        return new AdminUpdateHymnUseCase(hymnRepository);
    }

    @Bean
    AdminDeleteHymnUseCase adminDeleteHymnUseCase(
        HymnRepository hymnRepository,
        AssetRepository assetRepository,
        HymnNoteRepository hymnNoteRepository,
        UserHymnStateRepository userHymnStateRepository,
        EventRepository eventRepository
    ) {
        return new AdminDeleteHymnUseCase(hymnRepository, assetRepository, hymnNoteRepository, userHymnStateRepository, eventRepository);
    }

    @Bean
    AdminListHymnsUseCase adminListHymnsUseCase(HymnRepository hymnRepository) {
        return new AdminListHymnsUseCase(hymnRepository);
    }

    @Bean
    AdminListEventsUseCase adminListEventsUseCase(EventRepository eventRepository) {
        return new AdminListEventsUseCase(eventRepository);
    }

    @Bean
    AdminEventExportJobUseCase adminEventExportJobUseCase(
        EventRepository eventRepository,
        EventExportJobRepository eventExportJobRepository
    ) {
        return new AdminEventExportJobUseCase(eventRepository, eventExportJobRepository);
    }

    @Bean
    CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase(
        EventExportJobRepository eventExportJobRepository,
        @Value("${events.export.jobs.retention-days:7}") int retentionDays
    ) {
        return new CleanupEventExportJobsUseCase(eventExportJobRepository, retentionDays);
    }

    @Bean
    GetEventExportOpsMetricsUseCase getEventExportOpsMetricsUseCase(
        EventExportJobRepository eventExportJobRepository,
        EventExportJobCleanupRunRepository eventExportJobCleanupRunRepository
    ) {
        return new GetEventExportOpsMetricsUseCase(eventExportJobRepository, eventExportJobCleanupRunRepository);
    }

    @Bean
    RecordEventsUseCase recordEventsUseCase(EventRepository eventRepository) {
        return new RecordEventsUseCase(eventRepository);
    }

    @Bean
    AdminListUsersUseCase adminListUsersUseCase(UserRepository userRepository) {
        return new AdminListUsersUseCase(userRepository);
    }

    @Bean
    AdminUpdateUserUseCase adminUpdateUserUseCase(UserRepository userRepository) {
        return new AdminUpdateUserUseCase(userRepository);
    }

    @Bean
    AdminCreateUserUseCase adminCreateUserUseCase(UserRepository userRepository) {
        return new AdminCreateUserUseCase(userRepository);
    }

    @Bean
    AdminDeleteUserUseCase adminDeleteUserUseCase(UserRepository userRepository) {
        return new AdminDeleteUserUseCase(userRepository);
    }
}
