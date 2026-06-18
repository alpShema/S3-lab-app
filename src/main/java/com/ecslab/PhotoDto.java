package com.ecslab;

import java.time.LocalDateTime;

public record PhotoDto(Long id, String imageUrl, String description, LocalDateTime uploadedAt) {}
