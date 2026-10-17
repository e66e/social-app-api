package dev.e66e.social_app_api.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface IdempotencyRecordRepository<E extends AbstractIdempotencyRecord>
        extends JpaRepository<E, IdempotencyKey> {

    @Query("""
                select e from #{#entityName} e
                where e.id.authorId = :authorId
                    and e.id.idempotencyKey = :idempotencyKey""")
    Optional<E> findByIdempotencyKey(@Param("authorId") UUID authorId,
                                     @Param("idempotencyKey") String idempotencyKey);
}
