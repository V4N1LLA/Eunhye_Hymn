package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.AdminPasswordCredential;
import java.util.Optional;

public interface AdminPasswordCredentialRepository {
    Optional<AdminPasswordCredential> find();

    AdminPasswordCredential save(AdminPasswordCredential credential);
}
