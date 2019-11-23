package club.bayview.models;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByHandle(String handle);

    User findByEmail(String email);

    @Override
    void delete(User user);

}
