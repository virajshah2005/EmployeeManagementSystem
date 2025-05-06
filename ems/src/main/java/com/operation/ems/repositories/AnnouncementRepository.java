package com.operation.ems.repositories;

import com.operation.ems.models.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    List<Announcement> findTop5ByOrderByCreatedAtDesc();
}