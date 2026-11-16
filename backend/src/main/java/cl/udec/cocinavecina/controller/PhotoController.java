package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.UploadRequest;
import cl.udec.cocinavecina.dto.UploadUrl;
import cl.udec.cocinavecina.service.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/v1/photos")
public class PhotoController {
    private final PhotoService photos;

    public PhotoController(PhotoService photos) {
        this.photos = photos;
    }

    @PostMapping("/uploads")
    @PreAuthorize("hasRole('SELLER')")
    public UploadUrl prepare(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UploadRequest body) {
        return photos.prepareUpload(CurrentUser.id(jwt), body.contentType());
    }

    @PutMapping("/local/{key}")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadLocal(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String key,
        HttpServletRequest request
    ) throws IOException {
        photos.storeLocal(CurrentUser.id(jwt), key, request.getContentType(), request.getInputStream());
    }
}
