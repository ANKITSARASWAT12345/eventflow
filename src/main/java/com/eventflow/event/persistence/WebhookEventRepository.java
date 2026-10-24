package com.eventflow.event.persistence;

import com.eventflow.event.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository  extends JpaRepository<WebhookEvent, UUID> {

}
