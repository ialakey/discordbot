package com.alakey.discordbot.repository;

import com.alakey.discordbot.model.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, String> {
}
