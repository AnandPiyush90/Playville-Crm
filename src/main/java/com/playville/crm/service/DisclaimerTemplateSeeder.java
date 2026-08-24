package com.playville.crm.service;

import com.playville.crm.entity.Branch;
import com.playville.crm.entity.DisclaimerTemplate;
import com.playville.crm.repository.BranchRepository;
import com.playville.crm.repository.DisclaimerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisclaimerTemplateSeeder implements ApplicationRunner {
    private final DisclaimerTemplateRepository templates;
    private final BranchRepository branches;
    private final PlayvilleDisclaimerCopy copy;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String html = copy.html();
        List<DisclaimerTemplate> published = templates.findByStatus("PUBLISHED");
        DisclaimerTemplate usable = published.stream()
                .filter(template -> !isPlaceholder(template.getContentHtml()))
                .findFirst()
                .orElse(null);
        if (usable == null) {
            DisclaimerTemplate placeholder = published.stream().findFirst().orElse(null);
            if (placeholder != null) {
                placeholder.setTitle(PlayvilleDisclaimerCopy.TITLE);
                placeholder.setContentHtml(html);
                placeholder.setContentSha256(hash(html));
                usable = templates.save(placeholder);
                log.info("Filled placeholder disclaimer template id={} with PlayVille Conditions of Entry", usable.getId());
            } else {
                usable = templates.save(DisclaimerTemplate.builder()
                        .templateCode(PlayvilleDisclaimerCopy.TEMPLATE_CODE)
                        .version(PlayvilleDisclaimerCopy.VERSION)
                        .title(PlayvilleDisclaimerCopy.TITLE)
                        .contentHtml(html)
                        .contentSha256(hash(html))
                        .status("PUBLISHED")
                        .publishedAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build());
                log.info("Seeded PlayVille Conditions of Entry disclaimer template id={}", usable.getId());
            }
        }
        int assigned = 0;
        for (Branch branch : branches.findAllWithDisclaimerTemplate()) {
            DisclaimerTemplate active = branch.getActiveDisclaimerTemplate();
            if (active == null || isPlaceholder(active.getContentHtml())) {
                branch.setActiveDisclaimerTemplate(usable);
                if (!branch.isTabletSignatureEnabled() && !branch.isEmailConfirmationEnabled()) {
                    branch.setTabletSignatureEnabled(true);
                }
                branches.save(branch);
                assigned++;
            }
        }
        if (assigned > 0) {
            log.info("Assigned the PlayVille Conditions of Entry template to {} branch(es)", assigned);
        }
    }

    private boolean isPlaceholder(String html) {
        if (html == null || html.isBlank()) return true;
        String plain = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return plain.length() < 400;
    }

    private String hash(String html) {
        try {
            String normalized = html.replace("\r\n", "\n").replace('\r', '\n');
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
