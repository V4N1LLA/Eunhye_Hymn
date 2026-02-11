package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.List;

public class AdminListUsersUseCase {
    private final UserRepository userRepository;

    public AdminListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listAll() {
        return userRepository.findAll();
    }
}
