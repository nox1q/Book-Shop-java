package kz.iitu.csse.group34.repositories;

import kz.iitu.csse.group34.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    List<Users> findAll();
    Users findByEmail(String email);
    Optional<Users> findByIdAndDeletedAtNull(Long id);
    void deleteById(long id);
}
