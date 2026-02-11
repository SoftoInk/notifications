package com.novacomp.notifications.channel.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * An email file attachment.
 */
@Getter
@Builder
@ToString(exclude = "content")
public class Attachment {
    private final String filename;
    private final String contentType;
    private final byte[] content;
}
