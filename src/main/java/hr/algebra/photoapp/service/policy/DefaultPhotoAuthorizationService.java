package hr.algebra.photoapp.service.policy;

import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.util.FunctionalPhotoHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DefaultPhotoAuthorizationService implements PhotoAuthorizationService {

    @Override
    public void requireCanModify(Photo photo, Authentication authentication) {
        if (!FunctionalPhotoHelpers.canModifyPhoto(
                photo,
                authentication.getName(),
                role -> authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(role)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
    }
}
