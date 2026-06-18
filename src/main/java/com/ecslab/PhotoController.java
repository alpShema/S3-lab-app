package com.ecslab;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class PhotoController {

    private final PhotoRepository photoRepository;
    private final StorageService storageService;

    public PhotoController(PhotoRepository photoRepository, StorageService storageService) {
        this.photoRepository = photoRepository;
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String gallery(Model model) {
        List<PhotoDto> photos = photoRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(p -> new PhotoDto(
                        p.getId(),
                        storageService.getUrl(p.getS3Key()),
                        p.getDescription(),
                        p.getUploadedAt()))
                .toList();

        model.addAttribute("photos", photos);
        return "index";
    }

    @PostMapping("/photos")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description) throws IOException {

        String key = storageService.upload(file);

        Photo photo = new Photo();
        photo.setS3Key(key);
        photo.setDescription(description);
        photoRepository.save(photo);

        return "redirect:/";
    }

    @PostMapping("/photos/{id}/delete")
    public String delete(@PathVariable Long id) {
        photoRepository.findById(id).ifPresent(photo -> {
            storageService.delete(photo.getS3Key());
            photoRepository.delete(photo);
        });
        return "redirect:/";
    }
}
