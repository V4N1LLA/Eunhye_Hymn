package com.eunhyehymn.common.config;

import com.eunhyehymn.application.usecases.AdminCreateInviteCodeUseCase;
import com.eunhyehymn.application.usecases.AdminListInviteCodesUseCase;
import com.eunhyehymn.application.usecases.AdminRevokeInviteCodeUseCase;
import com.eunhyehymn.application.usecases.ValidateInviteCodeUseCase;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InviteCodeConfig {
    @Bean
    AdminCreateInviteCodeUseCase adminCreateInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        return new AdminCreateInviteCodeUseCase(inviteCodeRepository);
    }

    @Bean
    AdminListInviteCodesUseCase adminListInviteCodesUseCase(InviteCodeRepository inviteCodeRepository) {
        return new AdminListInviteCodesUseCase(inviteCodeRepository);
    }

    @Bean
    AdminRevokeInviteCodeUseCase adminRevokeInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        return new AdminRevokeInviteCodeUseCase(inviteCodeRepository);
    }

    @Bean
    ValidateInviteCodeUseCase validateInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        return new ValidateInviteCodeUseCase(inviteCodeRepository);
    }
}
