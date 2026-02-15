package me.n1ar4.jar.analyzer.gui.runtime.model;

public record LeakRulesDto(
        boolean url,
        boolean jdbc,
        boolean filePath,
        boolean jwt,
        boolean mac,
        boolean ip,
        boolean phone,
        boolean idCard,
        boolean email,
        boolean apiKey,
        boolean bankCard,
        boolean cloudAkSk,
        boolean cryptoKey,
        boolean aiKey,
        boolean password,
        boolean detectBase64
) {
}
