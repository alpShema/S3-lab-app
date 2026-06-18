package com.ecslab;

import java.time.LocalDateTime;

public record PhotoDto(String imageUrl, String description, LocalDateTime uploadedAt) {}
