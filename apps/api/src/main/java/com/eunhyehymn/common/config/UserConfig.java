package com.eunhyehymn.common.config;

import com.eunhyehymn.application.usecases.AdminListUsersUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateUserUseCase;
import com.eunhyehymn.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {
    @Bean
    AdminListUsersUseCase adminListUsersUseCase(UserRepository userRepository) {
        return new AdminListUsersUseCase(userRepository);
    }

    @Bean
    AdminUpdateUserUseCase adminUpdateUserUseCase(UserRepository userRepository) {
        return new AdminUpdateUserUseCase(userRepository);
    }
}
