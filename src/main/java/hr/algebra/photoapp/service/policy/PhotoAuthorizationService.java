package hr.algebra.photoapp.service.policy;

import hr.algebra.photoapp.model.Photo;
import org.springframework.security.core.Authentication;

/**
 * ISP: narrow interface for photo ownership checks (one responsibility).
 */
public interface PhotoAuthorizationService {

    void requireCanModify(Photo photo, Authentication authentication);
}
