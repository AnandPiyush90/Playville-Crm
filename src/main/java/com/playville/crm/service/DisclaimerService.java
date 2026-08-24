package com.playville.crm.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.disclaimer.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class DisclaimerService {
    private final DisclaimerTemplateRepository templates;
    private final CustomerOnboardingDraftRepository drafts;
    private final DisclaimerSigningRequestRepository requests;
    private final DisclaimerAcceptanceRepository acceptances;
    private final BranchRepository branches;
    private final ObjectMapper json;
    private final BranchEmailService branchEmailService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final EmailTemplateService emailTemplateService;
    private final PlayvilleDisclaimerCopy playvilleDisclaimerCopy;
    @Value("${PLAYVILLE_PUBLIC_DISCLAIMER_URL:${app.disclaimer.public-base-url:http://localhost:4200/public/disclaimer}}") private String publicBaseUrl;
    @Transactional(readOnly=true) public DisclaimerView sampleTemplate(){
        return DisclaimerView.builder()
                .templateCode(PlayvilleDisclaimerCopy.TEMPLATE_CODE)
                .version(PlayvilleDisclaimerCopy.VERSION)
                .title(PlayvilleDisclaimerCopy.TITLE)
                .contentHtml(playvilleDisclaimerCopy.html())
                .status("SAMPLE")
                .build();
    }
    @Transactional public DisclaimerView createTemplate(TemplateRequest r){
        Branch b=branch();
        DisclaimerTemplate t=templates.save(DisclaimerTemplate.builder().branch(b).templateCode(r.getTemplateCode()).version(r.getVersion()).title(r.getTitle()).contentHtml(r.getContentHtml()).build());
        return template(t);
    }
    @Transactional(readOnly=true) public List<DisclaimerView> listTemplates(){
        return templates.findByBranchIdOrBranchIsNull(BranchContext.getBranchId()).stream().map(this::template).toList();
    }
    @Transactional public DisclaimerView updateTemplate(Long id,TemplateRequest r){
        DisclaimerTemplate t=templateOwned(id);
        if(!"DRAFT".equals(t.getStatus())) throw new BusinessRuleException("DISCLAIMER_TEMPLATE_IMMUTABLE: Published templates cannot be changed");
        t.setTemplateCode(r.getTemplateCode());
        t.setVersion(r.getVersion());
        t.setTitle(r.getTitle());
        t.setContentHtml(r.getContentHtml());
        return template(t);
    }
    @Transactional public DisclaimerView newDraftFrom(Long id){
        DisclaimerTemplate source=templateOwned(id);
        DisclaimerTemplate draft=templates.save(DisclaimerTemplate.builder()
                .branch(branch())
                .templateCode(source.getTemplateCode())
                .version("draft-"+System.currentTimeMillis())
                .title(source.getTitle())
                .contentHtml(source.getContentHtml())
                .status("DRAFT")
                .build());
        return template(draft);
    }
    @Transactional public DisclaimerView publish(Long id){
        DisclaimerTemplate t=templateOwned(id);
        if(!"DRAFT".equals(t.getStatus())) throw new BusinessRuleException("DISCLAIMER_TEMPLATE_IMMUTABLE: Template is not a draft");
        t.setContentSha256(hash(normalize(t.getContentHtml())));
        t.setStatus("PUBLISHED");
        t.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        Branch b=branch();
        b.setActiveDisclaimerTemplate(t);
        if(!b.isTabletSignatureEnabled()&&!b.isEmailConfirmationEnabled()) b.setTabletSignatureEnabled(true);
        branches.save(b);
        return template(t);
    }
    @Transactional public DisclaimerView activate(Long id){
        DisclaimerTemplate t=templateOwned(id);
        if(!"PUBLISHED".equals(t.getStatus())) throw new BusinessRuleException("DISCLAIMER_TEMPLATE_NOT_PUBLISHED: Activate a published template");
        Branch b=branch();
        b.setActiveDisclaimerTemplate(t);
        branches.save(b);
        return template(t);
    }
    @Transactional(readOnly=true) public DisclaimerSettingsResponse getSettings(){
        Branch b=branch();
        DisclaimerTemplate active=b.getActiveDisclaimerTemplate();
        boolean ready=active!=null&&"PUBLISHED".equals(active.getStatus());
        return DisclaimerSettingsResponse.builder()
                .disclaimerRequiredForPhysicalVisit(b.isDisclaimerRequiredForPhysicalVisit())
                .tabletSignatureEnabled(b.isTabletSignatureEnabled())
                .emailConfirmationEnabled(b.isEmailConfirmationEnabled())
                .emailLinkTtlHours(b.getDisclaimerEmailLinkTtlHours())
                .disclaimerResignOnNewVersion(b.isDisclaimerResignOnNewVersion())
                .activeDisclaimerTemplateId(active==null?null:active.getId())
                .activeDisclaimerTemplateTitle(active==null?null:active.getTitle())
                .configurationReady(ready)
                .build();
    }

    @Transactional public DisclaimerSettingsResponse settings(DisclaimerSettingsRequest r){
        Branch b=branch();
        if(r.getDisclaimerRequiredForPhysicalVisit()!=null)b.setDisclaimerRequiredForPhysicalVisit(r.getDisclaimerRequiredForPhysicalVisit());
        if(r.getTabletSignatureEnabled()!=null)b.setTabletSignatureEnabled(r.getTabletSignatureEnabled());
        if(r.getEmailConfirmationEnabled()!=null)b.setEmailConfirmationEnabled(r.getEmailConfirmationEnabled());
        if(r.getEmailLinkTtlHours()!=null)b.setDisclaimerEmailLinkTtlHours(r.getEmailLinkTtlHours());
        if(r.getDisclaimerResignOnNewVersion()!=null)b.setDisclaimerResignOnNewVersion(r.getDisclaimerResignOnNewVersion());
        if(r.getActiveDisclaimerTemplateId()!=null){
            if(r.getActiveDisclaimerTemplateId()<=0){
                b.setActiveDisclaimerTemplate(null);
            } else {
                DisclaimerTemplate t=templateOwned(r.getActiveDisclaimerTemplateId());
                if(!"PUBLISHED".equals(t.getStatus())) throw new BusinessRuleException("DISCLAIMER_TEMPLATE_NOT_PUBLISHED: Activate a published template");
                b.setActiveDisclaimerTemplate(t);
            }
        }
        validateBranchPolicy(b);
        branches.save(b);
        return getSettings();
    }

    private void validateBranchPolicy(Branch b){
        if(b.isDisclaimerRequiredForPhysicalVisit()&&b.getActiveDisclaimerTemplate()==null)
            throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: A published active template is required when disclaimer enforcement is enabled");
        if(b.isDisclaimerRequiredForPhysicalVisit()&&!b.isTabletSignatureEnabled()&&!b.isEmailConfirmationEnabled())
            throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: Enable tablet signing or email confirmation");
    }
    @Transactional public DisclaimerView createDraft(DraftRequest r,String key){
        if(key==null||key.isBlank())throw new BusinessRuleException("Idempotency-Key header is required");
        int bid=BranchContext.getBranchId();
        Optional<CustomerOnboardingDraft> replay=drafts.findByBranchIdAndIdempotencyKey(bid,key);
        if(replay.isPresent()) return draft(replay.get());
        try {
            String email=r.getEmail()==null||r.getEmail().isBlank()?null:r.getEmail().trim();
            CustomerOnboardingDraft d=drafts.save(CustomerOnboardingDraft.builder().branch(branch()).parentName(r.getParentName()).phoneNumber(r.getPhoneNumber().replaceAll("[^0-9]","")).email(email).visitPurpose(r.getVisitPurpose()).childrenJson(json.writeValueAsString(r.getChildren())).idempotencyKey(key).expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24)).build());
            return draft(d);
        }
        catch(JsonProcessingException e){
            throw new BusinessRuleException("Invalid children payload");
        }
    }
    @Transactional(readOnly=true) public DisclaimerView getDraft(Long id){
        return draft(draftOwned(id));
    }
    @Transactional public DisclaimerView tabletRequest(Long draftId,String key){
        if(key==null||key.isBlank())throw new BusinessRuleException("Idempotency-Key header is required");
        CustomerOnboardingDraft d=draftOwned(draftId);
        Branch b=branch();
        if(!b.isTabletSignatureEnabled())throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: Tablet signing is disabled");
        if(b.getActiveDisclaimerTemplate()==null||!"PUBLISHED".equals(b.getActiveDisclaimerTemplate().getStatus()))
            throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: No active disclaimer template");
        if(d.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))throw new BusinessRuleException("ONBOARDING_DRAFT_EXPIRED");
        Optional<DisclaimerSigningRequest> replay=requests.findByBranchIdAndIdempotencyKey(b.getId(),key);
        if(replay.isPresent())return request(replay.get());
        d.setStatus("AWAITING_SIGNATURE");
        DisclaimerSigningRequest q=requests.save(DisclaimerSigningRequest.builder().branch(b).draft(d).template(b.getActiveDisclaimerTemplate()).channel("TABLET_SIGNATURE").idempotencyKey(key).expiresAt(d.getExpiresAt()).build());
        return request(q);
    }
    @Transactional public DisclaimerView emailRequest(Long draftId,String key){
        if(key==null||key.isBlank())throw new BusinessRuleException("Idempotency-Key header is required");
        CustomerOnboardingDraft d=draftOwned(draftId);
        Branch b=branch();
        if(!b.isEmailConfirmationEnabled())throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: Email confirmation is disabled");
        if(d.getEmail()==null||d.getEmail().isBlank())throw new BusinessRuleException("DISCLAIMER_EMAIL_REQUIRED: A valid customer email is required");
        if(b.getActiveDisclaimerTemplate()==null||!"PUBLISHED".equals(b.getActiveDisclaimerTemplate().getStatus()))
            throw new BusinessRuleException("DISCLAIMER_CONFIGURATION_REQUIRED: No active disclaimer template");
        if(d.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))throw new BusinessRuleException("ONBOARDING_DRAFT_EXPIRED");
        Optional<DisclaimerSigningRequest> replay=requests.findByBranchIdAndIdempotencyKey(b.getId(),key);
        if(replay.isPresent())return request(replay.get());
        byte[] tokenBytes=new byte[32]; new SecureRandom().nextBytes(tokenBytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiry=now.plusHours(b.getDisclaimerEmailLinkTtlHours());
        DisclaimerSigningRequest q=requests.save(DisclaimerSigningRequest.builder().branch(b).draft(d).template(b.getActiveDisclaimerTemplate()).channel("EMAIL_CONFIRMATION").status("SENT").idempotencyKey(key).tokenSha256(hash(raw)).sentAt(now).expiresAt(expiry).build());
        d.setStatus("AWAITING_SIGNATURE");
        var emailTemplate=emailTemplateService.render(b.getId(),EmailTemplateService.DISCLAIMER_SIGNING,Map.of("GUARDIAN_NAME",d.getParentName(),"BRANCH_NAME",b.getBranchName(),"DISCLAIMER_URL",publicBaseUrl+"/"+raw,"EXPIRES_AT",expiry+" UTC"));
        notificationDeliveryService.enqueueEmail(b,null,null,"DISCLAIMER_SIGNING","DISCLAIMER_SIGNING",String.valueOf(q.getId()),d.getEmail(),key,emailTemplate.subject(),emailTemplate.bodyText(),null,null);
        return request(q);
    }
    @Transactional public DisclaimerView publicView(String rawToken){
        DisclaimerSigningRequest q=publicRequest(rawToken); valid(q);
        if("CREATED".equals(q.getStatus())||"SENT".equals(q.getStatus())) q.setStatus("OPENED");
        DisclaimerView v=template(q.getTemplate()); v.setId(q.getId()); v.setChannel(q.getChannel()); v.setExpiresAt(q.getExpiresAt()); v.setEmail(mask(q.getDraft().getEmail())); return v;
    }
    @Transactional public DisclaimerView emailAccept(String rawToken,EmailAcceptanceRequest r){
        DisclaimerSigningRequest q=publicRequest(rawToken); valid(q);
        if(!"EMAIL_CONFIRMATION".equals(q.getChannel()))throw new BusinessRuleException("DISCLAIMER_REQUEST_NOT_SIGNABLE");
        if(!Boolean.TRUE.equals(r.getConfirmedReadAndAccepted()))throw new BusinessRuleException("DISCLAIMER_REQUIRED: Acceptance confirmation is required");
        return recordAcceptance(q,r.getSignerName(),r.getSignerRelationship(),"EMAIL_CONFIRMATION",null);
    }
    @Transactional(readOnly=true) public DisclaimerView tabletView(Long id){
        DisclaimerSigningRequest q=requestOwned(id);
        valid(q);
        DisclaimerView v=template(q.getTemplate());
        v.setId(q.getId());
        v.setExpiresAt(q.getExpiresAt());
        return v;
    }
    @Transactional public DisclaimerView accept(Long id,TabletAcceptanceRequest r){
        DisclaimerSigningRequest q=requestOwned(id);
        valid(q);
        if(!Boolean.TRUE.equals(r.getConfirmedReadAndAccepted()))throw new BusinessRuleException("DISCLAIMER_REQUIRED: Acceptance confirmation is required");
        if(acceptances.findByRequestId(id).isPresent())throw new BusinessRuleException("DISCLAIMER_ALREADY_SIGNED");
        byte[] image=signature(r.getSignatureDataUrl());
        String sig=hash(image);
        return recordAcceptance(q,r.getSignerName(),r.getSignerRelationship(),"TABLET_SIGNATURE",image);
    }
    private DisclaimerView recordAcceptance(DisclaimerSigningRequest q,String signerName,String relationship,String method,byte[] image){
        if(acceptances.findByRequestId(q.getId()).isPresent())throw new BusinessRuleException("DISCLAIMER_ALREADY_SIGNED");
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC); DisclaimerTemplate t=q.getTemplate(); String sig=image==null?"EMAIL_CONFIRMATION":hash(image);
        String evidence=hash(String.join("|",method,q.getBranch().getId().toString(),q.getDraft().getId().toString(),q.getId().toString(),t.getId().toString(),t.getVersion(),t.getContentSha256(),signerName,relationship,q.getDraft().getEmail()==null?"":q.getDraft().getEmail(),q.getDraft().getPhoneNumber(),now.toString(),sig));
        DisclaimerAcceptance a=acceptances.save(DisclaimerAcceptance.builder().branch(q.getBranch()).draft(q.getDraft()).request(q).template(t).templateCodeSnapshot(t.getTemplateCode()).templateVersionSnapshot(t.getVersion()).templateTitleSnapshot(t.getTitle()).contentSha256(t.getContentSha256()).acceptanceMethod(method).signerName(signerName).signerRelationship(relationship).signerEmail(q.getDraft().getEmail()).signerPhone(q.getDraft().getPhoneNumber()).acceptedAt(now).signatureMimeType(image==null?null:"image/png").signatureImage(image).signatureSha256(image==null?null:sig).evidenceSha256(evidence).build());
        q.setStatus("SIGNED");
        q.setSignedAt(now);
        q.getDraft().setStatus("SIGNED");
        return acceptance(a);
    }
    @Transactional(readOnly=true) public List<DisclaimerView> customerAcceptances(Integer customerId){
        return acceptances.findByCustomerIdOrderByAcceptedAtDesc(customerId).stream().map(this::acceptance).toList();
    }
    @Transactional(readOnly=true) public List<DisclaimerView> branchAcceptances(){
        return acceptances.findByBranchIdOrderByAcceptedAtDesc(BranchContext.getBranchId()).stream().map(this::acceptance).toList();
    }
    public boolean current(Customer c,Branch b){
        if(!b.isDisclaimerRequiredForPhysicalVisit())return true;
        DisclaimerAcceptance a=c.getCurrentDisclaimerAcceptance();
        return a!=null&&"VALID".equals(a.getStatus())&&(!b.isDisclaimerResignOnNewVersion()||b.getActiveDisclaimerTemplate()!=null&&a.getTemplate().getId().equals(b.getActiveDisclaimerTemplate().getId()));
    }
    private void valid(DisclaimerSigningRequest q){
        if(q.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))throw new BusinessRuleException("DISCLAIMER_REQUEST_EXPIRED");
        if(!List.of("CREATED","SENT","OPENED","VERIFIED").contains(q.getStatus()))throw new BusinessRuleException("DISCLAIMER_REQUEST_NOT_SIGNABLE");
    }
    private byte[] signature(String data){
        try{
            if(!data.startsWith("data:image/png;base64,"))throw new BusinessRuleException("DISCLAIMER_SIGNATURE_INVALID");
            byte[] b=Base64.getDecoder().decode(data.substring(data.indexOf(',')+1));
            if(b.length<100||b.length>500_000||b[0]!=(byte)137||b[1]!=80||b[2]!=78||b[3]!=71)throw new BusinessRuleException("DISCLAIMER_SIGNATURE_INVALID");
            return b;
        }
        catch(IllegalArgumentException e){
            throw new BusinessRuleException("DISCLAIMER_SIGNATURE_INVALID");
        }
    }
    private Branch branch(){
        Integer id=BranchContext.getBranchId();
        return branches.findByIdWithDisclaimerTemplate(id).or(()->branches.findById(id)).orElseThrow(()->new ResourceNotFoundException("Branch",id));
    }
    private DisclaimerTemplate templateOwned(Long id){
        DisclaimerTemplate t=templates.findById(id).orElseThrow(()->new ResourceNotFoundException("Disclaimer template",id.intValue()));
        if(t.getBranch()!=null&&!t.getBranch().getId().equals(BranchContext.getBranchId()))throw new BranchAccessDeniedException();
        return t;
    }
    private CustomerOnboardingDraft draftOwned(Long id){
        CustomerOnboardingDraft d=drafts.findById(id).orElseThrow(()->new ResourceNotFoundException("Onboarding draft",id.intValue()));
        if(!d.getBranch().getId().equals(BranchContext.getBranchId()))throw new BranchAccessDeniedException();
        return d;
    }
    private DisclaimerSigningRequest requestOwned(Long id){
        DisclaimerSigningRequest q=requests.findByIdWithTemplate(id).or(()->requests.findById(id)).orElseThrow(()->new ResourceNotFoundException("Signing request",id.intValue()));
        if(!q.getBranch().getId().equals(BranchContext.getBranchId()))throw new BranchAccessDeniedException();
        return q;
    }
    private DisclaimerSigningRequest publicRequest(String rawToken){
        if(rawToken==null||rawToken.length()<40)throw new ResourceNotFoundException("Disclaimer request",0);
        return requests.findByTokenSha256(hash(rawToken)).orElseThrow(()->new ResourceNotFoundException("Disclaimer request",0));
    }
    private String mask(String email){
        if(email==null)return null; int at=email.indexOf('@'); if(at<2)return "***"; return email.substring(0,1)+"***"+email.substring(at);
    }
    private DisclaimerView template(DisclaimerTemplate t){
        return DisclaimerView.builder().id(t.getId()).status(t.getStatus()).templateCode(t.getTemplateCode()).version(t.getVersion()).title(t.getTitle()).contentHtml(t.getContentHtml()).build();
    }
    private DisclaimerView draft(CustomerOnboardingDraft d){
        Long acceptanceId=acceptances.findByDraftId(d.getId()).map(DisclaimerAcceptance::getId).orElse(null);
        return DisclaimerView.builder().id(d.getId()).acceptanceId(acceptanceId).status(d.getStatus()).expiresAt(d.getExpiresAt()).build();
    }
    private DisclaimerView request(DisclaimerSigningRequest q){
        return DisclaimerView.builder().id(q.getId()).status(q.getStatus()).channel(q.getChannel()).email(mask(q.getDraft().getEmail())).sentAt(q.getSentAt()).expiresAt(q.getExpiresAt()).build();
    }
    private DisclaimerView acceptance(DisclaimerAcceptance a){
        Customer customer=a.getCustomer();
        CustomerOnboardingDraft draft=a.getDraft();
        String image=a.getSignatureImage()==null?null:"data:image/png;base64,"+Base64.getEncoder().encodeToString(a.getSignatureImage());
        return DisclaimerView.builder()
                .id(a.getId())
                .acceptanceId(a.getId())
                .status(a.getStatus())
                .signerName(a.getSignerName())
                .signerRelationship(a.getSignerRelationship())
                .acceptedAt(a.getAcceptedAt())
                .evidenceSha256(a.getEvidenceSha256())
                .version(a.getTemplateVersionSnapshot())
                .title(a.getTemplateTitleSnapshot())
                .acceptanceMethod(a.getAcceptanceMethod())
                .channel(a.getAcceptanceMethod())
                .email(a.getSignerEmail())
                .phone(a.getSignerPhone())
                .customerId(customer==null?null:customer.getId())
                .customerName(customer!=null?customer.getParentName():(draft==null?null:draft.getParentName()))
                .childrenSummary(childrenSummary(draft))
                .hasSignature(image!=null)
                .signatureDataUrl(image)
                .build();
    }
    private String childrenSummary(CustomerOnboardingDraft draft){
        if(draft==null||draft.getChildrenJson()==null||draft.getChildrenJson().isBlank())return null;
        try{
            var node=json.readTree(draft.getChildrenJson());
            if(!node.isArray())return null;
            List<String> names=new ArrayList<>();
            node.forEach(child->{
                String name=child.path("kidName").asText(child.path("name").asText(""));
                if(!name.isBlank())names.add(name);
            });
            return names.isEmpty()?null:String.join(", ",names);
        }catch(Exception ignored){
            return null;
        }
    }
    private String normalize(String s){
        return s.replace("\r\n","\n").replace('\r','\n');
    }
    private String hash(String s){
        return hash(s.getBytes(StandardCharsets.UTF_8));
    }
    private String hash(byte[] b){
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        }
        catch(NoSuchAlgorithmException e){
            throw new IllegalStateException(e);
        }
    }
}
