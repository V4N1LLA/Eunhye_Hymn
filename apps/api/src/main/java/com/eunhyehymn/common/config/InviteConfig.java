package com.eunhyehymn.common.config;

import com.eunhyehymn.application.usecases.CreateInviteUseCase;
import com.eunhyehymn.application.usecases.ListInvitesUseCase;
import com.eunhyehymn.application.usecases.RevokeInviteUseCase;
import com.eunhyehymn.application.usecases.ValidateInviteUseCase;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InviteConfig {
    @Bean
    ValidateInviteUseCase validateInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        return new ValidateInviteUseCase(inviteCodeRepository);
    }

    @Bean
    CreateInviteUseCase createInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        return new CreateInviteUseCase(inviteCodeRepository);
    }

    @Bean
    RevokeInviteUseCase revokeInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        return new RevokeInviteUseCase(inviteCodeRepository);
    }

    @Bean
    ListInvitesUseCase listInvitesUseCase(InviteCodeRepository inviteCodeRepository) {
        return new ListInvitesUseCase(inviteCodeRepository);
    }
}
