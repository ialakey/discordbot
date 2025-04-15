package com.alakey.discordbot.repository;

import com.alakey.discordbot.model.BlockedRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedRoleRepository extends JpaRepository<BlockedRole, String> {
}
