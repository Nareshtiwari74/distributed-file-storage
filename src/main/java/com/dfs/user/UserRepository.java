
package com.dfs.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**

 * Data access for {@link User}. Spring Data implements these at runtime.

 */

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}

