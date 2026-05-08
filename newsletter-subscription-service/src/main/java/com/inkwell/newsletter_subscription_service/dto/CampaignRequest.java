package com.inkwell.newsletter_subscription_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignRequest {
    private String subject;
    private String content;
    private String preferenceKeyword;
}
