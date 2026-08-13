package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.CharmReceipt;
import com.meisterbear.domain.charm.entity.CharmReceiptStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmReceiptRepository extends JpaRepository<CharmReceipt, Long> {

    List<CharmReceipt> findByUserIdAndStatus(Long userId, CharmReceiptStatus status);
}
